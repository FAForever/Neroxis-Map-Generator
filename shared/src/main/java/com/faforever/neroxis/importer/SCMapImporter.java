package com.faforever.neroxis.importer;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.map.CubeMap;
import com.faforever.neroxis.map.Decal;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.DecalType;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SkyBox;
import com.faforever.neroxis.map.WaveGenerator;
import static com.faforever.neroxis.util.EndianSwapper.swap;
import com.faforever.neroxis.util.dds.DDSHeader;
import com.faforever.neroxis.util.jsquish.Squish;
import static com.faforever.neroxis.util.jsquish.Squish.decompressImage;
import com.faforever.neroxis.util.serial.biome.DecalMaterials;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;
import com.faforever.neroxis.util.serial.biome.WaterSettings;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public strictfp class SCMapImporter {
    public static File file;
    private static DataInputStream in;

    public static SCMap importSCMAP(Path folderPath) throws IOException {
        File dir = folderPath.toFile();

        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
        if (mapFiles == null || mapFiles.length == 0) {
            throw new IllegalArgumentException("Folder does not contain an scmap file");
        }

        file = mapFiles[0];

        in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        // header
        if (readInt() != SCMap.SIGNATURE) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (readInt() != SCMap.VERSION_MAJOR) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (readInt() != -1091567891) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (readInt() != 2) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        readFloat(); // width as float
        readFloat(); // height as float
        if (readInt() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (readShort() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        int[] previewImageData = readRawImage();
        int version = readInt();
        if (version != 56 && version != 60) {
            throw new UnsupportedEncodingException(String.format("SCMap version %d not supported", version));
        }

        // heightmap
        int widthInt = readInt();
        int heightInt = readInt();
        float heightMapScale = readFloat();
        short[] heightMapData = readShorts((widthInt + 1) * (heightInt + 1));
        if (readByte() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }

        // textures
        String shaderPath = readStringNull();
        String backgroundPath = readStringNull();
        String skyCubePath = readStringNull();
        int cubeMapCount = readInt();
        CubeMap[] cubeMaps = new CubeMap[cubeMapCount];
        for (int i = 0; i < cubeMapCount; i++) {
            String name = readStringNull();
            String path = readStringNull();
            cubeMaps[i] = new CubeMap(name, path);
        }

        // lighting
        LightingSettings mapLightingSettings = readLightingSettings();

        // water
        WaterSettings mapWaterSettings = readWaterSettings();

        // wave generators
        int waveGeneratorCount = readInt();
        WaveGenerator[] waveGenerators = new WaveGenerator[waveGeneratorCount];
        for (int i = 0; i < waveGeneratorCount; i++) {
            waveGenerators[i] = readWaveGenerator();
        }

        // terrain textures
        TerrainMaterials mapTerrainMaterials = new TerrainMaterials();
        int miniMapContourInterval = readInt();
        int miniMapDeepWaterColor = readInt();
        int miniMapContourColor = readInt();
        int miniMapShoreColor = readInt();
        int miniMapLandStartColor = readInt();
        int miniMapLandEndColor = readInt();

        if (version > 56) {
            readFloat(); //unknown
        }

        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            mapTerrainMaterials.getTexturePaths()[i] = readStringNull();
            mapTerrainMaterials.getTextureScales()[i] = readFloat();
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            mapTerrainMaterials.getNormalPaths()[i] = readStringNull();
            mapTerrainMaterials.getNormalScales()[i] = readFloat();
        }

        readInt(); // unknown
        readInt(); // unknown

        // decals
        int decalCount = readInt();
        Decal[] decals = new Decal[decalCount];
        for (int i = 0; i < decalCount; i++) {
            decals[i] = readDecal();
        }

        //decal group count
        int groupCount = readInt();
        DecalGroup[] decalGroups = new DecalGroup[groupCount];
        for (int i = 0; i < groupCount; i++) {
            decalGroups[i] = readDecalGroup();
        }

        readInt(); // width as int
        readInt(); // height as int

        // normal maps
        // normal map count
        if (readInt() != 1) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        int[] normalMapData = readCompressedImage();

        // texture maps
        int[] textureMaskLowData = readRawImage();
        int[] textureMaskHighData = readRawImage();

        // water maps
        if (readInt() != 1) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        int[] waterMapData = readCompressedImage();
        int halfSize = (heightInt / 2) * (widthInt / 2);
        byte[] waterFoamMaskData = readBytes(halfSize);
        byte[] waterFlatnessData = readBytes(halfSize);
        byte[] waterDepthBiasMaskData = readBytes(halfSize);

        // terrain type
        byte[] terrainTypeData = readBytes(widthInt * heightInt);

        // Additional Skybox
        SkyBox skyBox = null;
        if (version >= 60) {
            skyBox = readSkyBox();
        }

        // props
        int propCount = readInt();
        Prop[] props = new Prop[propCount];
        for (int i = 0; i < propCount; i++) {
            props[i] = readProp();
        }

        in.close();

        SCMap map = new SCMap(widthInt,
                              new Biome("loaded", mapTerrainMaterials, new PropMaterials(), new DecalMaterials(),
                                        mapWaterSettings, mapLightingSettings));
        map.setFilePrefix(file.getName().replace(".scmap", ""));
        map.setMinorVersion(version);
        map.setTerrainShaderPath(shaderPath);
        map.setBackgroundPath(backgroundPath);
        map.setSkyCubePath(skyCubePath);
        map.setHeightMapScale(heightMapScale);
        map.setSkyBox(skyBox);
        map.setCartographicContourInterval(miniMapContourInterval);
        map.setCartographicDeepWaterColor(miniMapDeepWaterColor);
        map.setCartographicMapContourColor(miniMapContourColor);
        map.setCartographicMapShoreColor(miniMapShoreColor);
        map.setCartographicMapLandStartColor(miniMapLandStartColor);
        map.setCartographicMapLandEndColor(miniMapLandEndColor);

        map.setPreview(getBufferedImageFromRawData(BufferedImage.TYPE_INT_ARGB, previewImageData));

        map.setHeightmap(getBufferedImageFromRawData(BufferedImage.TYPE_USHORT_GRAY, getIntegerArray(heightMapData)));

        map.setNormalMap(getBufferedImageFromRawData(BufferedImage.TYPE_INT_ARGB, normalMapData));

        map.setTextureMasksLow(getBufferedImageFromRawData(BufferedImage.TYPE_INT_ARGB, textureMaskLowData));

        map.setTextureMasksHigh(getBufferedImageFromRawData(BufferedImage.TYPE_INT_ARGB, textureMaskHighData));

        map.setWaterMap(getBufferedImageFromRawData(BufferedImage.TYPE_INT_ARGB, waterMapData));

        map.setWaterFoamMap(
                getBufferedImageFromRawData(BufferedImage.TYPE_BYTE_GRAY, getIntegerArray(waterFoamMaskData)));

        map.setWaterFlatnessMap(
                getBufferedImageFromRawData(BufferedImage.TYPE_BYTE_GRAY, getIntegerArray(waterFlatnessData)));

        map.setWaterDepthBiasMap(
                getBufferedImageFromRawData(BufferedImage.TYPE_BYTE_GRAY, getIntegerArray(waterDepthBiasMaskData)));

        map.setTerrainType(getBufferedImageFromRawData(BufferedImage.TYPE_BYTE_GRAY, getIntegerArray(terrainTypeData)));

        for (WaveGenerator waveGenerator : waveGenerators) {
            map.addWaveGenerator(waveGenerator);
        }

        for (Prop prop : props) {
            map.addProp(prop);
        }
        for (Decal decal : decals) {
            map.addDecal(decal);
        }
        for (DecalGroup decalGroup : decalGroups) {
            map.addDecalGroup(decalGroup);
        }
        map.getCubeMaps().clear();
        for (CubeMap cubeMap : cubeMaps) {
            map.addCubeMap(cubeMap);
        }
        return map;
    }

    private static float readFloat() throws IOException {
        return Float.intBitsToFloat(swap(in.readInt()));
    }

    private static int readInt() throws IOException {
        return swap(in.readInt());
    }

    private static short readShort() throws IOException {
        return swap(in.readShort());
    }

    private static byte readByte() throws IOException {
        return in.readByte();
    }

    private static byte[] readBytes(int numBytes) throws IOException {
        byte[] readBytes = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            readBytes[i] = readByte();
        }
        return readBytes;
    }

    private static short[] readShorts(int numShorts) throws IOException {
        short[] readShorts = new short[numShorts];
        for (int i = 0; i < numShorts; i++) {
            readShorts[i] = readShort();
        }
        return readShorts;
    }

    private static int[] readInts(int numInts) throws IOException {
        int[] readInts = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            readInts[i] = readInt();
        }
        return readInts;
    }

    private static String readStringNull() throws IOException {
        StringBuilder readString = new StringBuilder();
        byte read = readByte();
        while (read != 0) {
            readString.append((char) read);
            read = readByte();
        }
        return readString.toString();
    }

    private static String readString(int length) throws IOException {
        StringBuilder readString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            byte read = readByte();
            readString.append((char) read);
        }
        return readString.toString();
    }

    private static Vector3 readVector3f() throws IOException {
        return new Vector3(readFloat(), readFloat(), readFloat());
    }

    private static Vector4 readVector4f() throws IOException {
        return new Vector4(readFloat(), readFloat(), readFloat(), readFloat());
    }

    private static Vector2 readVector2f() throws IOException {
        return new Vector2(readFloat(), readFloat());
    }

    private static LightingSettings readLightingSettings() throws IOException {
        LightingSettings lightingSettings = new LightingSettings();
        lightingSettings.setLightingMultiplier(readFloat());
        lightingSettings.setSunDirection(readVector3f());
        lightingSettings.setSunAmbience(readVector3f());
        lightingSettings.setSunColor(readVector3f());
        lightingSettings.setShadowFillColor(readVector3f());
        lightingSettings.setSpecularColor(readVector4f());
        lightingSettings.setBloom(readFloat());
        lightingSettings.setFogColor(readVector3f());
        lightingSettings.setFogStart(readFloat());
        lightingSettings.setFogEnd(readFloat());
        return lightingSettings;
    }

    private static WaterSettings readWaterSettings() throws IOException {
        WaterSettings waterSettings = new WaterSettings();
        waterSettings.setWaterPresent(readByte() == 1);
        waterSettings.setElevation(readFloat());
        waterSettings.setElevationDeep(readFloat());
        waterSettings.setElevationAbyss(readFloat());
        waterSettings.setSurfaceColor(readVector3f());
        waterSettings.setColorLerp(readVector2f());
        waterSettings.setRefractionScale(readFloat());
        waterSettings.setFresnelBias(readFloat());
        waterSettings.setFresnelPower(readFloat());
        waterSettings.setUnitReflection(readFloat());
        waterSettings.setSkyReflection(readFloat());
        waterSettings.setSunShininess(readFloat());
        waterSettings.setSunStrength(readFloat());
        waterSettings.setSunDirection(readVector3f());
        waterSettings.setSunColor(readVector3f());
        waterSettings.setSunReflection(readFloat());
        waterSettings.setSunGlow(readFloat());
        waterSettings.setTexPathCubemap(readStringNull());
        waterSettings.setTexPathWaterRamp(readStringNull());

        // waves
        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            waterSettings.getWaveTextures().get(i).setNormalRepeat(readFloat());
        }

        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            waterSettings.getWaveTextures().get(i).setNormalMovement(readVector2f());
            waterSettings.getWaveTextures().get(i).setTexPath(readStringNull());
        }

        return waterSettings;
    }

    private static WaveGenerator readWaveGenerator() throws IOException {
        String textureName = readStringNull();
        String rampName = readStringNull();
        Vector3 position = readVector3f();
        float rotation = readFloat();
        Vector3 velocity = readVector3f();

        WaveGenerator waveGenerator = new WaveGenerator(textureName, rampName, position, rotation, velocity);

        waveGenerator.setLifeTimeFirst(readFloat());
        waveGenerator.setLifeTimeSecond(readFloat());
        waveGenerator.setPeriodFirst(readFloat());
        waveGenerator.setPeriodSecond(readFloat());
        waveGenerator.setScaleFirst(readFloat());
        waveGenerator.setScaleSecond(readFloat());
        waveGenerator.setFrameCount(readFloat());
        waveGenerator.setFrameRateFirst(readFloat());
        waveGenerator.setFrameRateSecond(readFloat());
        waveGenerator.setStripCount(readFloat());

        return waveGenerator;
    }

    private static Decal readDecal() throws IOException {
        readInt(); // id
        int type = readInt();
        int textureCount = readInt();
        String[] texturePaths = new String[textureCount];
        for (int j = 0; j < textureCount; j++) {
            int stringLength = readInt();
            texturePaths[j] = readString(stringLength);
        }
        Vector3 scale = readVector3f();
        Vector3 position = readVector3f();
        Vector3 rotation = readVector3f();
        float cutOffLOD = readFloat();
        readFloat(); // nearCutOffLOD
        readInt(); // ownerArmy
        return new Decal(texturePaths[0], position, rotation, scale, cutOffLOD, DecalType.of(type));
    }

    private static DecalGroup readDecalGroup() throws IOException {
        readInt(); // id
        String name = readStringNull();
        int length = readInt();
        int[] data = new int[length];
        for (int j = 0; j < length; j++) {
            data[j] = readInt();
        }
        return new DecalGroup(name, data);
    }

    private static int[] readRawImage() throws IOException {
        int byteCount = readInt() - 128;
        DDSHeader ddsHeader = DDSHeader.parseHeader(readBytes(128));
        if (ddsHeader.getWidth() * ddsHeader.getHeight() * 4 != byteCount) {
            throw new UnsupportedEncodingException("Not a recognized dds image format");
        }
        return readInts(byteCount / 4);
    }

    private static int[] readCompressedImage() throws IOException {
        int byteCount = readInt() - 128;
        int[] data = new int[byteCount / 4];
        DDSHeader ddsHeader = DDSHeader.parseHeader(readBytes(128));
        ByteBuffer.wrap(decompressImage(null, ddsHeader.getWidth(), ddsHeader.getHeight(), readBytes(byteCount),
                                        Squish.CompressionType.DXT5)).asIntBuffer().get(data);
        return data;
    }

    private static SkyBox readSkyBox() throws IOException {
        SkyBox skyBox = new SkyBox();
        skyBox.setPosition(readVector3f());
        skyBox.setHorizonHeight(readFloat());
        skyBox.setScale(readFloat());
        skyBox.setSubHeight(readFloat());
        skyBox.setSubDivAx(readInt());
        skyBox.setSubDivHeight(readInt());
        skyBox.setZenithHeight(readFloat());
        skyBox.setHorizonColor(readVector3f());
        skyBox.setZenithColor(readVector3f());
        skyBox.setDecalGlowMultiplier(readFloat());

        skyBox.setAlbedo(readStringNull());
        skyBox.setGlow(readStringNull());

        // Array of Planets/Stars
        int length = readInt();
        SkyBox.Planet[] planets = new SkyBox.Planet[length];
        for (int i = 0; i < length; i++) {
            planets[i] = new SkyBox.Planet();
            planets[i].setPosition(readVector3f());
            planets[i].setRotation(readFloat());
            planets[i].setScale(readVector2f());
            planets[i].setUv(readVector4f());
        }
        skyBox.setPlanets(planets);

        // Mid
        skyBox.setMidRgbColor(new Color(readByte(), readByte(), readByte(), 0));

        // Cirrus
        skyBox.setCirrusMultiplier(readFloat());
        skyBox.setCirrusColor(readVector3f());
        skyBox.setCirrusTexture(readStringNull());

        int cirrusLayerCount = readInt();
        SkyBox.Cirrus[] cirrusLayers = new SkyBox.Cirrus[cirrusLayerCount];
        for (int i = 0; i < cirrusLayerCount; i++) {
            cirrusLayers[i] = new SkyBox.Cirrus();
            cirrusLayers[i].setFrequency(readVector2f());
            cirrusLayers[i].setSpeed(readFloat());
            cirrusLayers[i].setDirection(readVector2f());
        }
        skyBox.setCirrusLayers(cirrusLayers);
        skyBox.setClouds7(readFloat());
        return skyBox;
    }

    private static Prop readProp() throws IOException {
        String path = readStringNull();
        Vector3 position = readVector3f();
        Vector3 rotationX = readVector3f();
        readVector3f(); // Y rotation
        readVector3f(); // Z rotation
        readVector3f(); // scale
        float rotation = (float) StrictMath.atan2(rotationX.getZ(), rotationX.getX());
        return new Prop(path, position, rotation);
    }

    private static BufferedImage getBufferedImageFromRawData(int bufferedImageType, int[] imageData) {
        int imageSize = (int) StrictMath.sqrt(imageData.length);
        BufferedImage image = new BufferedImage(imageSize, imageSize, bufferedImageType);
        DataBuffer imageDataBuffer = image.getRaster().getDataBuffer();
        for (int i = 0; i < imageDataBuffer.getSize(); i++) {
            imageDataBuffer.setElem(i, imageData[i]);
        }
        return image;
    }

    private static int[] getIntegerArray(byte[] bytes) {
        int[] intArray = new int[bytes.length];
        int i = 0;
        for (int value : bytes) {
            intArray[i++] = value;
        }
        return intArray;
    }

    private static int[] getIntegerArray(short[] shorts) {
        int[] intArray = new int[shorts.length];
        int i = 0;
        for (int value : shorts) {
            intArray[i++] = value;
        }
        return intArray;
    }
}

