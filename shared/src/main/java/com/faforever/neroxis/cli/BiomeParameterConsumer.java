package com.faforever.neroxis.cli;

import com.faforever.neroxis.biomes.Biomes;
import picocli.CommandLine;

import java.util.Stack;

public class BiomeParameterConsumer implements CommandLine.IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        String stringArg = args.pop();
        if (stringArg.endsWith("km")) {
            String kmString = stringArg.replace("km", "");
            float kmValue = Float.parseFloat(kmString);

            if (kmValue % 1.25f != 0) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        "Must be a multiple of 1.25km"
                );
            }

            argSpec.setValue(kmValue * 51.2);
            return;
        }

        int value = Integer.parseInt(stringArg);
        if (value % 64 != 0) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Must be a multiple of 64"
            );
        }

        argSpec.setValue(Biomes.loadBiome(args.pop()));
    }
}
