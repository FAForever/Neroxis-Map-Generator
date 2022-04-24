package com.faforever.neroxis.util;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.PrettifyOutputStream;
import com.dslplatform.json.runtime.Settings;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.SneakyThrows;

public strictfp class FileUtil {

    private static final DslJson<Object> DSL_JSON = new DslJson<>(Settings.basicSetup());

    @SneakyThrows
    public static void deleteRecursiveIfExists(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            Stream<Path> files = Files.list(path);
            files.forEach(FileUtil::deleteRecursiveIfExists);
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
        if ((inputStream = FileUtil.class.getResourceAsStream(filePath)) != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } else if ((resource = FileUtil.class.getResource(filePath)) != null) {
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

    public static <T> T deserialize(Path path, Class<T> clazz) throws IOException {
        return deserialize(path.toString(), clazz);
    }

    /**
     * Deserializes a file
     *
     * @param path the file to be read
     * @return the deserialized object
     */
    public static <T> T deserialize(String path, Class<T> clazz) throws IOException {
        InputStream inputStream;
        URL resource;
        if ((inputStream = FileUtil.class.getResourceAsStream(path)) != null) {
            return deserialize(inputStream, clazz);
        } else if ((resource = FileUtil.class.getResource(path)) != null) {
            return DSL_JSON.deserialize(clazz, resource.openStream());
        } else {
            return DSL_JSON.deserialize(clazz, new FileInputStream(path));
        }
    }

    public static <T> T deserialize(InputStream inputStream, Class<T> clazz) throws IOException {
        return DSL_JSON.deserialize(clazz, inputStream);
    }

    public static <T> void serialize(String filename, T obj) throws IOException {
        DSL_JSON.serialize(obj, new PrettifyOutputStream(new FileOutputStream(filename)));
    }
}
