package neroxis.util;

import neroxis.generator.MapGenerator;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.ConcurrentFloatMask;
import neroxis.map.ConcurrentMask;

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
import java.util.stream.IntStream;

public strictfp class Pipeline {

    public static boolean HASH_MASK = true;

    private static final List<Entry> pipeline = new ArrayList<>();
    public static CompletableFuture<List<ConcurrentMask<?>>> started = new CompletableFuture<>();
    public static String[] hashArray;

    public static void reset() {
        started = new CompletableFuture<>();
        pipeline.clear();
    }

    public static ConcurrentBinaryMask add(ConcurrentBinaryMask executingMask, List<ConcurrentMask<?>> dep, Function<List<ConcurrentMask<?>>, ?> function) {
        addInternal(executingMask, dep, function);
        return executingMask;
    }

    public static ConcurrentFloatMask add(ConcurrentFloatMask executingMask, List<ConcurrentMask<?>> dep, Function<List<ConcurrentMask<?>>, ?> function) {
        addInternal(executingMask, dep, function);
        return executingMask;
    }

    private static void addInternal(ConcurrentMask<?> executingMask, List<ConcurrentMask<?>> dep, Function<List<ConcurrentMask<?>>, ?> function) {
        int index = pipeline.size();
        boolean addedAfterPipelineStart = Pipeline.isStarted();
        final String callingLine = Util.getStackTraceLineInClass(MapGenerator.class);
        final String callingMethod = Util.getStackTraceMethod(executingMask.getClass());

        List<Pipeline.Entry> dependencies = Pipeline.getDependencyList(dep);
        CompletableFuture<?> newFuture = Pipeline.getDependencyFuture(dependencies, executingMask)
                .thenApply(res -> {
                    if (addedAfterPipelineStart && !executingMask.getName().equals("mocked") && !executingMask.getName().equals("new binary mask") && !executingMask.getName().equals("new float mask")) {
                        System.err.println("Running non deterministic task added after pipeline start!  " + executingMask.getName());
                    }
                    return res;
                })
                .thenApplyAsync(m -> {
                    long startTime = System.currentTimeMillis();
                    Object res = function.apply(m);
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
                    if (MapGenerator.DEBUG) {
                        System.out.printf("Done: function time %4d ms, hash time %4d ms, %s, %s(%d)->%s\n",
                                functionTime,
                                hashTime,
                                callingLine,
                                executingMask.getName(),
                                index,
                                callingMethod
                        );
                    }
                    return res;
                })
                .thenRun(() -> {

                });
        Entry entry = new Entry(index, executingMask, dependencies, newFuture);

        entry.dependencies.forEach(d -> d.dependants.add(entry));
        entry.index = pipeline.size();
        pipeline.add(entry);

        if (MapGenerator.DEBUG) {
            System.out.printf("%d: New pipeline entry:   %s,  %s,  deps:[%s]\n",
                    index,
                    executingMask.getName(),
                    new Throwable().getStackTrace()[2].getMethodName(),
                    dependencies.stream().map(e -> e.getExecutingMask().getName() + "(" + pipeline.indexOf(e) + ")").reduce((acc, r) -> acc + ", " + r).orElse("none")
            );
        }

    }

    public static void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[getPipelineSize()];
        started.complete(null);
    }

    public static void stop() {
        pipeline.forEach(e -> e.getFuture().join());
        System.out.println("pipeline stopped!");
    }

    public static boolean isStarted() {
        return started.isDone();
    }

    public static void await(ConcurrentMask<?>... masks) {
        getDependencyList(Arrays.asList(masks)).get(0).getFuture().join();
        getDependencyList(Arrays.asList(masks)).forEach(e -> e.getFuture().join());
    }

    public static List<Entry> getDependencyList(List<ConcurrentMask<?>> requiredMasks) {
        List<Entry> res = new ArrayList<>();


        for (ConcurrentMask<?> requiredMask : requiredMasks) {
            for (int i = pipeline.size() - 1; i >= 0; i--) {
                if (requiredMask == pipeline.get(i).executingMask) {
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
     * @return a list of the results, DO NOT MODIFY THOSE!, may be mocks
     */
    public static CompletableFuture<List<ConcurrentMask<?>>> getDependencyFuture(List<Entry> dependencyList, ConcurrentMask<?> requestingMask) {
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
                                .map(e -> e.getResult(requestingMask))
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

    public static strictfp class Entry {
        private final ConcurrentMask<?> executingMask;
        private final Set<Entry> dependencies;
        private final CompletableFuture<?> future;
        private final Set<Entry> dependants = new HashSet<>();
        private final List<ConcurrentMask<?>> maskBackups = new ArrayList<>();
        private int index;

        public Entry(int index, ConcurrentMask<?> executingMask, Collection<Entry> dependencies, CompletableFuture<?> future) {
            this.index = index;
            this.executingMask = executingMask;
            this.dependencies = new HashSet<>(dependencies);
            this.future = future.thenRun(() -> {
                if (dependants.size() > 0) {
                    if (dependants.stream().anyMatch(d -> d.getExecutingMask() == this.executingMask)) {
                        IntStream.range(0, dependants.size() - 1).forEach(i -> maskBackups.add(executingMask.mockClone()));
                    } else {
                        IntStream.range(0, dependants.size()).forEach(i -> maskBackups.add(executingMask.mockClone()));
                    }
                }
            });
        }

        public synchronized ConcurrentMask<?> getResult(ConcurrentMask<?> requestingMask) {
            if (requestingMask == executingMask) {
                return executingMask;
            } /*else if(dependants.size() == 1) {
				return executingMask;
			} */ else {
                if (maskBackups.isEmpty()) {
                    new RuntimeException(String.format("No backup mask left: %d, requested from: %s", index, requestingMask.getName())).printStackTrace();
                    return null;
                }
                return maskBackups.remove(0);
            }
        }

        public CompletableFuture<?> getFuture() {
            return future;
        }

        public ConcurrentMask<?> getExecutingMask() {
            return executingMask;
        }

        public Set<Entry> getDependencies() {
            return dependencies;
        }

        public Set<Entry> getDependants() {
            return dependants;
        }

        public int getIndex() {
            return index;
        }
    }
}
