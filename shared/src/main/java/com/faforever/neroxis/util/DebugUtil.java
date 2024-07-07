package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.visualization.VisualDebugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class DebugUtil {
    public static boolean VERBOSE = false;
    public static boolean DEBUG = false;
    private static List<String> VISUALIZE;

    public static void allowVisualization() {
        DebugUtil.VISUALIZE = new ArrayList<>();
    }

    public static void visualizeMask(Mask<?, ?> mask) {
        if (DebugUtil.VISUALIZE == null) {
            return;
        }

        DebugUtil.VISUALIZE.add(mask.getName());
    }

    public static void visualizeMask(String maskName) {
        if (DebugUtil.VISUALIZE == null) {
            return;
        }

        DebugUtil.VISUALIZE.add(maskName);
    }

    public static boolean shouldVisualize(Mask<?, ?> mask) {
        return DebugUtil.VISUALIZE != null && DebugUtil.VISUALIZE.contains(mask.getName());
    }

    public static void visualizeIfSet(Mask<?, ?> mask) {
        if (shouldVisualize(mask)) {
            String callingMethod = DebugUtil.getLastStackTraceMethodInPackage("com.faforever.neroxis.mask");
            String callingLine = DebugUtil.getLastStackTraceLineAfterPackage("com.faforever.neroxis.mask");
            VisualDebugger.visualizeMask(mask, callingMethod, callingLine);
        }
    }

    public static void visualizeIfSet(Mask<?, ?> mask, String callingMethod, String callingLine) {
        if (shouldVisualize(mask)) {
            VisualDebugger.visualizeMask(mask, callingMethod, callingLine);
        }
    }

    public static String getStackTraceMethodInPackage(String packageName, String... excludedMethodNames) {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.filter(
                                                                            stackTraceElement -> stackTraceElement.getClassName().startsWith(packageName))
                                                                    .filter(stackTraceElement -> Arrays.stream(
                                                                                                               excludedMethodNames)
                                                                                                       .noneMatch(
                                                                                                               excludedMethod -> stackTraceElement.getMethodName()
                                                                                                                                                  .contains(
                                                                                                                                                          excludedMethod)))
                                                                    .findFirst()
                                                                    .map(StackWalker.StackFrame::getMethodName)
                                                                    .orElse("not found"));
    }

    public static String getLastStackTraceMethodInPackage(String packageName) {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.filter(
                                                                            stackFrame -> stackFrame.getClassName().startsWith(packageName))
                                                                    .reduce(((stackFrame1, stackFrame2) -> stackFrame2))
                                                                    .map(StackWalker.StackFrame::getMethodName)
                                                                    .orElse("not found"));
    }

    public static void timedRun(Runnable runnable) {
        String packageName = getStackTraceParentClass();
        timedRun(packageName.substring(0, packageName.lastIndexOf(".")), null, runnable);
    }

    public static void timedRun(String description, Runnable runnable) {
        String className = getStackTraceParentClass();
        timedRun(className.substring(0, className.lastIndexOf(".")), description, runnable);
    }

    public static <T> T timedRun(Callable<T> callable) {
        String packageName = getStackTraceParentClass();
        return timedRun(packageName.substring(0, packageName.lastIndexOf(".")), null, callable);
    }

    public static <T> T timedRun(String description, Callable<T> callable) {
        String className = getStackTraceParentClass();
        return timedRun(className.substring(0, className.lastIndexOf(".")), description, callable);
    }

    private static String getStackTraceParentClass() {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.skip(3)
                                                                    .findFirst()
                                                                    .map(StackWalker.StackFrame::getClassName)
                                                                    .orElse("No Parent."));
    }

    public static void timedRun(String packageName, String description, Runnable runnable) {
        long sTime = System.nanoTime();
        if (VERBOSE && DEBUG) {
            System.out.printf("Started %s: %s\n", description, DebugUtil.getStackTraceLineInPackage(packageName));
        }
        runnable.run();
        if (DEBUG) {
            System.out.printf("Done %s: %4.2f ms, %s\n", description, (System.nanoTime() - sTime) / 1e6,
                              DebugUtil.getStackTraceLineInPackage(packageName));
        }
    }

    public static <T> T timedRun(String packageName, String description, Callable<T> callable) {
        long sTime = System.nanoTime();
        if (VERBOSE && DEBUG) {
            System.out.printf("Started %s: %s\n", description, DebugUtil.getStackTraceLineInPackage(packageName));
        }
        T result;
        try {
            result = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (DEBUG) {
            System.out.printf("Done %s: %4.2f ms, %s\n", description, (System.nanoTime() - sTime) / 1e6,
                              DebugUtil.getStackTraceLineInPackage(packageName));
        }
        return result;
    }

    public static String getLastStackTraceLineAfterPackage(String packageName) {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.reduce(
                                                                            (stackFrame1, stackFrame2) -> stackFrame1.getClassName().startsWith(packageName) ||
                                                                                                          stackFrame2.getClassName().startsWith(packageName) ?
                                                                                                          stackFrame2 :
                                                                                                          stackFrame1)
                                                                    .map(stackFrame -> stackFrame.getFileName() +
                                                                                       ":" +
                                                                                       stackFrame.getLineNumber())
                                                                    .orElse("not found"));
    }

    public static String getStackTraceLineInPackage(String packageName, String... excludedMethodNames) {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.filter(
                                                                            stackTraceElement -> stackTraceElement.getClassName().startsWith(packageName))
                                                                    .filter(stackTraceElement -> Arrays.stream(
                                                                                                               excludedMethodNames)
                                                                                                       .noneMatch(
                                                                                                               excludedMethod -> stackTraceElement.getMethodName()
                                                                                                                                                  .contains(
                                                                                                                                                          excludedMethod)))
                                                                    .findFirst()
                                                                    .map(stackFrame -> stackFrame.getFileName() +
                                                                                       ":" +
                                                                                       stackFrame.getLineNumber())
                                                                    .orElse("not found"));
    }
}
