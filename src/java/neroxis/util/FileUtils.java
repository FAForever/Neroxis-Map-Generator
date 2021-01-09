package neroxis.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import neroxis.map.TerrainMaterials;
import neroxis.util.serialized.TerrainMaterialsAdapter;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static String readFile(String filePath) throws IOException {
        BufferedReader bufferedReader;
        InputStream inputStream;
        URL resource;
        if ((inputStream = FileUtils.class.getResourceAsStream(filePath)) != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } else if ((resource = FileUtils.class.getResource(filePath)) != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(resource.openStream()));
        } else {
            bufferedReader = new BufferedReader(new FileReader(Paths.get(filePath).toFile()));
        }

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Deserializes a file
     *
     * @param path the file to be read
     * @return the deserialized object
     */
    public static <T> T deserialize(String path, Class<?> clazz) throws IOException {
        return (T) gson.fromJson(readFile(path), clazz);
    }

    public static <T> void serialize(String filename, T obj, Class<?> clazz) throws IOException {
        FileWriter file = new FileWriter(filename);
        file.write(gson.toJson(obj, clazz));
        file.flush();
        file.close();

    }
}
