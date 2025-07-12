package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.visualization.VisualDebugger;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("Pipeline");
    private static final ExecutorService PIPELINE_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofPlatform().daemon().group(THREAD_GROUP).name("pipeline-worker-", 0).factory());

    private final List<Entry> pipeline = new ArrayList<>();
    private final CompletableFuture<List<Mask<?, ?>>> started = new CompletableFuture<>();
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private String[] hashArray;
    @Getter
    private boolean debug;
    @Getter
    @Setter
    private boolean visualize;
    @Getter
    @Setter
    private boolean hashMasks;

    public void add(Mask<?, ?> executingMask, List<Mask<?, ?>> maskDependencies, Consumer<List<Mask<?, ?>>> function) {
        int index = pipeline.size();
        if (isStarted()) {
            throw new UnsupportedOperationException("Mask added after pipeline started");
        }
        String callingMethod = null;
        String callingLine = null;

        if (isDebug() || isVisualize()) {
            callingMethod = DebugUtil.getLastStackTraceMethodInPackage("com.faforever.neroxis.mask");
            callingLine = DebugUtil.getLastStackTraceLineAfterPackage("com.faforever.neroxis.mask");
        }

        List<Entry> entryDependencies = getDependencyList(maskDependencies, executingMask);
        String finalCallingLine = callingLine;
        String finalCallingMethod = callingMethod;
        CompletableFuture<Void> newFuture = getDependencyFuture(entryDependencies).thenAcceptAsync(dependencies -> {
            long startTime = System.currentTimeMillis();
            boolean visualDebug = executingMask.isVisualDebug();
            executingMask.setVisualDebug(false);
            function.accept(dependencies);
            long functionTime = System.currentTimeMillis() - startTime;
            startTime = System.currentTimeMillis();
            if (isHashMasks()) {
                try {
                    hashArray[index] = String.format("%s,\t%s,\t%s,\t%s%n", executingMask.toHash(), finalCallingLine,
                                                     executingMask.getName(), finalCallingMethod);
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Cannot hash mask");
                }
            }
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

        Entry entry = new Entry(index, executingMask, entryDependencies, newFuture, callingMethod, callingLine);

        entry.dependencies.forEach(dependency -> dependency.dependants.add(entry));
        pipeline.add(entry);
    }

    public boolean isStarted() {
        return started.isDone();
    }

    public boolean isDone() {
        return done.isDone();
    }

    private List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks, Mask<?, ?> executingMask) {
        requiredMasks = new ArrayList<>(requiredMasks);
        if (!requiredMasks.contains(executingMask)) {
            requiredMasks.add(executingMask);
        }
        return getDependencyList(requiredMasks);
    }

    private List<Entry> getDependencyList(List<Mask<?, ?>> requiredMasks) {
        List<Entry> entries = requiredMasks.stream()
                                           .map(Mask::getMostRecentEntry)
                                           .flatMap(Optional::stream)
                                           .toList();
        return entries;
    }

    /**
     * Returns a future that completes once all dependencies are met and returns their result
     *
     * @param dependencyList list of dependencies
     * @return future that completes when all dependent futures are completed
     */
    private CompletableFuture<List<Mask<?, ?>>> getDependencyFuture(List<Entry> dependencyList) {
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
                                                                       .collect(Collectors.toList()),
                                                PIPELINE_EXECUTOR_SERVICE);
    }

    public Optional<Entry> getMostRecentEntryForMask(Mask<?, ?> mask) {
        return pipeline.reversed().stream().filter(entry -> mask.equals(entry.getExecutingMask())).findFirst();
    }

    public void start() {
        System.out.println("Starting pipeline");
        hashArray = new String[pipeline.size()];

        if (isDebug()) {
            pipeline.forEach(entry -> System.out.printf(
                    "Pipeline entry: %s;\tdependencies:[%s];\tdependants:[%s];\texecuteMask %s;\tLine: %s;\t Method: %s\n",
                    entry.toString(),
                    entry.getDependencies().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getDependants().stream().map(Entry::toString).collect(Collectors.joining(", ")),
                    entry.getExecutingMask().getName(), entry.getLine(), entry.getMethodName()));
        }
        started.complete(null);
        CompletableFuture[] futures = pipeline.stream().map(Entry::getFuture).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).thenRun(() -> done.complete(null));
    }

    public void join() {
        done.join();
        System.out.println("Pipeline completed!");
    }

    public void await(Mask<?, ?>... masks) {
        if (!isStarted()) {
            throw new IllegalStateException("Pipeline not started cannot await");
        }
        CompletableFuture<?>[] futures = getDependencyList(List.of(masks)).stream()
                                                                          .map(Entry::getFuture)
                                                                          .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    public void write(OutputStream out) throws IOException {
        for (String s : hashArray) {
            if (s != null) {
                out.write(s.getBytes());
            }
        }
        out.flush();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.hashMasks = debug;
    }

    public String[] getHashArray() {
        return hashArray.clone();
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
