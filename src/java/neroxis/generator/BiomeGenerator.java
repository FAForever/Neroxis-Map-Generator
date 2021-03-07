package neroxis.generator;

import neroxis.exporter.BiomeExporter;
import neroxis.importer.MapImporter;
import neroxis.map.SCMap;
import neroxis.util.ArgumentParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

public strictfp class BiomeGenerator {

    Path folderPath = Paths.get(".");
    Path mapPath;
    Path envPath;
    String biomeName;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);

        BiomeGenerator generator = new BiomeGenerator();

        generator.interpretArguments(ArgumentParser.parse(args));
        if (generator.biomeName == null) {
            return;
        }

        System.out.println("Generating biome " + generator.biomeName + " from " + generator.mapPath);
        SCMap map = MapImporter.importMap(generator.mapPath);
        System.out.println("Saving biome to " + generator.folderPath + File.separator + generator.biomeName);
        BiomeExporter.exportBiome(generator.envPath, generator.folderPath, generator.biomeName, map.getBiome());
        System.out.println("Done");
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("neroxis.map-gen usage:\n" +
                    "--help                 produce help message\n" +
                    "--biome-name arg       required, set the name for the generated biome\n" +
                    "--map-path arg         required, set the map path to generate biome from\n" +
                    "--env-path arg         required, set the env path to load textures from\n" +
                    "--folder-path arg      optional, set the target folder for the generated biome\n");
            return;
        }

        if (!arguments.containsKey("biome-name")) {
            System.out.println("Biome name not supplied");
            return;
        }

        if (!arguments.containsKey("map-path")) {
            System.out.println("Map path not supplied");
            return;
        }

        if (!arguments.containsKey("env-path")) {
            System.out.println("Env path not supplied");
            return;
        }

        biomeName = arguments.get("biome-name");
        mapPath = Paths.get(arguments.get("map-path"));
        envPath = Paths.get(arguments.get("env-path"));

        if (arguments.containsKey("folder-path")) {
            folderPath = Paths.get(arguments.get("folder-path"));
        }
    }
}
