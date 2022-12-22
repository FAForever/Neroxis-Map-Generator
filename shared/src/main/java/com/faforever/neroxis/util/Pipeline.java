package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.visualization.VisualDebugger;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Pipeline {
    private static final List<Entry> pipeline = new ArrayList<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    public static boolean HASH_MASK = false;
    private static CompletableFuture<List<Mask<?, ?>>> started = new CompletableFuture<>();
    private static String[] hashArray;

    public static void reset() {
        started = new CompletableFuture<>();
        pipeline.clear();
    }

    public static void add(Mask<?, ?> executingMask, List<Mask<?, ?>> maskDependencies,
                           Consumer<List<Mask<?, ?>>> function) {
        int index = pipeline.size();
        if (isRunning()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingMethod = null;
        String callingLine = null;

        if (DebugUtil.DEBUG) {
            callingMethod = DebugUtil.getLastStackTraceMethodInPackage("com.faforever.neroxis.mask");
            callingLine = DebugUtil.getLastStackTraceLineAfterPackage("com.faforever.neroxis.mask");
        }

        List<Entry> entryDependencies = Pipeline.getDependencyList(maskDependencies, executingMask);
        String finalCallingLine = callingLine;
        String finalCallingMethod = callingMethod;
        CompletableFuture<Void> newFuture = Pipeline.getDependencyFuture(entryDependencies)
                                                    .thenAcceptAsync(dependencies -> {
                                                        long startTime = System.currentTimeMillis();
                                                        boolean visualDebug = executingMask.isVisualDebug();
                                                        executingMask.setVisualDebug(false);
                                                        function.accept(dependencies);
                                                        long functionTime = System.currentTimeMillis() - startTime;
                                                        startTime = System.currentTimeMillis();
                                                        if (HASH_MASK) {
                                                            try {
                                                                hashArray[index] = String.format("%s,\t%s,\t%s,\t%s%n",
                                                                                                 executingMask.toHash(),
                                                                                                 finalCallingLine,
                                                                                                 executingMask.getName(),
                                                                                                 finalCallingMethod);
                                                            } catch (NoSuchAlgorithmException e) {
                                                                System.err.println("Cannot hash mask");
                                                            }
                                                        }
                                                        long hashTime = System.currentTimeMillis() - startTime;
                                                        if (DebugUtil.DEBUG) {
                                                            System.out.printf(
                                                                    "Entry Done: function time %4d ms; hash time %4d ms; %s(%d); %s  -> %s\n",
                                                                    functionTime, hashTime, executingMask.getName(),
                                                                    index, finalCallingLine, finalCallingMethod);
                                                        }
                                                        executingMask.setVisualDebug(visualDebug);
                                                        if ((DebugUtil.DEBUG && visualDebug) || (DebugUtil.VISUALIZE
                                                                                                 &&
                                                                                                 !executingMask.isMock())) {
                                                            VisualDebugger.visualizeMask(executingMask,
                                                                                         finalCallingMethod,
                                                                                         finalCallingLine);
                                                        }
                                                    }, executorService);

        Entry entry = new Entry(index, executingMask, entryDependencies, newFuture, callingMethod, callingLine);

        entry.dependencies.forEach(dependency -> dependency.dependants.add(entry));
        pipeline.add(entry);
    }

    public static boolean isRunning() {
        return started.isDone();
    }

    public static List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks, Mask<?, ?> executingMask) {
        requiredMasks = new ArrayList<>(requiredMasks);
        if (!requiredMasks.contains(executingMask)) {
            requiredMasks.add(executingMask);
        }
        return getDependencyList(requiredMasks);
    }

    public static List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks) {
        List<Entry> dependencies = new ArrayList<>();

        for (Mask<?, ?> requiredMask : requiredMasks) {
            getMostRecentEntryForMask(requiredMask).ifPresent(dependencies::add);
        }
        return dependencies;
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyList list of dependencies
     * @return future that completes when all dependent futures are completed
     */
    private static CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(List<Entry> dependencyList) {
        if (pipeline.isEmpty() || dependencyList.isEmpty()) {
            return started;
        }

        CompletableFuture<?>[] futures = dependencyList.stream()
                                                       .map(Entry::getFuture)
                                                       .toArray(CompletableFuture<?>[]::new);

        if (futures.length == 0) {
            return started;
        }

        return CompletableFuture.allOf(futures)
                                .thenApplyAsync(aVoid -> dependencyList.stream()
                                                                       .map(Entry::getResult)
                                                                       .collect(Collectors.toList()), executorService);
    }

    public static Optional<Entry> getMostRecentEntryForMask(Mask<?, ?> mask) {
        return pipeline.stream()
                       .filter(entry -> mask.equals(entry.getExecutingMask()))
                       .reduce((first, second) -> second);
    }

    public static void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[getPipelineSize()];

        if (DebugUtil.DEBUG) {
            pipeline.forEach(entry -> System.out.printf(
                    "Pipeline entry: %s;\tdependencies:[%s];\tdependants:[%s];\texecuteMask %s;\tLine: %s;\t Method: %s\n",
                    entry.toString(),
                    entry.getDependencies().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getDependants().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getExecutingMask().getName(), entry.getLine(), entry.getMethodName()));
        }
        started.complete(null);
    }

    public static int getPipelineSize() {
        return pipeline.size();
    }

    public static void join() {
        pipeline.forEach(e -> e.getFuture().join());
        System.out.println("Pipeline completed!");
    }

    public static void await(Mask<?, ?>... masks) {
        if (!isRunning()) {
            throw new IllegalStateException("Pipeline not started cannot await");
        }
        getDependencyList(Arrays.asList(masks)).forEach(e -> e.getFuture().join());
    }

    public static void toFile(Path path) throws IOException {
        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        FileOutputStream out = new FileOutputStream(outFile);
        for (String s : hashArray) {
            if (s != null) {
                out.write(s.getBytes());
            }
        }
        out.flush();
        out.close();
    }

    public static String[] getHashArray() {
        return hashArray.clone();
    }

    public static void shutdown() {
        executorService.shutdown();
    }

    private static void abort() {
        executorService.shutdownNow();
    }

    @Getter
    public static class Entry {
        private final Mask<?, ?> executingMask;
        private final Set<Entry> dependencies = new HashSet<>();
        private final CompletableFuture<Void> future;
        private final Set<Entry> dependants = new HashSet<>();
        private final int index;
        private final String methodName;
        private final String line;
        private Mask<?, ?> immutableResult;

        public Entry(int index, Mask<?, ?> executingMask, Collection<Entry> dependencies,
                     CompletableFuture<Void> future, String method, String line) {
            this.index = index;
            this.executingMask = executingMask;
            this.dependencies.addAll(dependencies);
            this.methodName = method;
            this.line = line;
            this.future = future.thenRunAsync(() -> {
                if (!executingMask.isMock() && dependants.stream()
                                                         .anyMatch(entry -> !entry.getExecutingMask()
                                                                                  .equals(executingMask))) {
                    immutableResult = executingMask.immutableCopy();
                } else {
                    immutableResult = executingMask;
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                shutdown();
                return null;
            });
        }

        public Mask<?, ?> getResult() {
            if (!future.isDone()) {
                throw new IllegalStateException("Entry not done computing");
            }
            return immutableResult;
        }

        public String toString() {
            return String.format("%s(%d)", executingMask.getName(), index);
        }
    }
}
