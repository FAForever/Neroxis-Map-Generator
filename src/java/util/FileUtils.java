package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import map.TerrainMaterials;
import util.serialized.TerrainMaterialsAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    private static final String DIRECTORY_PATTERN = "%s/[^/]+/";
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(TerrainMaterials.class, new TerrainMaterialsAdapter()).create();

    @SneakyThrows
    public static void deleteRecursiveIfExists(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            Stream<Path> files = Files.list(path);
            files.forEach(FileUtils::deleteRecursiveIfExists);
            files.close();
        }

        Files.delete(path);
    }

    /**
     * Reads an entire file
     *
     * @param path the path to the directory where the file is located, either as path if to be read from the file system or as String if to be read from inside the currently running jar
     * @param file the file to be read
     * @return the content of the file
     */
    public static byte[] readFully(Object path, String file) throws IOException {
        if (path instanceof Path) {
            return Files.readAllBytes(((Path) path).resolve(file));
        }

        if (path instanceof String) {
            if (!((String) path).endsWith("/")) {
                path = path + "/";
            }

            InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(path + file);
            assert inputStream != null;
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int bytesRead;
            byte[] buffer = new byte[1024];
            while (-1 != (bytesRead = dataInputStream.read(buffer, 0, 1024))) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }

        throw new IllegalArgumentException("Unknown file type: " + file.getClass());
    }

    /**
     * Deserializes a file
     *
     * @param file the file to be read, either as path if to be read from the file system or as String if to be read from inside the currently running jar
     * @return the deserialized object
     */
    public static <T> T deserialize(Object path, String file, Class<?> clazz) throws IOException {
        String content = new String(readFully(path, file));

        return (T) gson.fromJson(content, clazz);
    }

    public static <T> void serialize(String filename, T obj, Class<?> clazz) throws IOException {
        FileWriter file = new FileWriter(filename);
        file.write(gson.toJson(obj, clazz));
        file.flush();
        file.close();

    }

    public static List<String> listFilesInZipDirectory(String dir, File zipFile) throws IOException {
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        Pattern pattern = Pattern.compile(String.format(DIRECTORY_PATTERN, dir));
        return listFilesInZip(zipFile).stream()
                .filter(f -> pattern.matcher(f).matches())
                .collect(Collectors.toList());
    }

    public static List<String> listFilesInZip(File zipFile) throws IOException {
        List<String> files = new LinkedList<>();

        ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

        ZipEntry entry;
        while ((entry = inputStream.getNextEntry()) != null) {
            files.add(entry.getName());
        }

        return Collections.unmodifiableList(files);
    }
}
