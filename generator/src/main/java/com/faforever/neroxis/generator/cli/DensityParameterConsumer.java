package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.util.MathUtil;
import picocli.CommandLine;

import java.util.Stack;

import static com.faforever.neroxis.generator.MapGenerator.NUM_BINS;

public class DensityParameterConsumer implements CommandLine.IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        float percent = Float.parseFloat(args.pop());
        if (percent < 0 || percent > 1) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("`Must be between 0 and 1 but was `%f`", percent)
            );
        }

        float value = MathUtil.discretePercentage(percent, NUM_BINS);
        argSpec.setValue(value);
    }
}
