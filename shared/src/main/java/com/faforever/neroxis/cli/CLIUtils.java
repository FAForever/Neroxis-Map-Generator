package com.faforever.neroxis.cli;

import com.faforever.neroxis.util.MathUtil;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class CLIUtils {

    public static float convertDensity(float percent, int numBins, CommandLine.Model.CommandSpec spec) {
        if (percent < 0 || percent > 1) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("`Must be between 0 and 1 but was `%f`", percent)
            );
        }
        return MathUtil.discretePercentage(percent, numBins);
    }

    public static int convertGeneratorMapSizeString(String string, CommandLine.Model.CommandSpec spec) {
        int value;
        if (string.endsWith("km")) {
            String kmString = string.replace("km", "");
            float kmValue = Float.parseFloat(kmString);

            if (kmValue % 1.25f != 0) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(),
                        "Size must be a multiple of 1.25km"
                );
            }

            value = (int) (kmValue * 51.2);
        } else {
            value = Integer.parseInt(string);
        }

        if (value % 64 != 0) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "Size ust be a multiple of 64"
            );
        }

        return value;
    }

    public static int convertMapSizeString(String string, CommandLine.Model.CommandSpec spec) {
        if (string.endsWith("km")) {
            String kmString = string.replace("km", "");
            float kmValue = Float.parseFloat(kmString);
            return (int) (kmValue * 51.2);
        }

        return Integer.parseInt(string);
    }

    public static int convertMapSizeStringStrict(String string, CommandLine.Model.CommandSpec spec) {
        int value;
        if (string.endsWith("km")) {
            String kmString = string.replace("km", "");
            float kmValue = Float.parseFloat(kmString);
            value = (int) (kmValue * 51.2);
        } else {
            value = Integer.parseInt(string);
        }

        if (StrictMath.log(value) / StrictMath.log(2) % 1 != 0) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "Size must be a power of 2"
            );
        }

        return value;
    }

    public static void checkWritableDirectory(Path path, CommandLine.Model.CommandSpec spec) throws IOException {
        File folder = path.toFile();

        if (!folder.isDirectory()) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s is not a directory", folder.getPath())
            );
        }

        if (!folder.canWrite()) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s cannot be written to", folder.getPath())
            );
        }

        Files.createDirectories(path);
    }

    public static void checkValidMapFolder(Path path, CommandLine.Model.CommandSpec spec) {
        File mapFolder = path.toFile();

        if (!mapFolder.exists()) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s does not exist", mapFolder.getPath())
            );
        }

        if (!mapFolder.isDirectory()) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s is not a directory", mapFolder.getPath())
            );
        }

        if (!mapFolder.canRead()) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s cannot be read", mapFolder.getPath())
            );
        }

        File[] files = mapFolder.listFiles(CLIUtils::isRequiredMapFile);

        if (files == null) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s cannot be read", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith(".scmap"))) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s does not contain an scmap file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_scenario.lua"))) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s does not contain a scenario file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_save.lua"))) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s does not contain a save file", mapFolder.getPath())
            );
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_script.lua"))) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("%s does not contain a script file", mapFolder.getPath())
            );
        }

        for (File file : files) {
            if (!file.canRead()) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(),
                        String.format("%s cannot be read", file.getPath())
                );
            }
        }
    }

    private static boolean isRequiredMapFile(File file) {
        String filename = file.getName();
        return filename.endsWith(".scmap")
                || filename.endsWith("_scenario.lua")
                || filename.endsWith("_save.lua")
                || filename.endsWith("_script.lua");
    }

}
