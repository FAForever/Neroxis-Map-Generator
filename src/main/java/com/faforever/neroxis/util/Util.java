package com.faforever.neroxis.util;

import java.util.Arrays;
import java.util.LinkedHashSet;

public strictfp class Util {

    public static boolean VERBOSE = false;
    public static boolean DEBUG = false;
    public static boolean VISUALIZE = false;

    public static String getStackTraceLineInClass(Class<?> clazz) {
        return getStackTraceLineInClass(clazz.getCanonicalName());
    }

    public static String getStackTraceLineInClass(String className) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().equals(className)) {
                return ste.getFileName() + ":" + ste.getLineNumber();
            }
        }
        return "not found";
    }

    public static String getStackTraceLineInPackage(String packageName) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            String className = ste.getClassName();
            String packName = className.substring(0, className.lastIndexOf("."));
            if (packName.startsWith(packageName)) {
                return ste.getFileName() + ":" + ste.getLineNumber();
            }
        }
        return "not found";
    }

    public static String getStackTraceMethod(Class<?> clazz) {
        return getStackTraceMethod(clazz.getCanonicalName());
    }

    public static String getStackTraceMethod(String className) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().equals(className)) {
                return ste.getMethodName();
            }
        }
        return "not found";
    }

    public static String getStackTraceMethodInPackage(String packageName, String... excludedMethodNames) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().contains(packageName) && Arrays.stream(excludedMethodNames).noneMatch(excluded -> ste.getMethodName().equals(excluded))) {
                return ste.getMethodName();
            }
        }
        return "not found";
    }

    public static LinkedHashSet<String> getStackTraceMethods(Class<?> clazz) {
        return getStackTraceMethods(clazz.getCanonicalName());
    }

    public static LinkedHashSet<String> getStackTraceMethods(String className) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        LinkedHashSet<String> methods = new LinkedHashSet<>();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getClassName().equals(className)) {
                methods.add(ste.getMethodName());
            }
        }
        return methods;
    }

    private static String getStackTraceParentClass() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return stackTrace[2].getClassName();
    }

    public static void timedRun(Runnable runnable) {
        String packageName = getStackTraceParentClass();
        timedRun(packageName.substring(0, packageName.lastIndexOf(".")), null, runnable);
    }

    public static void timedRun(String description, Runnable runnable) {
        String packageName = getStackTraceParentClass();
        timedRun(packageName.substring(0, packageName.lastIndexOf(".")), description, runnable);
    }

    public static void timedRun(String packageName, String description, Runnable runnable) {
        long sTime = System.currentTimeMillis();
        if (VERBOSE && DEBUG) {
            System.out.printf("Started %s: %s\n",
                    description,
                    Util.getStackTraceLineInPackage(packageName));
        }
        runnable.run();
        if (DEBUG) {
            System.out.printf("Done %s: %4d ms, %s\n",
                    description,
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInPackage(packageName));
        }
    }
}
