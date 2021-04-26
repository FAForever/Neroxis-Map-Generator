package com.faforever.neroxis.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public strictfp class FileUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
        BufferedReader bufferedReader;
        InputStream inputStream;
        URL resource;
        if ((inputStream = FileUtils.class.getResourceAsStream(path)) != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } else if ((resource = FileUtils.class.getResource(path)) != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(resource.openStream()));
        } else {
            bufferedReader = new BufferedReader(new FileReader(Paths.get(path).toFile()));
        }
        return objectMapper.readValue(bufferedReader, clazz);
    }

    public static <T> void serialize(String filename, T obj) throws IOException {
        objectMapper.writeValue(Paths.get(filename).toFile(), obj);
    }
}
