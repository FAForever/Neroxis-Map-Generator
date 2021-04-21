package neroxis.util;

import lombok.Getter;
import neroxis.map.Mask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public strictfp class Pipeline {

    public static boolean HASH_MASK = false;

    private static final List<Entry> pipeline = new ArrayList<>();
    private static CompletableFuture<Void> started = new CompletableFuture<>();
    private static String[] hashArray;

    public static void reset() {
        started = new CompletableFuture<>();
        pipeline.clear();
    }

    public static void add(Mask<?> executingMask, List<Mask<?>> maskDependencies, Runnable function) {
        int index = pipeline.size();
        if (isStarted() && !executingMask.getName().equals("mocked") && !executingMask.getName().equals("new binary mask") && !executingMask.getName().equals("new float mask")) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingLine = Util.getStackTraceLineInPackage("neroxis.generator");
        String callingMethod = Util.getStackTraceMethodInPackage("neroxis.map", "execute");

        List<Pipeline.Entry> entryDependencies = Pipeline.getDependencyList(maskDependencies);
        CompletableFuture<Void> newFuture = Pipeline.getDependencyFuture(entryDependencies)
                .thenAccept(m -> {
                    long startTime = System.currentTimeMillis();
                    function.run();
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
                        System.out.printf("Done: function time %4d ms, hash time %4d ms, %s, %s(%d)->%s\n",
                                functionTime,
                                hashTime,
                                callingLine,
                                executingMask.getName(),
                                index,
                                callingMethod
                        );
                    }
                });
        Entry entry = new Entry(index, executingMask, entryDependencies, newFuture);

        entry.dependencies.forEach(d -> d.dependants.add(entry));
        pipeline.add(entry);

        if (Util.DEBUG) {
            System.out.printf("%d: New pipeline entry:   %s,  %s,  deps:[%s]\n",
                    index,
                    executingMask.getName(),
                    Util.getStackTraceMethodInPackage("neroxis.map", "execute"),
                    entryDependencies.stream().map(e -> e.getExecutingMask().getName() + "(" + pipeline.indexOf(e) + ")").reduce((acc, r) -> acc + ", " + r).orElse("none")
            );
        }
    }

    public static void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[getPipelineSize()];
        pipeline.forEach(entry -> entry.getExecutingMask().setProcessing(true));
        started.complete(null);
    }

    public static void join() {
        pipeline.forEach(e -> e.getFuture().join());
        System.out.println("Pipeline completed!");
    }

    public static boolean isStarted() {
        return started.isDone();
    }

    public static void await(Mask<?>... masks) {
        getDependencyList(Arrays.asList(masks)).forEach(e -> e.getFuture().join());
        for (Mask<?> mask : masks) {
            mask.setProcessing(false);
            mask.setParallel(false);
        }
    }

    public static <T extends Mask<?>> List<Entry> getDependencyList(List<T> requiredMasks) {
        List<Entry> res = new ArrayList<>();

        for (T requiredMask : requiredMasks) {
            if (requiredMask.isParallel()) {
                for (int i = pipeline.size() - 1; i >= 0; i--) {
                    if (requiredMask == pipeline.get(i).executingMask) {
                        res.add(pipeline.get(i));
                        break;
                    }
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
    private static CompletableFuture<Void> getDependencyFuture(List<Entry> dependencyList) {
        if (pipeline.isEmpty() || dependencyList.isEmpty()) {
            return started;
        }

        CompletableFuture<?>[] futures = dependencyList.stream().map(Entry::getFuture).toArray(CompletableFuture<?>[]::new);

        if (futures.length == 0) {
            return started;
        }

        return CompletableFuture.allOf(futures);
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
    public static strictfp class Entry {
        private final Mask<?> executingMask;
        private final Set<Entry> dependencies;
        private final CompletableFuture<Void> future;
        private final Set<Entry> dependants = new HashSet<>();
        private final int index;

        public Entry(int index, Mask<?> executingMask, Collection<Entry> dependencies, CompletableFuture<Void> future) {
            this.index = index;
            this.executingMask = executingMask;
            this.dependencies = new HashSet<>(dependencies);
            this.future = future;
        }
    }
}
