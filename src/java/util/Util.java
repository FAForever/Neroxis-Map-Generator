package util;

public class Util {

	public static String getStackTraceLineInClass(Class clazz) {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for(StackTraceElement ste : stackTrace) {
			if(ste.getClassName().equals(clazz.getCanonicalName())) {
				return clazz.getSimpleName() + ".java:" + ste.getLineNumber();
			}
		}
		return "not found";
	}

	public static String getStackTraceMethod(Class clazz) {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for(StackTraceElement ste : stackTrace) {
			if(ste.getClassName().equals(clazz.getCanonicalName())) {
				return ste.getMethodName();
			}
		}
		return "not found";
	}
}
