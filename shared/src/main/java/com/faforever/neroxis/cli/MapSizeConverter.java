package com.faforever.neroxis.cli;

import picocli.CommandLine;

public class MapSizeConverter implements CommandLine.ITypeConverter<Integer> {

    @Override
    public Integer convert(String rawValue) {
        if (rawValue.endsWith("km")) {
            String kmString = rawValue.replace("km", "");
            float kmValue = Float.parseFloat(kmString);

            return (int) (kmValue * 51.2);
        } else {
            return Integer.parseInt(rawValue);
        }
    }
}
