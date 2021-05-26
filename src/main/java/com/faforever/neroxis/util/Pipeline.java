package com.faforever.neroxis.util;

import com.faforever.neroxis.map.mask.Mask;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public strictfp class Pipeline {

    public static boolean HASH_MASK = false;

    private static final List<Entry> pipeline = new ArrayList<>();
    private static CompletableFuture<List<Mask<?, ?>>> started = new CompletableFuture<>();
    private static String[] hashArray;

    public static void reset() {
        started = new CompletableFuture<>();
        pipeline.clear();
    }

    public static void add(Mask<?, ?> executingMask, List<Mask<?, ?>> maskDependencies, Consumer<List<Mask<?, ?>>> function) {
        int index = pipeline.size();
        if (isStarted()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingMethod = null;
        String callingLine = null;

        if (Util.DEBUG) {
            callingMethod = Util.getStackTraceMethodInPackage("com.faforever.neroxis.map", "enqueue");
            callingLine = Util.getStackTraceLineInPackage("com.faforever.neroxis.map.generator");
        }

        List<Entry> entryDependencies = Pipeline.getDependencyList(maskDependencies, executingMask);
        String finalCallingLine = callingLine;
        String finalCallingMethod = callingMethod;
        CompletableFuture<Void> newFuture = Pipeline.getDependencyFuture(entryDependencies, executingMask)
                .thenAcceptAsync(dependencies -> {
                    long startTime = System.currentTimeMillis();
                    boolean visualDebug = executingMask.isVisualDebug();
                    executingMask.setVisualDebug(false);
                    function.accept(dependencies);
                    executingMask.setVisualDebug(visualDebug);
                    long functionTime = System.currentTimeMillis() - startTime;
                    startTime = System.currentTimeMillis();
                    if (HASH_MASK) {
                        try {
                            hashArray[index] = String.format("%s,\t%s,\t%s,\t%s%n", executingMask.toHash(), finalCallingLine, executingMask.getName(), finalCallingMethod);
                        } catch (NoSuchAlgorithmException e) {
                            System.err.println("Cannot hash mask");
                        }
                    }
                    long hashTime = System.currentTimeMillis() - startTime;
                    if (Util.DEBUG) {
                        System.out.printf("Entry Done: function time %4d ms; hash time %4d ms; %s(%d); %s  -> %s\n",
                                functionTime,
                                hashTime,
                                executingMask.getName(),
                                index,
                                finalCallingLine,
                                finalCallingMethod
                        );
                    }
                });

        Entry entry = new Entry(index, executingMask, entryDependencies, newFuture, callingMethod, callingLine);

        entry.dependencies.forEach(d -> d.dependants.add(entry));
        pipeline.add(entry);
    }

    public static void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[getPipelineSize()];
        if (Util.DEBUG) {
            pipeline.forEach(entry -> System.out.printf("Pipeline entry: %s;\tdependencies:[%s];\tdependants:[%s];\texecuteMask %s;\tLine: %s;\t Method: %s\n",
                    entry.toString(),
                    entry.getDependencies().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getDependants().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getExecutingMask().getName(),
                    entry.getLine(),
                    entry.getMethod()
            ));
        }
        started.complete(null);
    }

    public static void join() {
        pipeline.forEach(e -> e.getFuture().join());
        System.out.println("Pipeline completed!");
    }

    public static boolean isStarted() {
        return started.isDone();
    }

    public static void await(Mask<?, ?>... masks) {
        if (!isStarted()) {
            throw new IllegalStateException("Pipeline not started cannot await");
        }
        getDependencyList(Arrays.asList(masks)).forEach(e -> e.getFuture().join());
        for (Mask<?, ?> mask : masks) {
            mask.setParallel(false);
        }
    }

    public static List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks, Mask<?, ?> executingMask) {
        requiredMasks = new ArrayList<>(requiredMasks);
        if (!requiredMasks.contains(executingMask)) {
            requiredMasks.add(executingMask);
        }
        return getDependencyList(requiredMasks);
    }

    public static List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks) {
        List<Entry> res = new ArrayList<>();

        for (Mask<?, ?> requiredMask : requiredMasks) {
            pipeline.stream()
                    .filter(entry -> requiredMask.equals(entry.getExecutingMask()))
                    .reduce((first, second) -> second)
                    .ifPresent(res::add);
        }
        return res;
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyList list of dependencies
     * @return future that completes when all dependent futures are completed
     */
    private static CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(List<Entry> dependencyList, Mask<?, ?> executingMask) {
        if (pipeline.isEmpty() || dependencyList.isEmpty()) {
            return started;
        }

        CompletableFuture<?>[] futures = dependencyList.stream().map(Entry::getFuture).toArray(CompletableFuture<?>[]::new);

        if (futures.length == 0) {
            return started;
        }

        return CompletableFuture.allOf(futures)
                .thenApplyAsync(aVoid ->
                        dependencyList.stream()
                                .map(entry -> entry.getResult(executingMask))
                                .collect(Collectors.toList())
                );
    }

    public static int getPipelineSize() {
        return pipeline.size();
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

    @Getter
    private static strictfp class Entry {
        private final Mask<?, ?> executingMask;
        private final Set<Entry> dependencies;
        private final CompletableFuture<Void> future;
        private final Set<Entry> dependants = new HashSet<>();
        private final int index;
        private final String method;
        private final String line;
        private Mask<?, ?> immutableResult;
        private long resultCount = 0;


        public Entry(int index, Mask<?, ?> executingMask, Collection<Entry> dependencies, CompletableFuture<Void> future, String method, String line) {
            this.index = index;
            this.executingMask = executingMask;
            this.dependencies = new HashSet<>(dependencies);
            this.method = method;
            this.line = line;
            this.future = future.thenRunAsync(() -> {
                if (Util.DEBUG || Util.VISUALIZE) {
                    VisualDebugger.visualizeMask(executingMask, method, line);
                }
                resultCount = dependants.stream().filter(dep -> !executingMask.equals(dep.getExecutingMask())).count();
                if (resultCount > 0) {
                    immutableResult = executingMask.mock();
                }
            });
        }

        public synchronized Mask<?, ?> getResult(Mask<?, ?> requestingMask) {
            if (requestingMask.equals(executingMask)) {
                return executingMask;
            }
            --resultCount;
            Mask<?, ?> result = immutableResult;
            if (resultCount == 0) {
                immutableResult = null;
            } else if (resultCount < 0) {
                throw new IllegalStateException("More results asked for than dependants");
            }
            return result;
        }

        public String toString() {
            return String.format("%s(%d)", executingMask.getName(), index);
        }
    }
}
