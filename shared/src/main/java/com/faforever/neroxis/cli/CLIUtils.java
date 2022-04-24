package com.faforever.neroxis.cli;

import com.faforever.neroxis.util.MathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import picocli.CommandLine;

public class CLIUtils {

    public static float convertDensity(float percent, int numBins, CommandLine.Model.CommandSpec spec) {
        if (percent < 0 || percent > 1) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("`Must be between 0 and 1 but was `%f`", percent));
        }
        return MathUtil.discretePercentage(percent, numBins);
    }

    public static int convertMapSizeString(String string, MapSizeStrictness strictness,
                                           CommandLine.Model.CommandSpec spec) {
        int value;
        if (string.endsWith("km")) {
            String kmString = string.replace("km", "");
            float kmValue = Float.parseFloat(kmString);

            value = (int) (kmValue * 51.2);
        } else {
            value = Integer.parseInt(string);
        }

        if (MapSizeStrictness.DISCRETE_64.equals(strictness) && value % 64 != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Size must be a multiple of 64 but is %d", value));
        }

        if (MapSizeStrictness.POWER_OF_2.equals(strictness) && StrictMath.log(value) / StrictMath.log(2) % 1 != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Size must be a power of 2 but is %d", value));
        }

        return value;
    }

    public static void checkWritableDirectory(Path path, CommandLine.Model.CommandSpec spec) {
        File folder = path.toFile();

        if (folder.exists() && !folder.isDirectory()) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s is not a directory", folder.getPath()));
        }

        if (folder.exists() && !folder.canWrite()) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s cannot be written to", folder.getPath()));
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Could not create directory at %s", path), e);
        }
    }

    public static void checkValidMapFolder(Path path, CommandLine.Model.CommandSpec spec) {
        checkReadablePath(path, spec);

        File mapFolder = path.toFile();

        if (!mapFolder.isDirectory()) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("%s is not a directory", path));
        }

        File[] files = mapFolder.listFiles(CLIUtils::isRequiredMapFile);

        if (files == null) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("%s cannot be read", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith(".scmap"))) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s does not contain an scmap file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_scenario.lua"))) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s does not contain a scenario file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_save.lua"))) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s does not contain a save file", path));
        }

        if (Arrays.stream(files).noneMatch(file -> file.getName().endsWith("_script.lua"))) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("%s does not contain a script file", path));
        }

        for (File file : files) {
            if (!file.canRead()) {
                throw new CommandLine.ParameterException(spec.commandLine(),
                                                         String.format("%s cannot be read", file.getPath()));
            }
        }
    }

    public static void checkReadablePath(Path path, CommandLine.Model.CommandSpec spec) {
        if (!Files.exists(path)) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("%s does not exist", path));
        }

        if (!path.toFile().canRead()) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("%s cannot be read", path));
        }
    }

    private static boolean isRequiredMapFile(File file) {
        String filename = file.getName();
        return filename.endsWith(".scmap")
               || filename.endsWith("_scenario.lua")
               || filename.endsWith("_save.lua")
               || filename.endsWith("_script.lua");
    }

    public enum MapSizeStrictness {
        NONE, DISCRETE_64, POWER_OF_2
    }
}
