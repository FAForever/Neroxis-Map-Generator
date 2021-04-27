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
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public strictfp class Pipeline {

    public static boolean HASH_MASK = false;

    private static final List<Entry<?>> pipeline = new ArrayList<>();
    private static CompletableFuture<List<Mask<?, ?>>> started = new CompletableFuture<>();
    private static String[] hashArray;

    public static void reset() {
        started = new CompletableFuture<>();
        pipeline.clear();
    }

    public static <V extends Mask<?, ?>> void add(Mask<?, ?> executingMask, V resultMask, List<Mask<?, ?>> maskDependencies, Function<List<Mask<?, ?>>, V> function) {
        int index = pipeline.size();
        if (isStarted()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingLine = Util.getStackTraceLineInPackage("com.faforever.neroxis.map.generator");
        String callingMethod = Util.getStackTraceMethodInPackage("com.faforever.neroxis.map", "enqueue");

        List<Entry<?>> entryDependencies = Pipeline.getDependencyList(maskDependencies, executingMask);
        CompletableFuture<V> newFuture = Pipeline.getDependencyFuture(entryDependencies, resultMask)
                .thenApplyAsync(inputs -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        V result = function.apply(inputs);
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
                        return result;
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.printf("Pipeline threw error computing %s%n", executingMask.getName());
                        System.out.printf("Entry id %d, method %s, line %s%n", index, callingMethod, callingLine);
                        System.out.printf("Expected dependencies: %s%n", maskDependencies.stream().map(Mask::getName).collect(Collectors.joining(", ")));
                        if (inputs != null) {
                            System.out.printf("Received dependencies: %s%n", inputs.stream().map(Mask::getName).collect(Collectors.joining(", ")));
                        }
                        Pipeline.cancel();
                        return null;
                    }
                });

        Entry<V> entry = new Entry<>(index, executingMask, resultMask, entryDependencies, newFuture, callingMethod, callingLine);

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

    private static void cancel() {
        pipeline.forEach(e -> e.getFuture().cancel(true));
        System.out.println("Pipeline completed!");
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

    public static List<Entry<?>> getDependencyList(List<Mask<?, ?>> requiredMasks, Mask<?, ?> executingMask) {
        requiredMasks = new ArrayList<>(requiredMasks);
        if (!requiredMasks.contains(executingMask)) {
            requiredMasks.add(executingMask);
        }
        return getDependencyList(requiredMasks);
    }

    public static List<Entry<?>> getDependencyList(List<Mask<?, ?>> requiredMasks) {
        List<Entry<?>> res = new ArrayList<>();

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
    private static CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(List<Entry<?>> dependencyList, Mask<?, ?> resultMask) {
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
    private static strictfp class Entry<V extends Mask<?, ?>> {
        private final Mask<?, ?> executingMask;
        private final V resultMask;
        private final Set<Entry<?>> dependencies;
        private final CompletableFuture<Void> future;
        private final Set<Entry<?>> dependants = new HashSet<>();
        private final int index;
        private final String method;
        private final String line;
        private V backupResult;


        public Entry(int index, Mask<?, ?> executingMask, V resultMask, Collection<Entry<?>> dependencies, CompletableFuture<V> future, String method, String line) {
            this.index = index;
            this.executingMask = executingMask;
            this.resultMask = resultMask;
            this.dependencies = new HashSet<>(dependencies);
            this.method = method;
            this.line = line;
            this.future = future.thenAcceptAsync(result -> {
                backupResult = (V) result.copy();
                VisualDebugger.visualizeMask(resultMask, method, line);
            });
        }

        public V getResult(Mask<?, ?> resultingMask) {
            if (resultingMask.equals(resultMask)) {
                return resultMask;
            }
            V resultCopy = (V) backupResult.copy();
            resultCopy.setVisualDebug(resultMask.isVisualDebug());
            resultCopy.setVisualName(resultMask.getVisualName());
            return resultCopy;
        }
    }
}
