package neroxis.biomes;

import com.google.gson.JsonParseException;
import lombok.Data;
import neroxis.map.PropMaterials;
import neroxis.map.TerrainMaterials;
import neroxis.util.FileUtils;
import neroxis.util.serialized.LightingSettings;
import neroxis.util.serialized.WaterSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Data
public strictfp class Biomes {

    // ├ Biome
    // ├-- materials.json <required>
    // ├-- props.json <required>
    // ├-- WaterSettings.scmwtr <optional>
    // └-- Light.scmlighting <optional>

    public static final List<String> BIOMES_LIST = Arrays.asList("Desert", "Frithen", "Loki", "Mars", "Moonlight", "Prayer", "Stones", "Syrtis", "Wonder");
    private static final String CUSTOM_BIOMES_DIR = "/custom_biome/";

    public static Biome loadResourceBiome(String resource) {
        TerrainMaterials terrainMaterials = null;
        String resourcePath = CUSTOM_BIOMES_DIR + resource;
        if (Biomes.class.getResource(resourcePath) == null) {
            resourcePath = resource;
        }
        resourcePath = Paths.get(resourcePath).toString();
        if (!resourcePath.endsWith(File.separator)) {
            resourcePath += File.separator;
        }
        try {
            terrainMaterials = FileUtils.deserialize(resourcePath + "materials.json", TerrainMaterials.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while loading biome %s\n", resourcePath);
            System.exit(1);
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while parsing the following biome:%s\n", resourcePath);
            System.exit(1);
        }

        PropMaterials propMaterials = null;

        try {
            propMaterials = FileUtils.deserialize(resourcePath + "props.json", PropMaterials.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while loading biome %s\n", resourcePath);
            System.exit(1);
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while parsing the following biome:%s\n", resourcePath);
            System.exit(1);
        }

        // Water parameters
        WaterSettings waterSettings = null;
        try {
            waterSettings = FileUtils.deserialize(resourcePath + "WaterSettings.scmwtr", WaterSettings.class);
        } catch (IOException e) {
            System.out.printf("Did not find water settings for biome: %s, falling back to default\n", resourcePath);
            waterSettings = new WaterSettings();
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while parsing the following biome: %s\n", resourcePath);
            System.exit(1);
        }

        // Lighting settings
        LightingSettings lightingSettings = null;
        try {
            lightingSettings = FileUtils.deserialize(resourcePath + "Light.scmlighting", LightingSettings.class);
        } catch (IOException e) {
            System.out.printf("Did not find light settings for biome: %s, falling back to default\n", resourcePath);
            lightingSettings = new LightingSettings();
        } catch (JsonParseException e) {
            e.printStackTrace();
            System.out.printf("An error occurred while parsing the following biome: %s\n", resourcePath);
            System.exit(1);
        }

        return new Biome(terrainMaterials.getName(), terrainMaterials, propMaterials, waterSettings, lightingSettings);
    }

    public static Biome getRandomBiome(Random random) {
        return loadResourceBiome(BIOMES_LIST.get(random.nextInt(BIOMES_LIST.size())));
    }
}
