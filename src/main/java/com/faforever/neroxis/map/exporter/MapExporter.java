package com.faforever.neroxis.map.exporter;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public strictfp class MapExporter {

    public static void exportMap(Path folderPath, SCMap map, boolean exportPreview, boolean exportDecals) {
        try {
            Path mapPath = folderPath.resolve(map.getFolderName());
            Files.createDirectories(mapPath);
            int mapSize = map.getSize();
            int compatibleMapSize = (int) StrictMath.pow(2, StrictMath.ceil(StrictMath.log(mapSize) / StrictMath.log(2)));
            Vector2 boundOffset = new Vector2(compatibleMapSize / 2f, compatibleMapSize / 2f);

            map.changeMapSize(mapSize, compatibleMapSize, boundOffset);

            if (exportDecals) {
                if (map.getCompressedNormal() != null) {
                    SCMapExporter.exportNormals(mapPath, map, boundOffset, mapSize);
                }
                if (map.getCompressedShadows() != null) {
                    SCMapExporter.exportShadows(mapPath, map, boundOffset, mapSize);
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
