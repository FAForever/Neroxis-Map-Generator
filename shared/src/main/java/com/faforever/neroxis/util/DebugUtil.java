package com.faforever.neroxis.util;

import java.util.Arrays;

public strictfp class DebugUtil {
    public static boolean VERBOSE = false;
    public static boolean DEBUG = false;
    public static boolean VISUALIZE = false;

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

    public static String getStackTraceTopMethodInPackage(String packageName, String... excludedClasses) {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.filter(
                                                                            stackTraceElement -> stackTraceElement.getClassName().contains(packageName)
                                                                                                 && Arrays.stream(excludedClasses)
                                                                                                          .noneMatch(
                                                                                                                  excludedClass -> stackTraceElement.getClassName()
                                                                                                                                                    .contains(
                                                                                                                                                            excludedClass)))
                                                                    .reduce((first, second) -> second)
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

    private static String getStackTraceParentClass() {
        return StackWalker.getInstance()
                          .walk(stackFrameStream -> stackFrameStream.skip(3)
                                                                    .findFirst()
                                                                    .map(StackWalker.StackFrame::getClassName)
                                                                    .orElse("No Parent."));
    }

    public static void timedRun(String packageName, String description, Runnable runnable) {
        long sTime = System.currentTimeMillis();
        if (VERBOSE && DEBUG) {
            System.out.printf("Started %s: %s\n", description, DebugUtil.getStackTraceLineInPackage(packageName));
        }
        runnable.run();
        if (DEBUG) {
            System.out.printf("Done %s: %4d ms, %s\n", description, System.currentTimeMillis() - sTime,
                              DebugUtil.getStackTraceLineInPackage(packageName));
        }
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
                                                                    .map(stackFrame -> stackFrame.getFileName()
                                                                                       + ":"
                                                                                       + stackFrame.getLineNumber())
                                                                    .orElse("not found"));
    }
}
