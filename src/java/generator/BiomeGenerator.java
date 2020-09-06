package generator;

import export.BiomeExporter;
import importer.SCMapImporter;
import map.SCMap;
import util.ArgumentParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.MissingFormatArgumentException;

public class BiomeGenerator {

    String folderPath = ".";
    String biomeName;
    String mapPath;
    String envPath;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);

        BiomeGenerator generator = new BiomeGenerator();

        generator.interpretArguments(ArgumentParser.parse(args));

        System.out.println("Generating biome " + generator.biomeName + " from " + Paths.get(generator.mapPath).toAbsolutePath());
        File dir = new File(generator.mapPath);
        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No scmap file in map folder");
        }
        SCMap map = SCMapImporter.loadSCMAP(mapFiles[0].toPath());
        System.out.println("Saving biome to " + Paths.get(generator.folderPath).toAbsolutePath() + File.separator + generator.biomeName);
        BiomeExporter.exportBiome(Paths.get(generator.envPath), Paths.get(generator.folderPath), generator.biomeName, map.getBiome());
        System.out.println("Done");
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-gen usage:\n" +
                    "--help                 produce help message\n" +
                    "--biome-name arg       required, set the name for the generated biome\n" +
                    "--map-path arg         required, set the map path to generate biome from\n" +
                    "--env-path arg         required, set the env path to load textures from\n" +
                    "--folder-path arg      optional, set the target folder for the generated biome\n");
            System.exit(0);
        }

        if (!arguments.containsKey("biome-name")) {
            System.out.println("Biome name not supplied");
            throw new MissingFormatArgumentException("Biome Name Missing");
        }
        biomeName = arguments.get("biome-name");

        if (!arguments.containsKey("map-path")) {
            System.out.println("Map path not supplied");
            throw new MissingFormatArgumentException("Map Path Missing");
        }
        mapPath = arguments.get("map-path");

        if (!arguments.containsKey("env-path")) {
            System.out.println("Env path not supplied");
            throw new MissingFormatArgumentException("Env Path Missing");
        }
        envPath = arguments.get("env-path");

        if (arguments.containsKey("folder-path")) {
            folderPath = arguments.get("folder-path");
        }
    }
}
