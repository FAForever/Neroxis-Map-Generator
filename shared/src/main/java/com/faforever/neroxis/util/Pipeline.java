package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.visualization.VisualDebugger;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Pipeline {
    private static final ScopedValue<Pipeline> PIPELINE = ScopedValue.newInstance();
    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("Pipeline");
    private static final ExecutorService PIPELINE_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofPlatform().daemon().group(THREAD_GROUP).name("pipeline-worker-", 0).factory());

    private final List<Entry> entries = new ArrayList<>();
    private final CompletableFuture<List<Mask<?, ?>>> started = new CompletableFuture<>();
    @Setter
    @Getter
    private boolean debug;
    @Getter
    @Setter
    private boolean visualize;

    private Pipeline() {}

    public static List<Pipeline.Entry> run(Runnable setup) {
        return run(_ -> {}, setup);
    }

    public static List<Pipeline.Entry> run(Consumer<Pipeline> pipelineConfigurator, Runnable setup) {
        Pipeline pipeline = new Pipeline();
        pipelineConfigurator.accept(pipeline);
        ScopedValue.where(PIPELINE, pipeline).run(setup);
        pipeline.run();
        return List.copyOf(pipeline.entries);
    }

    public static void add(Mask<?, ?> executingMask, List<Mask<?, ?>> maskDependencies,
                           Consumer<List<Mask<?, ?>>> function) {
        Pipeline thisPipeline = PIPELINE.orElseThrow(() -> new IllegalStateException("Pipeline not bound"));
        thisPipeline.addInternal(executingMask, maskDependencies, function);
    }

    public static boolean isAccepting() {
        return PIPELINE.isBound();
    }

    private void addInternal(Mask<?, ?> executingMask, List<Mask<?, ?>> maskDependencies,
                             Consumer<List<Mask<?, ?>>> function) {
        int index = entries.size();
        if (started.isDone()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingMethod = null;
        String callingLine = null;

        if (isDebug() || isVisualize()) {
            callingMethod = DebugUtil.getLastStackTraceMethodInPackage("com.faforever.neroxis.mask");
            callingLine = DebugUtil.getLastStackTraceLineAfterPackage("com.faforever.neroxis.mask");
        }

        SequencedMap<Mask<?, ?>, Optional<Entry>> entryDependencies = getDependencyMap(maskDependencies, executingMask);
        String finalCallingLine = callingLine;
        String finalCallingMethod = callingMethod;
        CompletableFuture<Void> newFuture = getDependencyFuture(entryDependencies).thenAcceptAsync(dependencies -> {
            long startTime = System.currentTimeMillis();
            boolean visualDebug = executingMask.isVisualDebug();
            executingMask.setVisualDebug(false);
            function.accept(dependencies);
            long functionTime = System.currentTimeMillis() - startTime;
            startTime = System.currentTimeMillis();
            long hashTime = System.currentTimeMillis() - startTime;
            if (isDebug()) {
                System.out.printf("Entry Done: function time %4d ms; hash time %4d ms; %s(%d); %s  -> %s\n",
                                  functionTime, hashTime, executingMask.getName(), index, finalCallingLine,
                                  finalCallingMethod);
            }
            executingMask.setVisualDebug(visualDebug);
            if ((isDebug() && visualDebug) || (isVisualize() && !executingMask.isMock())) {
                VisualDebugger.visualizeMask(executingMask, finalCallingMethod, finalCallingLine, null);
            }
        }, PIPELINE_EXECUTOR_SERVICE);

        Entry entry = new Entry(index, executingMask,
                                entryDependencies.values().stream().flatMap(Optional::stream).toList(), newFuture,
                                callingMethod, callingLine);

        entry.dependencies.forEach(dependency -> dependency.dependants.add(entry));
        entries.add(entry);
    }

    private SequencedMap<Mask<?, ?>, Optional<Entry>> getDependencyMap(List<Mask<?, ?>> requiredMasks,
                                                                       Mask<?, ?> executingMask) {
        requiredMasks = new ArrayList<>(requiredMasks);
        if (!requiredMasks.contains(executingMask)) {
            requiredMasks.add(executingMask);
        }
        return getDependencyMap(requiredMasks);
    }

    private SequencedMap<Mask<?, ?>, Optional<Entry>> getDependencyMap(List<Mask<?, ?>> requiredMasks) {
        return requiredMasks.stream()
                            .collect(Collectors.toMap(Function.identity(), this::getMostRecentEntryForMask, (_, _) -> {
                                throw new IllegalStateException("Multiple entries for the same map");
                            }, LinkedHashMap::new));
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyMap list of dependencies
     * @return future that completes when all dependent futures are completed
     */
    private CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(
            SequencedMap<Mask<?, ?>, Optional<Entry>> dependencyMap) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(started);

        dependencyMap.values().stream()
                     .flatMap(Optional::stream)
                     .map(Entry::getFuture)
                     .forEach(futures::add);

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                                .thenApplyAsync(_ -> dependencyMap.entrySet().stream()
                                                                  .map(entry -> entry.getValue()
                                                                                     .<Mask<?, ?>>map(Entry::getResult)
                                                                                     .orElse(entry.getKey()))
                                                                  .collect(Collectors.toList()),
                                                PIPELINE_EXECUTOR_SERVICE);
    }

    private Optional<Entry> getMostRecentEntryForMask(Mask<?, ?> mask) {
        return entries.reversed().stream().filter(entry -> mask.equals(entry.getExecutingMask())).findFirst();
    }

    private void run() {
        System.out.println("Starting pipeline");

        if (isDebug()) {
            entries.forEach(entry -> System.out.printf(
                    "Pipeline entry: %s;\tdependencies:[%s];\tdependants:[%s];\texecuteMask %s;\tLine: %s;\t Method: %s\n",
                    entry.toString(),
                    entry.getDependencies().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getDependants().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getExecutingMask().getName(), entry.getLine(), entry.getMethodName()));
        }
        started.complete(null);
        CompletableFuture<?>[] futures = entries.stream().map(Entry::getFuture).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
        System.out.println("Pipeline completed!");
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

        private Entry(int index, Mask<?, ?> executingMask, Collection<Entry> dependencies,
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
            }, PIPELINE_EXECUTOR_SERVICE);
        }

        private Mask<?, ?> getResult() {
            if (!future.isDone()) {
                throw new IllegalStateException("Entry not done computing");
            }
            return immutableResult;
        }

        public void write(OutputStream out) throws IOException {
            try {
                out.write(String.format("%s,\t%s,\t%s,\t%s%n", getResult().toHash(), getLine(),
                                        getResult().getName(), getMethodName())
                                .getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        public String toString() {
            return String.format("%s(%d)", executingMask.getName(), index);
        }
    }
}
