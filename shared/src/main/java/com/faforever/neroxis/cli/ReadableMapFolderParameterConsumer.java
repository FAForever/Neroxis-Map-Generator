package com.faforever.neroxis.cli;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Stack;

public class ReadableMapFolderParameterConsumer implements CommandLine.IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        Path mapFolderPath = Path.of(args.pop());
        File mapFolder = mapFolderPath.toFile();

        if (!mapFolder.exists()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s does not exist", mapFolder.getPath())
            );
        }

        if (!mapFolder.isDirectory()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s is not a directory", mapFolder.getPath())
            );
        }

        if (!mapFolder.canRead()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s cannot be read", mapFolder.getPath())
            );
        }

        File[] files = mapFolder.listFiles(this::isRequiredMapFile);

        if (files == null) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s cannot be read", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith(".scmap"))) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s does not contain an scmap file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_scenario.lua"))) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s does not contain a scenario file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_save.lua"))) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s does not contain a save file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_script.lua"))) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s does not contain a script file", mapFolder.getPath())
            );
        }

        for (File file : files) {
            if (!file.canRead()) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        String.format("%s cannot be read", file.getPath())
                );
            }
        }

        argSpec.setValue(mapFolderPath);
    }

    private boolean isRequiredMapFile(File file) {
        String filename = file.getName();
        return filename.endsWith(".scmap")
                || filename.endsWith("_scenario.lua")
                || filename.endsWith("_save.lua")
                || filename.endsWith("_script.lua");
    }
}
