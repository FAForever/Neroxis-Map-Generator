package neroxis.exporter;

import neroxis.map.SCMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public strictfp class MapExporter {

    public static void exportMap(Path folderPath, SCMap map, boolean exportPreview) throws IOException {
        Path mapPath = folderPath.resolve(map.getFolderName());
        Files.createDirectories(mapPath);
        SCMapExporter.exportSCMAP(mapPath, map);
        if (exportPreview) {
            SCMapExporter.exportPreview(mapPath, map);
        }
        SaveExporter.exportSave(mapPath, map);
        ScenarioExporter.exportScenario(mapPath, map);
        ScriptExporter.exportScript(mapPath, map);
    }

}
