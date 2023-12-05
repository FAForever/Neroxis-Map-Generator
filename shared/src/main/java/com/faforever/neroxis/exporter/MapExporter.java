package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.faforever.neroxis.map.SCMap.LEGACY_SHADER_NAME;
import static com.faforever.neroxis.map.SCMap.PBR_SHADER_NAME;

public class MapExporter {
    public static void exportMap(Path folderPath, SCMap map, boolean exportPreview) {
        try {
            Path mapPath = folderPath.resolve(map.getFolderName());
            Files.createDirectories(mapPath);

            if (map.getTerrainShaderPath().equals(LEGACY_SHADER_NAME)) {
                if (map.getCompressedNormal() != null) {
                    SCMapExporter.exportNormals(mapPath, map);
                }
                if (map.getCompressedShadows() != null) {
                    SCMapExporter.exportShadows(mapPath, map);
                }
            } else if (map.getTerrainShaderPath().equals(PBR_SHADER_NAME)){
                SCMapExporter.exportMapwideTexture(folderPath.resolve(map.getFolderName()), map);
                SCMapExporter.exportPBR(folderPath.resolve(map.getFolderName()), map);
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
