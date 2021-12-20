package com.faforever.neroxis.util;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.PrettifyOutputStream;
import com.dslplatform.json.runtime.Settings;
import lombok.SneakyThrows;

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

public strictfp class FileUtils {

    private static final DslJson<Object> dslJson = new DslJson<>(Settings.basicSetup());

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
    public static <T> T deserialize(String path, Class<T> clazz) throws IOException {
        InputStream inputStream;
        URL resource;
        if ((inputStream = FileUtils.class.getResourceAsStream(path)) != null) {
            return deserialize(inputStream, clazz);
        } else if ((resource = FileUtils.class.getResource(path)) != null) {
            return dslJson.deserialize(clazz, resource.openStream());
        } else {
            return dslJson.deserialize(clazz, new FileInputStream(path));
        }
    }

    public static <T> T deserialize(InputStream inputStream, Class<T> clazz) throws IOException {
        return dslJson.deserialize(clazz, inputStream);
    }

    public static <T> void serialize(String filename, T obj) throws IOException {
        dslJson.serialize(obj, new PrettifyOutputStream(new FileOutputStream(filename)));
    }
}
