package com.faforever.neroxis.importer;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ScriptImporter {
    public static void importScript(Path folderPath, SCMap map) throws IOException {
        File dir = folderPath.toFile();

        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith("_script.lua"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No script file in map folder");
            return;
        }

        Path scriptPath = mapFiles[0].toPath();
        map.setScript(FileUtil.readFile(scriptPath.toString()));
    }
}
