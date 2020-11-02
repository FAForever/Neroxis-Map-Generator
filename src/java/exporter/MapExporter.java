package exporter;

import map.SCMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapExporter {

    public static void exportMap(Path folderPath, String mapName, SCMap map, boolean exportPreview) throws IOException {
        Files.createDirectories(folderPath);
        SCMapExporter.exportSCMAP(folderPath, mapName, map);
        if (!exportPreview) {
            SCMapExporter.exportPreview(folderPath, mapName, map);
        }
        SaveExporter.exportSave(folderPath, mapName, map);
        ScenarioExporter.exportScenario(folderPath, mapName, map);
        ScriptExporter.exportScript(folderPath, mapName, map);
    }

}
