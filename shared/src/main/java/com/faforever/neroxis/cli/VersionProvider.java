package com.faforever.neroxis.cli;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = VersionProvider.class.getPackage().getImplementationVersion();
        version = version != null ? version : "snapshot";
        return new String[]{version};
    }
}
