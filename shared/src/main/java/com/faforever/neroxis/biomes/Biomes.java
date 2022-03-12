package com.faforever.neroxis.biomes;

import com.faforever.neroxis.map.DecalMaterials;
import com.faforever.neroxis.map.PropMaterials;
import com.faforever.neroxis.map.TerrainMaterials;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.serial.LightingSettings;
import com.faforever.neroxis.util.serial.WaterSettings;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Data
public strictfp class Biomes {

    // ├ Biome
    // ├-- materials.json <required>
    // ├-- props.json <required>
    // ├-- WaterSettings.scmwtr <required>
    // └-- Light.scmlighting <required>

    public static final List<String> BIOMES_LIST = Arrays.asList("Brimstone", "Desert", "EarlyAutumn", "Frithen", "Loki",
            "Mars", "Moonlight", "Prayer", "Stones", "Syrtis", "WindingRiver", "Wonder");
    private static final Path CUSTOM_BIOMES_DIR = Path.of("/custom_biome/");

    public static Biome loadBiome(String biomeNameOrFolderPath) {
        Path folderPath;

        if (Biomes.class.getResourceAsStream(CUSTOM_BIOMES_DIR.resolve(biomeNameOrFolderPath).toString()) != null) {
            folderPath = CUSTOM_BIOMES_DIR.resolve(biomeNameOrFolderPath);
        } else {
            folderPath = Path.of(biomeNameOrFolderPath);
        }

        TerrainMaterials terrainMaterials;
        try {
            terrainMaterials = FileUtil.deserialize(folderPath.resolve("materials.json"), TerrainMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %smaterials.json\n", folderPath), e);
        }

        PropMaterials propMaterials;
        try {
            propMaterials = FileUtil.deserialize(folderPath.resolve("props.json"), PropMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %sprops.json\n", folderPath), e);
        }

        DecalMaterials decalMaterials;
        try {
            decalMaterials = FileUtil.deserialize(folderPath.resolve("decals.json"), DecalMaterials.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %sdecals.json\n", folderPath), e);
        }

        // Water parameters
        WaterSettings waterSettings;
        try {
            waterSettings = FileUtil.deserialize(folderPath.resolve("WaterSettings.scmwtr"), WaterSettings.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %s WaterSettings\n", folderPath), e);
        }

        // Lighting settings
        LightingSettings lightingSettings;
        try {
            lightingSettings = FileUtil.deserialize(folderPath.resolve("Light.scmlighting"), LightingSettings.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %s LightingSettings\n", folderPath), e);
        }

        return new Biome(terrainMaterials.getName(), terrainMaterials, propMaterials, decalMaterials, waterSettings, lightingSettings);
    }
}
