package com.faforever.neroxis.map.importer;

import com.faforever.neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Path;

public strictfp class MapImporter {

    public static SCMap importMap(Path folderPath) throws IOException {
        SCMap map = SCMapImporter.importSCMAP(folderPath);
        if (map != null) {
            map.setFolderName(folderPath.getName(folderPath.getNameCount() - 1).toString());
            SaveImporter.importSave(folderPath, map);
            ScenarioImporter.importScenario(folderPath, map);
            ScriptImporter.importScript(folderPath, map);
        }
        return map;
    }

}
