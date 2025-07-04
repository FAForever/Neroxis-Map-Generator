package com.faforever.neroxis.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WritableDirectoryConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String value) {
        Path path = Path.of(value);

        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new CommandLine.TypeConversionException(String.format("%s is not a directory", path));
            }
            if (!Files.isWritable(path)) {
                throw new CommandLine.TypeConversionException(String.format("%s cannot be written to", path));
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new CommandLine.TypeConversionException(
                        String.format("Could not create directory at %s: %s", path, e.getMessage()));
            }
        }

        return path;
    }
}
