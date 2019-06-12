package util;

import lombok.SneakyThrows;

import java.io.File;
import java.net.URISyntaxException;

public class PlatformUtils {
	public static boolean isRunningFromJAR() {
		return FileUtils.class.getResource("FileUtils.class").getProtocol().equals("jar");
	}

	@SneakyThrows(URISyntaxException.class)
	public static File getRunnableJarFile() {
		if(! isRunningFromJAR()) {
			throw new UnsupportedOperationException("Failed to get jar file, we're not running from a jar.");
		}

		return new File(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}
}
