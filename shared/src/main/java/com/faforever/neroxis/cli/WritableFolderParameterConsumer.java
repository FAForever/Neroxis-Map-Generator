package com.faforever.neroxis.cli;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Stack;

public class WritableFolderParameterConsumer implements CommandLine.IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        Path path = Path.of(args.pop());
        File folder = path.toFile();

        if (!folder.isDirectory()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s is not a directory", folder.getPath())
            );
        }

        if (!folder.canWrite()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("%s cannot be written to", folder.getPath())
            );
        }

        argSpec.setValue(path);
    }
}
