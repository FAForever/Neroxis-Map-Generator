package neroxis.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import neroxis.map.TerrainMaterials;
import neroxis.util.serialized.TerrainMaterialsAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileUtils {

    private static final String DIRECTORY_PATTERN = "%s/[^/]+/";
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(TerrainMaterials.class, new TerrainMaterialsAdapter()).setPrettyPrinting().create();

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
     * @param filePath the filePath to the directory where the file is located as a String
     * @return the content of the file
     */
    public static byte[] readResource(String filePath) throws IOException {
        InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(filePath);
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

    /**
     * Deserializes a file
     *
     * @param path the file to be read, either as path if to be read from the file system or as String if to be read from inside the currently running jar
     * @return the deserialized object
     */
    public static <T> T deserialize(String path, Class<?> clazz) throws IOException {
        String content = new String(readResource(path));

        return (T) gson.fromJson(content, clazz);
    }

    public static <T> void serialize(String filename, T obj, Class<?> clazz) throws IOException {
        FileWriter file = new FileWriter(filename);
        file.write(gson.toJson(obj, clazz));
        file.flush();
        file.close();

    }
}
