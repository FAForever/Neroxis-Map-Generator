package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.MapGenerator;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String version = MapGenerator.class.getPackage().getImplementationVersion();
        version = version != null ? version : "snapshot";
        return new String[]{version};
    }
}
