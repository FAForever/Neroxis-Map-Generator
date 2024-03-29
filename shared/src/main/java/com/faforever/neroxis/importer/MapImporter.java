package com.faforever.neroxis.importer;

import com.faforever.neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Path;

public class MapImporter {
    public static SCMap importMap(Path folderPath) throws IOException {
        SCMap map = SCMapImporter.importSCMAP(folderPath);
        map.setFolderName(folderPath.getName(folderPath.getNameCount() - 1).toString());
        SaveImporter.importSave(folderPath, map);
        ScenarioImporter.importScenario(folderPath, map);
        ScriptImporter.importScript(folderPath, map);
        return map;
    }
}
