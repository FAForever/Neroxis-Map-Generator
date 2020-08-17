package export;

import biomes.Biome;
import map.TerrainMaterials;
import util.DDSReader;
import util.FileUtils;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
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
        int width = (int) StrictMath.sqrt(pixels.length);
        int height = width;
        BufferedImage unscaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        unscaled.setRGB(0, 0, width, height, pixels, 0, width);
        BufferedImage scaled = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(width, height);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        scaled = scaleOp.filter(unscaled, scaled);
        return new Color(scaled.getRGB(0, 0));
    }
}
