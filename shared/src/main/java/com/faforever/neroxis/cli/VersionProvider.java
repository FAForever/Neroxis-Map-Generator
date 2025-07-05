package com.faforever.neroxis.cli;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String appVersion = System.getProperty("jpackage.app-version");
        if (appVersion != null) {
            return new String[]{appVersion};
        }
        
        String version = VersionProvider.class.getPackage().getImplementationVersion();
        version = version != null ? version : "snapshot";
        return new String[]{version};
    }
}
