package com.faforever.neroxis.map.exporter;

import com.faforever.neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public strictfp class MapExporter {

    public static void exportMap(Path folderPath, SCMap map, boolean exportPreview, boolean exportNormal) {
        try {
            Path mapPath = folderPath.resolve(map.getFolderName());
            Files.createDirectories(mapPath);
            if (exportPreview) {
                SCMapExporter.exportPreview(mapPath, map);
            }
            if (exportNormal) {
                SCMapExporter.exportNormal(mapPath, map);
            }
            SCMapExporter.exportSCMAP(mapPath, map);
            SaveExporter.exportSave(mapPath, map);
            ScenarioExporter.exportScenario(mapPath, map);
            ScriptExporter.exportScript(mapPath, map);
        } catch (IOException e) {
            System.err.println("Error while saving the map.");
            e.printStackTrace();
        }
    }

}
