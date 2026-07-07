package com.faforever.neroxis.util;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;

public class FileUtil {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();


    public static void deleteRecursiveIfExists(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads an entire file
     *
     * @param filePath the filePath to the directory where the file is located as a String
     * @return the content of the file
     */
    public static String readFile(String filePath) throws IOException {
        try (InputStream inputStream = ResourceUtil.getResourceAsStream(filePath)) {
            if (inputStream != null) {
                return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            }
        }

        return new BufferedReader(new FileReader(Paths.get(filePath).toFile())).lines()
                                                                               .collect(Collectors.joining("\n"));
    }

    /**
     * Deserializes a file
     *
     * @param path the file to be read
     * @return the deserialized object
     */
    public static <T> T deserialize(String path, Class<T> clazz) throws IOException {
        try (InputStream inputStream = ResourceUtil.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + path);
            }
            return deserialize(inputStream, clazz);
        }
    }

    public static <T> T deserialize(InputStream inputStream, Class<T> clazz) {
        return JSON_MAPPER.readValue(inputStream, clazz);
    }

    public static <T> void serialize(String filename, T obj) throws IOException {
        serialize(new FileOutputStream(filename), obj);
    }

    public static <T> void serialize(File file, T obj) throws IOException {
        serialize(new FileOutputStream(file), obj);
    }

    public static <T> void serialize(OutputStream outputStream, T obj) {
        JSON_MAPPER.writeValue(outputStream, obj);
    }
}
