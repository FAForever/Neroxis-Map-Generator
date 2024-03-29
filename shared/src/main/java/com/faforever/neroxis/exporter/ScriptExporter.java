package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.SCMap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class ScriptExporter {
    public static void exportScript(Path folderPath, SCMap map) throws IOException {
        File file = folderPath.resolve(map.getFilePrefix() + "_script.lua").toFile();
        boolean status = file.createNewFile();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        out.writeBytes(map.getScript());

        out.flush();
        out.close();
    }
}
