package com.faforever.neroxis.cli;

import picocli.CommandLine;

public class MultipleMapSizeConverter extends MapSizeConverter {

    @Override
    public Integer convert(String rawValue) {
        int value = super.convert(rawValue);

        if (value % 64 != 0) {
            throw new CommandLine.TypeConversionException(
                    String.format("Size must be a multiple of 64 but is %d", value));
        }

        return value;
    }
}
