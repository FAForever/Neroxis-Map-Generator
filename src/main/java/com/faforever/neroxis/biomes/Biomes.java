package com.faforever.neroxis.biomes;

import com.faforever.neroxis.map.DecalMaterials;
import com.faforever.neroxis.map.PropMaterials;
import com.faforever.neroxis.map.TerrainMaterials;
import com.faforever.neroxis.util.FileUtils;
import com.faforever.neroxis.util.serialized.LightingSettings;
import com.faforever.neroxis.util.serialized.WaterSettings;
import com.google.gson.JsonParseException;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Data
public strictfp class Biomes {

    // ├ Biome
    // ├-- materials.json <required>
    // ├-- props.json <required>
    // ├-- WaterSettings.scmwtr <required>
    // └-- Light.scmlighting <required>

    public static final List<String> BIOMES_LIST = Arrays.asList("Desert", "Frithen", "Loki", "Mars", "Moonlight", "Prayer", "Stones", "Syrtis", "Wonder");
    private static final String CUSTOM_BIOMES_DIR = "/custom_biome/";

    public static Biome loadBiome(String folderPath) throws Exception {
        if (Biomes.class.getResourceAsStream(CUSTOM_BIOMES_DIR + folderPath) != null) {
            folderPath = CUSTOM_BIOMES_DIR + folderPath;
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }
        } else {
            folderPath = Paths.get(folderPath).toString();
            if (!folderPath.endsWith(File.separator)) {
                folderPath += File.separator;
            }
        }

        TerrainMaterials terrainMaterials;
        try {
            terrainMaterials = FileUtils.deserialize(folderPath + "materials.json", TerrainMaterials.class);
        } catch (IOException e) {
            throw new Exception(String.format("An error occurred while loading %smaterials.json\n", folderPath), e);
        } catch (JsonParseException e) {
            throw new Exception(String.format("An error occurred while parsing materials.json from the following biome:%s\n", folderPath), e);
        }

        PropMaterials propMaterials;
        try {
            propMaterials = FileUtils.deserialize(folderPath + "props.json", PropMaterials.class);
        } catch (IOException e) {
            throw new Exception(String.format("An error occurred while loading %sprops.json\n", folderPath), e);
        } catch (JsonParseException e) {
            throw new Exception(String.format("An error occurred while parsing props.json from the following biome:%s\n", folderPath), e);
        }

        DecalMaterials decalMaterials;
        try {
            decalMaterials = FileUtils.deserialize(folderPath + "decals.json", DecalMaterials.class);
        } catch (IOException e) {
            throw new Exception(String.format("An error occurred while loading %sdecals.json\n", folderPath), e);
        } catch (JsonParseException e) {
            throw new Exception(String.format("An error occurred while parsing decals.json from the following biome:%s\n", folderPath), e);
        }

        // Water parameters
        WaterSettings waterSettings;
        try {
            waterSettings = FileUtils.deserialize(folderPath + "WaterSettings.scmwtr", WaterSettings.class);
        } catch (IOException e) {
            throw new Exception(String.format("An error occurred while loading %s WaterSettings\n", folderPath), e);
        } catch (JsonParseException e) {
            throw new Exception(String.format("An error occurred while parsing WaterSettings from the following biome:%s\n", folderPath), e);
        }

        // Lighting settings
        LightingSettings lightingSettings;
        try {
            lightingSettings = FileUtils.deserialize(folderPath + "Light.scmlighting", LightingSettings.class);
        } catch (IOException e) {
            throw new Exception(String.format("An error occurred while loading %s LightingSettings\n", folderPath), e);
        } catch (JsonParseException e) {
            throw new Exception(String.format("An error occurred while parsing LightingSettings from the following biome:%s\n", folderPath), e);
        }

        return new Biome(terrainMaterials.getName(), terrainMaterials, propMaterials, decalMaterials, waterSettings, lightingSettings);
    }
}
