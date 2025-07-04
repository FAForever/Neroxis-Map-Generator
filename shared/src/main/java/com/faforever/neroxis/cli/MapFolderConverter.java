package com.faforever.neroxis.cli;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MapFolderConverter implements CommandLine.ITypeConverter<Path> {

    public Path convert(String value) {
        Path path = Path.of(value);
        checkReadablePath(path);

        File mapFolder = path.toFile();

        if (!Files.isDirectory(path)) {
            throw new CommandLine.TypeConversionException(String.format("%s is not a directory", path));
        }

        File[] files = mapFolder.listFiles(this::isRequiredMapFile);

        if (files == null) {
            throw new CommandLine.TypeConversionException(String.format("%s cannot be read", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith(".scmap"))) {
            throw new CommandLine.TypeConversionException(String.format("%s does not contain an scmap file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_scenario.lua"))) {
            throw new CommandLine.TypeConversionException(String.format("%s does not contain a scenario file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_save.lua"))) {
            throw new CommandLine.TypeConversionException(String.format("%s does not contain a save file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_script.lua"))) {
            throw new CommandLine.TypeConversionException(String.format("%s does not contain a script file", path));
        }

        for (File file : files) {
            if (!file.canRead()) {
                throw new CommandLine.TypeConversionException(String.format("%s cannot be read", file.getPath()));
            }
        }

        return path;
    }

    public void checkReadablePath(Path path) {
        if (!Files.exists(path)) {
            throw new CommandLine.TypeConversionException(String.format("%s does not exist", path));
        }

        if (!Files.isReadable(path)) {
            throw new CommandLine.TypeConversionException(String.format("%s cannot be read", path));
        }
    }

    private boolean isRequiredMapFile(File file) {
        String filename = file.getName();
        return filename.endsWith(".scmap")
               || filename.endsWith("_scenario.lua")
               || filename.endsWith("_save.lua")
               || filename.endsWith("_script.lua");
    }
}
