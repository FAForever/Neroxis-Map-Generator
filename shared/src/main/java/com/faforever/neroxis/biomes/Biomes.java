package com.faforever.neroxis.biomes;

import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.serial.biome.DecalMaterials;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;
import com.faforever.neroxis.util.serial.biome.WaterSettings;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Data
public class Biomes {
    // ├ Biome
    // ├-- materials.json <required>
    // ├-- props.json <required>
    // ├-- WaterSettings.scmwtr <required>
    // └-- Light.scmlighting <required>
    public static final List<String> BIOMES_LIST = List.of("Brimstone", "Desert", "EarlyAutumn", "Frithen",
                                                           "Mars", "Moonlight", "Prayer", "Stones", "Sunset",
                                                           "Syrtis", "WindingRiver", "Wonder");
    private static final String CUSTOM_BIOMES_DIR = "/custom_biome/";

    public static Biome loadBiome(String folderPath) {
        if (Biomes.class.getResource(CUSTOM_BIOMES_DIR + folderPath) != null) {
            folderPath = CUSTOM_BIOMES_DIR + folderPath;
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }
        } else {
            folderPath = Path.of(folderPath).toString();
            if (!folderPath.endsWith(File.separator)) {
                folderPath += File.separator;
            }
        }

        TerrainMaterials terrainMaterials;
        try {
            terrainMaterials = FileUtil.deserialize(folderPath + "materials.json", TerrainMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %smaterials.json\n", folderPath),
                                       e);
        }

        PropMaterials propMaterials;
        try {
            propMaterials = FileUtil.deserialize(folderPath + "props.json", PropMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %sprops.json\n", folderPath), e);
        }

        DecalMaterials decalMaterials;
        try {
            decalMaterials = FileUtil.deserialize(folderPath + "decals.json", DecalMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %sdecals.json\n", folderPath), e);
        }

        // Water parameters
        WaterSettings waterSettings;
        try {
            waterSettings = FileUtil.deserialize(folderPath + "WaterSettings.scmwtr", WaterSettings.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %s WaterSettings\n", folderPath),
                                       e);
        }

        // Lighting settings
        LightingSettings lightingSettings;
        try {
            lightingSettings = FileUtil.deserialize(folderPath + "Light.scmlighting", LightingSettings.class);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("An error occurred while loading %s LightingSettings\n", folderPath), e);
        }

        return new Biome(terrainMaterials.getName(), terrainMaterials, propMaterials, decalMaterials, waterSettings,
                         lightingSettings);
    }
}
