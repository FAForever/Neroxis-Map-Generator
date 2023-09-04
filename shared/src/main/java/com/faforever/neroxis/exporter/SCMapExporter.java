package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.CubeMap;
import com.faforever.neroxis.map.Decal;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SkyBox;
import com.faforever.neroxis.map.WaveGenerator;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.dds.DDSHeader;
import com.faforever.neroxis.util.jsquish.Squish;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;
import com.faforever.neroxis.util.serial.biome.WaterSettings;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.List;

import static com.faforever.neroxis.util.EndianSwapper.swap;
import static com.faforever.neroxis.util.jsquish.Squish.compressImage;

public class SCMapExporter {
    public static final String PBR_DDS = "heightRoughness.dds";
    public static final String MAPWIDE_DDS = "mapwide.dds";
    public static File file;
    private static DataOutputStream out;

    public static void exportSCMAP(Path folderPath, SCMap map) throws IOException {
        file = folderPath.resolve(map.getFilePrefix() + ".scmap").toFile();
        boolean status = file.createNewFile();
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        // header
        writeInt(SCMap.SIGNATURE);
        writeInt(SCMap.VERSION_MAJOR);
        writeInt(-1091567891); // unknown
        writeInt(2); // unknown
        writeFloat(map.getSize()); // width
        writeFloat(map.getSize()); // height
        writeInt(0); // unknown
        writeShort((short) 0); // unknown

        DDSHeader previewDDSHeader = new DDSHeader();
        previewDDSHeader.setWidth(map.getPreview().getWidth());
        previewDDSHeader.setHeight(map.getPreview().getHeight());
        previewDDSHeader.setRGBBitCount(32);
        previewDDSHeader.setRBitMask(0x00FF0000);
        previewDDSHeader.setGBitMask(0x0000FF00);
        previewDDSHeader.setBBitMask(0x000000FF);
        previewDDSHeader.setABitMask(0xFF000000);

        writeRawImage(map.getPreview(), previewDDSHeader);

        writeInt(map.getMinorVersion());

        // heightmap
        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height
        writeFloat(map.getHeightMapScale());
        writeShorts(((DataBufferUShort) map.getHeightmap().getData().getDataBuffer()).getData()); // heightmap data

        writeByte((byte) 0); // unknown

        // textures
        writeStringNull(map.getTerrainShaderPath());
        writeStringNull(map.getBackgroundPath());
        writeStringNull(map.getSkyCubePath());
        List<CubeMap> cubeMaps = map.getBiome().terrainMaterials().getCubeMaps();
        writeInt(cubeMaps.size());
        for (CubeMap cubeMap : cubeMaps) {
            writeStringNull(cubeMap.getName());
            writeStringNull(cubeMap.getPath());
        }

        // lighting
        writeLightingSettings(map.getBiome().lightingSettings());

        // water
        writeWaterSettings(map.getBiome().waterSettings());

        // wave generators
        writeInt(map.getWaveGeneratorCount());
        for (WaveGenerator waveGenerator : map.getWaveGenerators()) {
            writeWaveGenerator(waveGenerator);
        }

        // terrain textures
        TerrainMaterials mapTerrainMaterials = map.getBiome().terrainMaterials();
        writeInt(map.getCartographicContourInterval());
        writeInt(map.getCartographicDeepWaterColor());
        writeInt(map.getCartographicMapContourColor());
        writeInt(map.getCartographicMapShoreColor());
        writeInt(map.getCartographicMapLandStartColor());
        writeInt(map.getCartographicMapLandEndColor());

        if (map.getMinorVersion() > 56) {
            writeFloat(0);
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.getTexturePaths()[i]);
            writeFloat(mapTerrainMaterials.getTextureScales()[i]);
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.getNormalPaths()[i]);
            writeFloat(mapTerrainMaterials.getNormalScales()[i]);
        }

        writeInt(0); // unknown
        writeInt(0); // unknown

        // decals
        if (!map.isUnexplored()) {
            writeInt(map.getDecalCount());
            for (Decal decal : map.getDecals()) {
                writeDecal(decal, map.getDecals().indexOf(decal));
            }
        } else {
            writeInt(0);
        }

        writeInt(map.getDecalGroupCount());
        for (DecalGroup decalGroup : map.getDecalGroups()) {
            writeDecalGroup(decalGroup, map.getDecalGroups().indexOf(decalGroup));
        }

        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height

        DDSHeader normalDDSHeader = new DDSHeader();
        normalDDSHeader.setWidth(map.getNormalMap().getWidth());
        normalDDSHeader.setHeight(map.getNormalMap().getHeight());
        normalDDSHeader.setFourCC("DXT5");

        // normal maps
        writeInt(1); // normal map count
        writeCompressedImage(map.getNormalMap(), normalDDSHeader);

        DDSHeader textureMaskLowDDSHeader = new DDSHeader();
        textureMaskLowDDSHeader.setWidth(map.getTextureMasksLow().getWidth());
        textureMaskLowDDSHeader.setHeight(map.getTextureMasksLow().getHeight());
        textureMaskLowDDSHeader.setRGBBitCount(32);
        textureMaskLowDDSHeader.setRBitMask(0x00FF0000);
        textureMaskLowDDSHeader.setGBitMask(0x0000FF00);
        textureMaskLowDDSHeader.setBBitMask(0x000000FF);
        textureMaskLowDDSHeader.setABitMask(0xFF000000);

        writeRawImage(map.getTextureMasksLow(), textureMaskLowDDSHeader);

        DDSHeader textureMaskHighDDSHeader = new DDSHeader();
        textureMaskHighDDSHeader.setWidth(map.getTextureMasksHigh().getWidth());
        textureMaskHighDDSHeader.setHeight(map.getTextureMasksHigh().getHeight());
        textureMaskHighDDSHeader.setRGBBitCount(32);
        textureMaskHighDDSHeader.setRBitMask(0x00FF0000);
        textureMaskHighDDSHeader.setGBitMask(0x0000FF00);
        textureMaskHighDDSHeader.setBBitMask(0x000000FF);
        textureMaskHighDDSHeader.setABitMask(0xFF000000);

        writeRawImage(map.getTextureMasksHigh(), textureMaskHighDDSHeader);

        DDSHeader waterDDSHeader = new DDSHeader();
        waterDDSHeader.setWidth(map.getWaterMap().getWidth());
        waterDDSHeader.setHeight(map.getWaterMap().getHeight());
        waterDDSHeader.setFourCC("DXT5");

        // water maps
        writeInt(1); // unknown
        writeCompressedImage(map.getWaterMap(), waterDDSHeader); // watermap data
        writeImageBytes(map.getWaterFoamMap()); // water foam mask data
        writeImageBytes(map.getWaterFlatnessMap()); // water flatness mask data
        writeImageBytes(map.getWaterDepthBiasMap()); // water depth bias mask data

        // terrain type
        writeImageBytes(map.getTerrainType()); // terrain type data

        // additional skybox
        if (map.getMinorVersion() >= 60) {
            writeSkyBox(map.getSkyBox());
        }

        //props
        if (!map.isUnexplored()) {
            writeInt(map.getPropCount());
            for (Prop prop : map.getProps()) {
                writeProp(prop);
            }
        } else {
            writeInt(0);
        }

        out.flush();
        out.close();
    }

    public static void exportSCMapString(Path folderPath, String mapname, SCMap map) {
        map.writeToFile(folderPath.resolve(mapname).resolve("debug").resolve(mapname + ".txt"));
    }

    public static void exportPreview(Path folderPath, SCMap map) throws IOException {
        final String fileFormat = "png";
        File previewFile = folderPath.resolve(map.getFilePrefix() + "_preview." + fileFormat).toFile();
        BufferedImage mapPreview = map.getPreview();
        BufferedImage previewCopy = new BufferedImage(mapPreview.getWidth(), mapPreview.getHeight(),
                                                      BufferedImage.TYPE_INT_ARGB);
        Graphics previewCopyGraphics = previewCopy.getGraphics();
        previewCopyGraphics.drawImage(mapPreview, 0, 0, null);
        previewCopyGraphics.dispose();
        RenderedImage renderedImage = PreviewGenerator.addMarkers(previewCopy, map);
        try {
            ImageIO.write(renderedImage, fileFormat, previewFile);
        } catch (IOException e) {
            System.out.print("Could not write the preview image\n" + e);
        }
    }

    public static void exportMapwideTexture(Path folderPath, SCMap map) throws IOException {
        byte[] rawTexture = map.getRawMapTexture();
        Path textureDirectory = Paths.get("env", "texture");
        Path filePath = textureDirectory.resolve(MAPWIDE_DDS);
        Path writingPath = folderPath.resolve(filePath);
        Files.createDirectories(writingPath.getParent());
        try {
            Files.write(writingPath, rawTexture, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.out.print("Could not write the map-wide texture\n" + e);
        }
    }

    public static void exportPBR(Path folderPath, SCMap map) throws IOException {
        URL sourceURL = SCMapExporter.class.getResource("/images/heightRoughness.png");
        if (sourceURL != null) {
            BufferedImage image = ImageIO.read(sourceURL);
            Path textureDirectory = Paths.get("env", "texture");
            Path filePath = textureDirectory.resolve(PBR_DDS);
            Path writingPath = folderPath.resolve(filePath);
            Files.createDirectories(writingPath.getParent());
            try {
                ImageUtil.writeNormalDDS(image, writingPath);
            } catch (IOException e) {
                System.out.print("Could not write the pbr texture\n" + e);
            }
        } else {
            System.out.print("Can't find pbr texture to write\n");
        }
    }

    private static void writeFloat(float f) throws IOException {
        out.writeInt(swap(Float.floatToRawIntBits(f)));
    }

    private static void writeInt(int i) throws IOException {
        out.writeInt(swap(i));
    }

    private static void writeShort(short s) throws IOException {
        out.writeShort(swap(s));
    }

    private static void writeByte(byte b) throws IOException {
        out.writeByte(b);
    }

    private static void writeBytes(byte[] b) throws IOException {
        out.write(b);
    }

    private static void writeShorts(short[] s) throws IOException {
        for (short value : s) {
            writeShort(value);
        }
    }

    private static void writeInts(int[] data) throws IOException {
        for (int i : data) {
            writeInt(i);
        }
    }

    private static void writeStringNull(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.writeByte(s.charAt(i));
        }
        out.writeByte(0);
    }

    private static void writeString(String s) throws IOException {
        writeInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.writeByte(s.charAt(i));
        }
    }

    private static void writeVector3f(Vector3 v) throws IOException {
        writeFloat(v.getX());
        writeFloat(v.getY());
        writeFloat(v.getZ());
    }

    private static void writeVector4f(Vector4 v) throws IOException {
        writeFloat(v.getX());
        writeFloat(v.getY());
        writeFloat(v.getZ());
        writeFloat(v.getW());
    }

    private static void writeVector2f(Vector2 v) throws IOException {
        writeFloat(v.getX());
        writeFloat(v.getY());
    }

    private static void writeProp(Prop prop) throws IOException {
        writeStringNull(prop.getPath());
        writeVector3f(prop.getPosition());
        writeVector3f(new Vector3((float) StrictMath.cos(prop.getRotation()), 0f,
                                  (float) StrictMath.sin(prop.getRotation())));
        writeVector3f(new Vector3(0f, 1f, 0f));
        writeVector3f(new Vector3((float) -StrictMath.sin(prop.getRotation()), 0f,
                                  (float) StrictMath.cos(prop.getRotation())));
        writeVector3f(new Vector3(1f, 1f, 1f)); //scale
    }

    private static void writeDecal(Decal decal, int id) throws IOException {
        writeInt(id);
        writeInt(decal.getType().getTypeNum());
        writeInt(2);
        writeString(decal.getPath());
        writeString("");
        writeVector3f(decal.getScale());
        writeVector3f(decal.getPosition());
        writeVector3f(decal.getRotation());
        writeFloat(decal.getCutOffLOD());
        writeFloat(0);
        writeInt(-1);
    }

    private static void writeDecalGroup(DecalGroup decalGroup, int id) throws IOException {
        writeInt(id);
        writeStringNull(decalGroup.getName());
        writeInt(decalGroup.getData().length);
        for (int j = 0; j < decalGroup.getData().length; j++) {
            writeInt(decalGroup.getData()[j]);
        }
    }

    private static void writeWaveGenerator(WaveGenerator waveGenerator) throws IOException {
        writeStringNull(waveGenerator.getTextureName());
        writeStringNull(waveGenerator.getRampName());
        writeVector3f(waveGenerator.getPosition());
        writeFloat(waveGenerator.getRotation());
        writeVector3f(waveGenerator.getVelocity());
        writeFloat(waveGenerator.getLifeTimeFirst());
        writeFloat(waveGenerator.getLifeTimeSecond());
        writeFloat(waveGenerator.getPeriodFirst());
        writeFloat(waveGenerator.getPeriodSecond());
        writeFloat(waveGenerator.getScaleFirst());
        writeFloat(waveGenerator.getScaleSecond());
        writeFloat(waveGenerator.getFrameCount());
        writeFloat(waveGenerator.getFrameRateFirst());
        writeFloat(waveGenerator.getFrameRateSecond());
        writeFloat(waveGenerator.getStripCount());
    }

    private static void writeWaterSettings(WaterSettings waterSettings) throws IOException {
        writeByte((byte) (waterSettings.isWaterPresent() ? 1 : 0));
        writeFloat(waterSettings.getElevation());
        writeFloat(waterSettings.getElevationDeep());
        writeFloat(waterSettings.getElevationAbyss());
        writeVector3f(waterSettings.getSurfaceColor());
        writeVector2f(waterSettings.getColorLerp());
        writeFloat(waterSettings.getRefractionScale());
        writeFloat(waterSettings.getFresnelBias());
        writeFloat(waterSettings.getFresnelPower());
        writeFloat(waterSettings.getUnitReflection());
        writeFloat(waterSettings.getSkyReflection());
        writeFloat(waterSettings.getSunShininess());
        writeFloat(waterSettings.getSunStrength());
        writeVector3f(waterSettings.getSunDirection());
        writeVector3f(waterSettings.getSunColor());
        writeFloat(waterSettings.getSunReflection());
        writeFloat(waterSettings.getSunGlow());
        writeStringNull(waterSettings.getTexPathCubemap());
        writeStringNull(waterSettings.getTexPathWaterRamp());

        // waves
        for (WaterSettings.WaveTexture waveTexture : waterSettings.getWaveTextures()) {
            writeFloat(waveTexture.getNormalRepeat());
        }

        for (WaterSettings.WaveTexture waveTexture : waterSettings.getWaveTextures()) {
            writeVector2f(waveTexture.getNormalMovement());
            writeStringNull(waveTexture.getTexPath());
        }
    }

    private static void writeLightingSettings(LightingSettings lightingSettings) throws IOException {
        writeFloat(lightingSettings.getLightingMultiplier());
        writeVector3f(lightingSettings.getSunDirection());
        writeVector3f(lightingSettings.getSunAmbience());
        writeVector3f(lightingSettings.getSunColor());
        writeVector3f(lightingSettings.getShadowFillColor());
        writeVector4f(lightingSettings.getSpecularColor());
        writeFloat(lightingSettings.getBloom());
        writeVector3f(lightingSettings.getFogColor());
        writeFloat(lightingSettings.getFogStart());
        writeFloat(lightingSettings.getFogEnd());
    }

    private static void writeSkyBox(SkyBox skyBox) throws IOException {
        writeVector3f(skyBox.getPosition());
        writeFloat(skyBox.getHorizonHeight());
        writeFloat(skyBox.getScale());
        writeFloat(skyBox.getSubHeight());
        writeInt(skyBox.getSubDivAx());
        writeInt(skyBox.getSubDivHeight());
        writeFloat(skyBox.getZenithHeight());
        writeVector3f(skyBox.getHorizonColor());
        writeVector3f(skyBox.getZenithColor());
        writeFloat(skyBox.getDecalGlowMultiplier());

        writeStringNull(skyBox.getAlbedo());
        writeStringNull(skyBox.getGlow());

        // Array of Planets/Stars
        writeInt(skyBox.getPlanets().length);
        for (SkyBox.Planet planet : skyBox.getPlanets()) {
            writeVector3f(planet.getPosition());
            writeFloat(planet.getRotation());
            writeVector2f(planet.getScale());
            writeVector4f(planet.getUv());
        }

        // Mid
        writeByte((byte) skyBox.getMidRgbColor().getRed());
        writeByte((byte) skyBox.getMidRgbColor().getBlue());
        writeByte((byte) skyBox.getMidRgbColor().getGreen());

        // Cirrus
        writeFloat(skyBox.getCirrusMultiplier());
        writeVector3f(skyBox.getCirrusColor());
        writeStringNull(skyBox.getCirrusTexture());

        writeInt(skyBox.getCirrusLayers().length);
        for (SkyBox.Cirrus cirrus : skyBox.getCirrusLayers()) {
            writeVector2f(cirrus.getFrequency());
            writeFloat(cirrus.getSpeed());
            writeVector2f(cirrus.getDirection());
        }
        writeFloat(skyBox.getClouds7());
    }

    private static void writeRawImage(BufferedImage image, DDSHeader ddsHeader) throws IOException {
        byte[] headerBytes = ddsHeader.toBytes();
        writeInt(headerBytes.length + image.getWidth() * image.getHeight() * 4); // image byte count
        writeBytes(headerBytes);
        writeInts(((DataBufferInt) image.getData().getDataBuffer()).getData()); // image data
    }

    private static void writeCompressedImage(BufferedImage image, DDSHeader ddsHeader) throws IOException {
        int[] imageData = ((DataBufferInt) image.getData().getDataBuffer()).getData();
        byte[] headerBytes = ddsHeader.toBytes();
        ByteBuffer imageBytes = ByteBuffer.allocate(imageData.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        imageBytes.asIntBuffer().put(imageData);
        byte[] compressedData = compressImage(imageBytes.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null,
                                              Squish.CompressionType.DXT5);

        writeInt(headerBytes.length + compressedData.length); // image byte count
        writeBytes(headerBytes);
        writeBytes(compressedData); // image data
    }

    private static void writeImageBytes(BufferedImage image) throws IOException {
        writeBytes(((DataBufferByte) image.getData().getDataBuffer()).getData());
    }
}
