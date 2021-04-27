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

    public static void add(Mask<?, ?> executingMask, Mask<?, ?> resultMask, List<Mask<?, ?>> maskDependencies, Consumer<List<Mask<?, ?>>> function) {
        int index = pipeline.size();
        if (isStarted()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingLine = Util.getStackTraceLineInPackage("com.faforever.neroxis.map.generator");
        String callingMethod = Util.getStackTraceMethodInPackage("com.faforever.neroxis.map", "enqueue");

        List<Entry> entryDependencies = Pipeline.getDependencyList(maskDependencies, executingMask);
        CompletableFuture<Void> newFuture = Pipeline.getDependencyFuture(entryDependencies, resultMask)
                .thenAcceptAsync(inputs -> {
                    long startTime = System.currentTimeMillis();
                    function.accept(inputs);
                    long functionTime = System.currentTimeMillis() - startTime;
                    startTime = System.currentTimeMillis();
                    if (HASH_MASK) {
                        try {
                            hashArray[index] = String.format("%s,\t%s,\t%s,\t%s%n", executingMask.toHash(), callingLine, executingMask.getName(), callingMethod);
                        } catch (NoSuchAlgorithmException e) {
                            System.err.println("Cannot hash mask");
                        }
                    }
                    long hashTime = System.currentTimeMillis() - startTime;
                    if (Util.DEBUG) {
                        System.out.printf("Done: function time %4d ms, hash time %4d ms, %s,\t %s(%d)\t->\t%s\n",
                                functionTime,
                                hashTime,
                                callingLine,
                                executingMask.getName(),
                                index,
                                callingMethod
                        );
                    }
                });

        Entry entry = new Entry(index, executingMask, resultMask, entryDependencies, newFuture, callingMethod, callingLine);

        entry.dependencies.forEach(d -> d.dependants.add(entry));
        pipeline.add(entry);
    }

    public static void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[getPipelineSize()];
        if (Util.DEBUG) {
            pipeline.forEach(entry -> System.out.printf("%d Pipeline entry:  %s,\t  %s,\t executor %s(%d);\t resultor %s;\t parents:[%s];\t  children:[%s]\n",
                    entry.getIndex(),
                    entry.getLine(),
                    entry.getMethod(),
                    entry.getExecutingMask().getName(),
                    entry.getIndex(),
                    entry.getResultMask().getName(),
                    entry.getDependencies().stream().map(e -> e.getExecutingMask().getName() + "(" + pipeline.indexOf(e) + ")").reduce((acc, r) -> acc + ", " + r).orElse("none"),
                    entry.getDependants().stream().map(e -> e.getExecutingMask().getName() + "(" + pipeline.indexOf(e) + ")").reduce((acc, r) -> acc + ", " + r).orElse("none")
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
            for (int i = pipeline.size() - 1; i >= 0; i--) {
                if (requiredMask.equals(pipeline.get(i).resultMask)) {
                    res.add(pipeline.get(i));
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyList list of dependencies
     * @return future that completes when all dependent futures are completed
     */
    private static CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(List<Entry> dependencyList, Mask<?, ?> resultMask) {
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
                                .map(entry -> entry.getResult(resultMask))
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
        private final Mask<?, ?> resultMask;
        private final Set<Entry> dependencies;
        private final CompletableFuture<Void> future;
        private final Set<Entry> dependants = new HashSet<>();
        private final List<Mask<?, ?>> maskCopies = new ArrayList<>();
        private final int index;
        private final String method;
        private final String line;


        public Entry(int index, Mask<?, ?> executingMask, Mask<?, ?> resultMask, Collection<Entry> dependencies, CompletableFuture<Void> future, String method, String line) {
            this.index = index;
            this.executingMask = executingMask;
            this.resultMask = resultMask;
            this.dependencies = new HashSet<>(dependencies);
            this.method = method;
            this.line = line;
            this.future = future.thenRunAsync(() -> {
                VisualDebugger.visualizeMask(resultMask, method, line);
                dependants.stream().filter(dep -> !resultMask.equals(dep.getResultMask())).forEach(dep -> maskCopies.add(resultMask.copy()));
            });
        }

        public synchronized Mask<?, ?> getResult(Mask<?, ?> resultingMask) {
            if (resultingMask.equals(resultMask)) {
                return resultMask;
            }
            if (maskCopies.isEmpty()) {
                throw new RuntimeException(String.format("No backup mask left: %d, requested from: %s", index, resultingMask.getName()));
            }
            return maskCopies.remove(0);
        }
    }
}
