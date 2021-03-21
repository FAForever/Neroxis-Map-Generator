package neroxis.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public strictfp class Util {

    public static String getStackTraceLineInOneOfClasses(List<Class<?>> classes) {
        List<String> classNames = classes.stream().map(Class::getCanonicalName).collect(Collectors.toList());
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (classNames.contains(ste.getClassName())) {
                return ste.getFileName() + ":" + ste.getLineNumber();
            }
        }
        return "not found";
    }

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

    public static String getStackTraceParentClass() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return stackTrace[2].getClassName();
    }
}
