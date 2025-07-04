package com.faforever.neroxis.cli;

import picocli.CommandLine;

public class PowerOfTwoMapSizeConverter extends MapSizeConverter {

    @Override
    public Integer convert(String rawValue) {
        int value = super.convert(rawValue);

        if (StrictMath.log(value) / StrictMath.log(2) % 1 != 0) {
            throw new CommandLine.TypeConversionException(String.format("Size must be a power of 2 but is %d", value));
        }

        return value;
    }
}
