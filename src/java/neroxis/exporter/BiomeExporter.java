package neroxis.exporter;

import neroxis.biomes.Biome;
import neroxis.map.TerrainMaterials;
import neroxis.util.DDSReader;
import neroxis.util.FileUtils;
import neroxis.util.serialized.LightingSettings;
import neroxis.util.serialized.WaterSettings;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public strictfp class BiomeExporter {

    public static String filename;
    private static DataOutputStream out;

    public static void exportBiome(Path envDir, Path folderPath, String biomeName, Biome biome) throws IOException {
        Files.createDirectories(folderPath.resolve(biomeName));

        filename = folderPath.resolve(biomeName).resolve("Light.scmlighting").toString();
        FileUtils.serialize(filename, biome.getLightingSettings(), LightingSettings.class);

        filename = folderPath.resolve(biomeName).resolve("WaterSettings.scmwtr").toString();
        FileUtils.serialize(filename, biome.getWaterSettings(), WaterSettings.class);

        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            if (biome.getTerrainMaterials().getPreviewColors()[i] == null && !biome.getTerrainMaterials().getTexturePaths()[i].isEmpty()) {
                biome.getTerrainMaterials().getPreviewColors()[i] = getTexturePreviewColor(envDir, biome.getTerrainMaterials().getTexturePaths()[i]);
            }
        }
        biome.getTerrainMaterials().setName(biomeName);

        filename = folderPath.resolve(biomeName).resolve("materials.json").toString();
        FileUtils.serialize(filename, biome.getTerrainMaterials(), TerrainMaterials.class);
    }

    public static Color getTexturePreviewColor(Path envDir, String texturePath) throws IOException {
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
            return new Color((int) avgR / pixels.length, (int) avgG / pixels.length, (int) avgB / pixels.length);
        }
        return null;
    }
}
