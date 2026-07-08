package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.GeneratedMapNameEncoder;
import com.faforever.neroxis.util.MathUtil;
import picocli.CommandLine;

public class BinnedDensityConverter implements CommandLine.ITypeConverter<Float> {

    @Override
    public Float convert(String value) {
        float percent = Float.parseFloat(value);
        if (percent < 0 || percent > 1) {
            throw new CommandLine.TypeConversionException(
                    String.format("Must be between 0 and 1 but was `%f`", percent));
        }
        return MathUtil.discretePercentage(percent, GeneratedMapNameEncoder.NUM_BINS);
    }
}
