package util;

import generator.MapGenerator;
import map.ConcurrentBinaryMask;
import map.ConcurrentFloatMask;
import map.ConcurrentMask;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public strictfp class Pipeline {

    public static CompletableFuture<List<ConcurrentMask>> started = new CompletableFuture<>();
    private static final List<Entry> pipeline = new ArrayList<>();

    public static ConcurrentBinaryMask add(ConcurrentBinaryMask executingMask, List<ConcurrentMask> dep, Function<List<ConcurrentMask>, ?> function) {
        addInternal(executingMask, dep, function);
        return executingMask;
    }

    public static ConcurrentFloatMask add(ConcurrentFloatMask executingMask, List<ConcurrentMask> dep, Function<List<ConcurrentMask>, ?> function) {
        addInternal(executingMask, dep, function);
        return executingMask;
    }

    private static void addInternal(ConcurrentMask executingMask, List<ConcurrentMask> dep, Function<List<ConcurrentMask>, ?> function) {
        int index = pipeline.size();
        boolean addedAfterPipelineStart = Pipeline.isStarted();

        List<Pipeline.Entry> dependencies = Pipeline.getDependencyList(dep);
        CompletableFuture<?> newFuture = Pipeline.getDependencyFuture(dependencies, executingMask)
                .thenApply(res -> {
                    System.out.printf("Start: %s(%d)\n", executingMask.getName(), index);
                    if (addedAfterPipelineStart && !executingMask.getName().equals("mocked") && !executingMask.getName().equals("new binary mask") && !executingMask.getName().equals("new float mask")) {
                        System.err.println("Running non deterministic task added after pipeline start!  " + executingMask.getName());
                    }
                    return res;
                })
                .thenApplyAsync(function)
                .thenRun(() -> {
                    System.out.printf("Done: %s(%d)\n", executingMask.getName(), index);
                    if (MapGenerator.DEBUG) {
                        executingMask.writeToFile(Paths.get(".", "debug", index + ".mask"));
                    }
                });
        Entry entry = new Entry(index, executingMask, dependencies, newFuture);

        entry.dependencies.forEach(d -> d.dependants.add(entry));
        entry.index = pipeline.size();
        pipeline.add(entry);


        System.out.printf("%d: New pipeline entry:   %s,  %s,  deps:[%s]\n",
                index,
                executingMask.getName(),
                new Throwable().getStackTrace()[2].getMethodName(),
                dependencies.stream().map(e -> e.getExecutingMask().getName() + "(" + pipeline.indexOf(e) + ")").reduce((acc, r) -> acc + ", " + r).orElse("none")
        );
    }

    public static void start() {
        System.out.println("Starting pipeline");
        started.complete(null);
    }

    public static void stop() {
        pipeline.forEach(e -> e.getFuture().join());
        System.out.println("pipeline stopped!");
    }

    public static boolean isStarted() {
        return started.isDone();
    }

    public static void await(ConcurrentMask... masks) {
        getDependencyList(Arrays.asList(masks)).get(0).getFuture().join();
        getDependencyList(Arrays.asList(masks)).forEach(e -> e.getFuture().join());
    }

    public static List<Entry> getDependencyList(List<ConcurrentMask> requiredMasks) {
        List<Entry> res = new ArrayList<>();


        for (ConcurrentMask requiredMask : requiredMasks) {
            for (int i = pipeline.size() - 1; i >= 0; i--) {
                if (requiredMask == pipeline.get(i).executingMask) {
                    res.add(pipeline.get(i));
                    break;
                }
            }
        }

//		if(requiredMasks.size() != res.size()) {
//			throw new RuntimeException("Unmet dependency!");
//		}

        return res;
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyList
     * @return a list of the results, DO NOT MODIFY THOSE!, may be mocks
     */
    public static CompletableFuture<List<ConcurrentMask>> getDependencyFuture(List<Entry> dependencyList, ConcurrentMask requestingMask) {
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

    public static strictfp class Entry {
        private int index;
        private final ConcurrentMask executingMask;
        private final Set<Entry> dependencies;
        private final CompletableFuture<?> future;
        private final Set<Entry> dependants = new HashSet<>();

        private final List<ConcurrentMask> maskBackups = new ArrayList<>();

        public Entry(int index, ConcurrentMask executingMask, Collection<Entry> dependencies, CompletableFuture<?> future) {
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

        public synchronized ConcurrentMask getResult(ConcurrentMask requestingMask) {
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

        public ConcurrentMask getExecutingMask() {
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
