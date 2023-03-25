package com.faforever.neroxis.exporter;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.dds.DDSReader;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;

import java.awt.Color;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BiomeExporter {
    public static String filename;
    private static DataOutputStream out;

    public static void exportBiome(Path envDir, Path folderPath, String biomeName, Biome biome) throws IOException {
        Files.createDirectories(folderPath.resolve(biomeName));

        filename = folderPath.resolve(biomeName).resolve("Light.scmlighting").toString();
        FileUtil.serialize(filename, biome.lightingSettings());

        filename = folderPath.resolve(biomeName).resolve("WaterSettings.scmwtr").toString();
        FileUtil.serialize(filename, biome.waterSettings());

        Integer[] previewColors = biome.terrainMaterials().getPreviewColors();
        String[] texturePaths = biome.terrainMaterials().getTexturePaths();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            if (previewColors[i] == null && !texturePaths[i].isEmpty()) {
                previewColors[i] = getTexturePreviewColor(envDir, texturePaths[i]);
            }
        }

        biome.terrainMaterials().setName(biomeName);

        filename = folderPath.resolve(biomeName).resolve("materials.json").toString();
        FileUtil.serialize(filename, biome.terrainMaterials());
    }

    public static Integer getTexturePreviewColor(Path envDir, String texturePath) throws IOException {
        File file = Paths.get(envDir.toString(), texturePath).toFile();
        InputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        int[] pixels = DDSReader.read(buffer, DDSReader.ARGB, 0);
        float avgR = 0;
        float avgB = 0;
        float avgG = 0;
        if (pixels != null) {
            for (int pix : pixels) {
                Color pixColor = new Color(pix);
                avgR += pixColor.getRed();
                avgB += pixColor.getBlue();
                avgG += pixColor.getGreen();
            }
            return new Color((int) avgR / pixels.length, (int) avgG / pixels.length,
                             (int) avgB / pixels.length).getRGB();
        }
        return null;
    }
}
