package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public strictfp class MapExporter {

    public static void exportMap(Path folderPath, SCMap map, boolean exportPreview, boolean exportDecals) {
        try {
            Path mapPath = folderPath.resolve(map.getFolderName());
            Files.createDirectories(mapPath);

            if (exportDecals) {
                if (map.getCompressedNormal() != null) {
                    SCMapExporter.exportNormals(mapPath, map);
                }
                if (map.getCompressedShadows() != null) {
                    SCMapExporter.exportShadows(mapPath, map);
                }
            }
            if (exportPreview) {
                SCMapExporter.exportPreview(mapPath, map);
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
