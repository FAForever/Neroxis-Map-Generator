package com.faforever.neroxis.map.reshader;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.exporter.SCMapExporter;
import com.faforever.neroxis.map.importer.SCMapImporter;
import com.faforever.neroxis.util.ArgumentParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

public class MapReshader {

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.ROOT);

        Map<String, String> parsedArgs = ArgumentParser.parse(args);

        if (parsedArgs.containsKey("help")) {
            System.out.println("map-reshader usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-map-path arg      required, set the input folder for the map\n" +
                    "--out-map-path arg     required, set the output folder for the transformed map\n" +
                    "--shader arg           required, set the shader for the map\n");
            return;
        }

        Path inFolderPath = Paths.get(parsedArgs.get("in-map-path"));
        Path outFolderPath = Paths.get(parsedArgs.get("out-folder-path"));
        System.out.println("Reshading map " + inFolderPath);
        copyDirectory(inFolderPath, outFolderPath);
        SCMap map = SCMapImporter.importSCMAP(outFolderPath);
        map.setTerrainShaderPath(parsedArgs.get("shader"));
        SCMapExporter.exportSCMAP(outFolderPath, map);
        System.out.println("Saving map to " + outFolderPath.toAbsolutePath());
        System.out.println("Done");
    }

    public static void copyDirectory(Path sourceDirectory, Path destinationDirectory) throws IOException {
        Files.createDirectories(destinationDirectory);
        Files.walk(sourceDirectory)
                .forEach(source -> {
                    try {
                        Files.copy(source, destinationDirectory.resolve(source.getFileName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
