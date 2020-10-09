package biomes;

import com.google.gson.JsonParseException;
import lombok.Data;
import map.PropMaterials;
import map.TerrainMaterials;
import util.FileUtils;
import util.PlatformUtils;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Data
public strictfp class Biomes {

    private static final String CUSTOM_BIOMES_DIR = "custom_biome";

    public static List<Biome> list = new ArrayList<>();

    static {
        // ├ Biome
        // ├-- materials.json <required>
        // └-- WaterSettings.scmwtr <optional>
        // └-- Light.scmlighting <optional>

        // Materials
        try {
            if (PlatformUtils.isRunningFromJAR()) {
                List<String> files = FileUtils.listFilesInZipDirectory(CUSTOM_BIOMES_DIR, PlatformUtils.getRunnableJarFile());

                files.stream().map(Biomes::loadBiomeSerial).forEachOrdered(list::add);
            } else {
                Path biomePath = Paths.get(Objects.requireNonNull(Biome.class.getClassLoader().getResource(CUSTOM_BIOMES_DIR)).toURI());

                Files.list(biomePath).map(Biomes::loadBiomeSerial).forEachOrdered(list::add);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            System.err.println("An error occured while trying to list the biomes directory path.");
            System.exit(1);
        }
    }

    public static Biome loadBiomeSerial(Object path) {
        TerrainMaterials terrainMaterials = null;

        try {
            terrainMaterials = FileUtils.deserialize(path, "materials.json", TerrainMaterials.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("An error occured while loading biome %s\n", path);
            System.exit(1);
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occured while parsing the following biome:%s\n", path);
            System.exit(1);
        }

        PropMaterials propMaterials = null;

        try {
            propMaterials = FileUtils.deserialize(path, "props.json", PropMaterials.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("An error occured while loading biome %s\n", path);
            System.exit(1);
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occured while parsing the following biome:%s\n", path);
            System.exit(1);
        }

        // Water parameters
        WaterSettings waterSettings = null;
        try {
            waterSettings = FileUtils.deserialize(path, "WaterSettings.scmwtr", WaterSettings.class);
        } catch (IOException e) {
            System.out.printf("Did not find water settings for biome: %s, falling back to default\n", path);
            waterSettings = new WaterSettings();
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occured while parsing the following biome: %s\n", path);
            System.exit(1);
        }

        // Lighting settings
        LightingSettings lightingSettings = null;
        try {
            lightingSettings = FileUtils.deserialize(path, "Light.scmlighting", LightingSettings.class);
        } catch (IOException e) {
            System.out.printf("Did not find light settings for biome: %s, falling back to default\n", path);
            lightingSettings = new LightingSettings();
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occured while parsing the following biome: %s\n", path);
            System.exit(1);
        }

        return new Biome(terrainMaterials.getName(), terrainMaterials, propMaterials, waterSettings, lightingSettings);
    }


    public static Biome getRandomBiome(Random random) {
        return list.get(random.nextInt(list.size()));
    }

    public static Biome getBiomeByName(String name) {
        return list.stream()
                .filter(b -> Objects.equals(b.name, name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find a biome for name: " + name));
    }
}
