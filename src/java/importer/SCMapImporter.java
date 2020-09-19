package importer;

import biomes.Biome;
import map.*;
import util.DDSHeader;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.io.*;
import java.nio.file.Path;

import static util.Swapper.swap;

public strictfp class SCMapImporter {

    public static File file;

    private static DataInputStream in;

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No scmap file in map folder");
        }
        SCMap map = loadSCMAP(mapFiles[0].toPath());
    }

    public static SCMap loadSCMAP(Path filePath) throws IOException {
        file = filePath.toAbsolutePath().toFile();

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
        float width = readFloat(); // width
        float height = readFloat(); // height
        if (readInt() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (readShort() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        int previewImageSize = readInt() - 128;
        DDSHeader previewHeader = DDSHeader.parseHeader(readBytes(128));
        int[] previewImageData = readInts(previewImageSize / 4);
        int version = readInt();
        if (version != SCMap.VERSION_MINOR && version != 60) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }

        // heightmap
        int widthInt = readInt();
        int heightInt = readInt();
        if (readFloat() != SCMap.HEIGHTMAP_SCALE) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        short[] heightMapData = readShorts((widthInt + 1) * (heightInt + 1));

        if (readByte() != 0) {
            throw new UnsupportedEncodingException("File not valid SCMap");
        }

        // textures
        String shaderPath = readStringNull();
        if (!shaderPath.equals(SCMap.TERRAIN_SHADER_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        String backgroundPath = readStringNull();
        if (!backgroundPath.equals(SCMap.BACKGROUND_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        String skyCubePath = readStringNull();
        if (!skyCubePath.equals(SCMap.SKYCUBE_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        int cubemapCount = readInt();
        for (int i = 0; i < cubemapCount; i++) {
            if (!readStringNull().equals(SCMap.CUBEMAP_NAME)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
            }
            if (!readStringNull().equals(SCMap.CUBEMAP_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
            }
        }

        // lighting
        LightingSettings mapLightingSettings = new LightingSettings();
        mapLightingSettings.setLightingMultiplier(readFloat());
        mapLightingSettings.setSunDirection(readVector3f());
        mapLightingSettings.setSunAmbience(readVector3f());
        mapLightingSettings.setSunColor(readVector3f());
        mapLightingSettings.setShadowFillColor(readVector3f());
        mapLightingSettings.setSpecularColor(readVector4f());
        mapLightingSettings.setBloom(readFloat());
        mapLightingSettings.setFogColor(readVector3f());
        mapLightingSettings.setFogStart(readFloat());
        mapLightingSettings.setFogEnd(readFloat());

        // water
        WaterSettings mapWaterSettings = new WaterSettings();
        mapWaterSettings.setWaterPresent(readByte() == 1);
        mapWaterSettings.setElevation(readFloat());
        mapWaterSettings.setElevationDeep(readFloat());
        mapWaterSettings.setElevationAbyss(readFloat());
        mapWaterSettings.setSurfaceColor(readVector3f());
        mapWaterSettings.setColorLerp(readVector2f());
        mapWaterSettings.setRefractionScale(readFloat());
        mapWaterSettings.setFresnelBias(readFloat());
        mapWaterSettings.setFresnelPower(readFloat());
        mapWaterSettings.setUnitReflection(readFloat());
        mapWaterSettings.setSkyReflection(readFloat());
        mapWaterSettings.setSunShininess(readFloat());
        mapWaterSettings.setSunStrength(readFloat());
        mapWaterSettings.setSunDirection(readVector3f());
        mapWaterSettings.setSunColor(readVector3f());
        mapWaterSettings.setSunReflection(readFloat());
        mapWaterSettings.setSunGlow(readFloat());
        mapWaterSettings.setTexPathCubemap(readStringNull());
        mapWaterSettings.setTexPathWaterRamp(readStringNull());

        // waves
        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            mapWaterSettings.getWaveTextures()[i].setNormalRepeat(readFloat());
        }

        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            mapWaterSettings.getWaveTextures()[i].setNormalMovement(readVector2f());
            mapWaterSettings.getWaveTextures()[i].setTexPath(readStringNull());
        }

        // wave generators
        int waveGeneratorCount = readInt();
        WaveGenerator[] waveGenerators = new WaveGenerator[waveGeneratorCount];
        for (int i = 0; i < waveGeneratorCount; i++) {
            String textureName = readStringNull();
            String rampName = readStringNull();
            Vector3f position = readVector3f();
            float rotation = readFloat();
            Vector3f velocity = readVector3f();
            float lifetimeFirst = readFloat();
            float lifetimeSecond = readFloat();
            float periodFirst = readFloat();
            float periodSecond = readFloat();
            float scaleFirst = readFloat();
            float scaleSecond = readFloat();

            float frameCount = readFloat();
            float frameRateFirst = readFloat();
            float frameRateSecond = readFloat();
            float stripCount = readFloat();

            waveGenerators[i] = new WaveGenerator(textureName, rampName, position, rotation, velocity);
        }

        // terrain textures
        TerrainMaterials mapTerrainMaterials = new TerrainMaterials(new Material[0], new Material("", "", 0));
        int miniMapContourInterval = readInt();
        int miniMapDeepWaterColor = readInt();
        int miniMapContourColor = readInt();
        int miniMapShoreColor = readInt();
        int miniMapLandStartColor = readInt();
        int miniMapLandEndColor = readInt();

        if (version > 56) {
            float unknown14 = readFloat();
        }

        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            mapTerrainMaterials.getTexturePaths()[i] = readStringNull();
            mapTerrainMaterials.getTextureScales()[i] = readFloat();
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            mapTerrainMaterials.getNormalPaths()[i] = readStringNull();
            mapTerrainMaterials.getNormalScales()[i] = readFloat();
        }

        int unknown1 = readInt();

        int unknown2 = readInt();

        // decals
        // decal count
        int decalCount = readInt();
        Decal[] decals = new Decal[decalCount];
        for (int i = 0; i < decalCount; i++) {
            int id = readInt();
            int type = readInt();
            int textureCount = readInt();
            String[] texturePaths = new String[textureCount];
            for (int j = 0; j < textureCount; j++) {
                int stringLength = readInt();
                texturePaths[j] = readString(stringLength);
            }
            Vector3f scale = readVector3f();
            Vector3f position = readVector3f();
            Vector3f rotation = readVector3f();
            float cutOffLOD = readFloat();
            float nearCutOffLOD = readFloat();
            int ownerArmy = readInt();
            float rotationFloat = (float) StrictMath.atan2(rotation.z, rotation.x);
            decals[i] = new Decal(texturePaths[0], position, rotationFloat, scale, cutOffLOD);
        }

        //decal group count
        int groupCount = readInt();
        for (int i = 0; i < groupCount; i++) {
            int id = readInt();
            String name = readStringNull();
            int length = readInt();
            int[] data = new int[length];
            for (int j = 0; j < length; j++) {
                data[j] = readInt();
            }
        }

        int widthInt2 = readInt();
        int heightInt2 = readInt();

        // normal maps
        // normal map count
        if (readInt() != 1) {
            throw new UnsupportedEncodingException("File not valid Generated SCMap");
        }
        int normalMapByteCount = readInt() - 128;
        DDSHeader normalDDSHeader = DDSHeader.parseHeader(readBytes(128));
        int[] normalMapData = readInts(normalMapByteCount / 4);

        // texture maps
        int textureMaskLowByteCount = readInt() - 128;
        DDSHeader textureMaskLowDDSHeader = DDSHeader.parseHeader(readBytes(128));
        int[] textureMaskLowData = readInts(textureMaskLowByteCount / 4);
        int textureMaskHighByteCount = readInt() - 128;
        DDSHeader textureMaskHighDDSHeader = DDSHeader.parseHeader(readBytes(128));
        int[] textureMaskHighData = readInts(textureMaskHighByteCount / 4);

        // water maps
        if (readInt() != 1) {
            throw new UnsupportedEncodingException("File not valid Generated SCMap");
        }
        int waterMapByteCount = readInt() - 128;
        DDSHeader waterMapDDSHeader = DDSHeader.parseHeader(readBytes(128));
        byte[] waterMapData = readBytes(waterMapByteCount);
        byte[] waterFoamMaskData = readBytes(waterMapByteCount);
        byte[] waterFlatnessData = readBytes(waterMapByteCount);
        byte[] waterDepthBiasMaskData = readBytes(waterMapByteCount);

        // terrain type
        int[] terrainTypeData = readInts(widthInt * heightInt / 4);

        // Additional Skybox
        if (version >= 60) {
            Vector3f position = readVector3f();
            float horizonHeight = readFloat();
            float scale = readFloat();
            float subHeight = readFloat();
            int subDivAx = readInt();
            int subDivHeight = readInt();
            float zenithHeight = readFloat();
            Vector3f horizonColor = readVector3f();
            Vector3f zenithColor = readVector3f();
            float decalGlowMultiplier = readFloat();

            String albedo = readStringNull();
            String glow = readStringNull();

            // Array of Planets/Stars
            int length = readInt();
//            Planet[] planets = new Planet[length];
            for (int i = 0; i < length; i++) {
//                planets[i] = new Planet();
                Vector3f pPosition = readVector3f();
                float rotation = readFloat();
                Vector2f pScale = readVector2f();
                Vector4f uv = readVector4f();
            }

            // Mid
            Color midRgbColor = new Color(readByte(), readByte(), readByte(), 0);

            // Cirrus
            float cirrusMultiplier = readFloat();
            Vector3f cirrusColor = readVector3f();

            String cirrusTexture = readStringNull();

            int cirrusLayerCount = readInt();
//            Cirrus[] cirrusLayers = new Cirrus[cirrusLayerCount];
            for (int i = 0; i < cirrusLayerCount; i++) {
                Vector2f frequency = readVector2f();
                float speed = readFloat();
                Vector2f direction = readVector2f();
            }
            float clouds7 = readFloat();
        }

        // props
        int propCount = readInt();
        Prop[] props = new Prop[propCount];
        for (int i = 0; i < propCount; i++) {
            String path = readStringNull();
            Vector3f position = readVector3f();
            Vector3f rotation1 = readVector3f();
            Vector3f unknown = readVector3f();
            Vector3f rotation2 = readVector3f();
            Vector3f scale = readVector3f();
            float rotation = (float) StrictMath.atan2(rotation1.z, rotation1.x);
            if ((rotation - StrictMath.atan2(-rotation2.x, rotation2.z)) % (StrictMath.PI * 2) > StrictMath.PI / 180) {
                System.out.println(String.format("Prop %d: Rotation inconsistent\n", i));
            }
            props[i] = new Prop(path, position, rotation);
        }

        in.close();

        SCMap map = new SCMap(widthInt, 0, 0, 0, new Biome("loaded", mapTerrainMaterials, mapWaterSettings, mapLightingSettings));
        DataBuffer previewDataBuffer = map.getPreview().getRaster().getDataBuffer();
        for (int i = 0; i < previewDataBuffer.getSize(); i++) {
            previewDataBuffer.setElem(i, previewImageData[i]);
        }
        DataBuffer heightmapDataBuffer = map.getHeightmap().getRaster().getDataBuffer();
        for (int i = 0; i < heightmapDataBuffer.getSize(); i++) {
            heightmapDataBuffer.setElem(i, heightMapData[i]);
        }
        DataBuffer normalmapDataBuffer = map.getNormalMap().getRaster().getDataBuffer();
        for (int i = 0; i < normalmapDataBuffer.getSize(); i++) {
            try {
                normalmapDataBuffer.setElem(i, normalMapData[i]);
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
        DataBuffer textureMasksLowDataBuffer = map.getTextureMasksLow().getRaster().getDataBuffer();
        for (int i = 0; i < textureMasksLowDataBuffer.getSize(); i++) {
            textureMasksLowDataBuffer.setElem(i, textureMaskLowData[i]);
        }
        DataBuffer textureMasksHighDataBuffer = map.getTextureMasksHigh().getRaster().getDataBuffer();
        for (int i = 0; i < textureMasksHighDataBuffer.getSize(); i++) {
            textureMasksHighDataBuffer.setElem(i, textureMaskHighData[i]);
        }
        DataBuffer waterMapDataBuffer = map.getWaterMap().getRaster().getDataBuffer();
        for (int i = 0; i < waterMapDataBuffer.getSize(); i++) {
            waterMapDataBuffer.setElem(i, waterMapData[i]);
        }
        DataBuffer waterFoamMaskDataBuffer = map.getWaterFoamMask().getRaster().getDataBuffer();
        for (int i = 0; i < waterFoamMaskDataBuffer.getSize(); i++) {
            waterFoamMaskDataBuffer.setElem(i, waterFoamMaskData[i]);
        }
        DataBuffer waterFlatnessMaskDataBuffer = map.getWaterFlatnessMask().getRaster().getDataBuffer();
        for (int i = 0; i < waterFlatnessMaskDataBuffer.getSize(); i++) {
            waterFlatnessMaskDataBuffer.setElem(i, waterFlatnessData[i]);
        }
        DataBuffer waterDepthBiasMaskDataBuffer = map.getWaterDepthBiasMask().getRaster().getDataBuffer();
        for (int i = 0; i < waterDepthBiasMaskDataBuffer.getSize(); i++) {
            waterDepthBiasMaskDataBuffer.setElem(i, waterDepthBiasMaskData[i]);
        }
        DataBuffer terrainTypeMaskDataBuffer = map.getTerrainType().getRaster().getDataBuffer();
        for (int i = 0; i < terrainTypeMaskDataBuffer.getSize(); i++) {
            terrainTypeMaskDataBuffer.setElem(i, terrainTypeData[i]);
        }
        for (Prop prop : props) {
            map.addProp(prop);
        }
        for (Decal decal : decals) {
            map.addDecal(decal);
        }
        return map;
    }

    private static float readFloat() throws IOException {
        return swap(in.readFloat());
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

    private static Vector3f readVector3f() throws IOException {
        return new Vector3f(readFloat(), readFloat(), readFloat());
    }

    private static Vector4f readVector4f() throws IOException {
        return new Vector4f(readFloat(), readFloat(), readFloat(), readFloat());
    }

    private static Vector2f readVector2f() throws IOException {
        return new Vector2f(readFloat(), readFloat());
    }

}

