package loader;

import map.*;
import util.DDSHeader;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.image.DataBuffer;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static util.Swapper.swap;

public strictfp class SCMapLoader {

    public static File file;

    private static DataInputStream in;

    public static void main(String[] args) throws IOException {
        SCMap map = loadSCMAP(Paths.get(args[0]));
    }

    public static SCMap loadSCMAP(Path filePath) throws IOException {
        file = filePath.toFile();
        boolean status = file.createNewFile();
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
        if (readInt() != SCMap.VERSION_MINOR) {
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
        if (!readStringNull().equals(SCMap.TERRAIN_SHADER_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (!readStringNull().equals(SCMap.BACKGROUND_PATH)) {
//            throw new UnsupportedEncodingException("File not valid SCMap");
        }
        if (!readStringNull().equals(SCMap.SKYCUBE_PATH)) {
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
        mapLightingSettings.LightingMultiplier = readFloat();
        mapLightingSettings.SunDirection = readVector3f();
        mapLightingSettings.SunAmbience = readVector3f();
        mapLightingSettings.SunColor = readVector3f();
        mapLightingSettings.ShadowFillColor = readVector3f();
        mapLightingSettings.SpecularColor = readVector4f();
        mapLightingSettings.Bloom = readFloat();
        mapLightingSettings.FogColor = readVector3f();
        mapLightingSettings.FogStart = readFloat();
        mapLightingSettings.FogEnd = readFloat();

        // water
        WaterSettings mapWaterSettings = new WaterSettings();
        mapWaterSettings.HasWater = readByte() == 1;
        mapWaterSettings.Elevation = readFloat();
        mapWaterSettings.ElevationDeep = readFloat();
        mapWaterSettings.ElevationAbyss = readFloat();
        mapWaterSettings.SurfaceColor = readVector3f();
        mapWaterSettings.ColorLerp = readVector2f();
        mapWaterSettings.RefractionScale = readFloat();
        mapWaterSettings.FresnelBias = readFloat();
        mapWaterSettings.FresnelPower = readFloat();
        mapWaterSettings.UnitReflection = readFloat();
        mapWaterSettings.SkyReflection = readFloat();
        mapWaterSettings.SunShininess = readFloat();
        mapWaterSettings.SunStrength = readFloat();
        mapWaterSettings.SunDirection = readVector3f();
        mapWaterSettings.SunColor = readVector3f();
        mapWaterSettings.SunReflection = readFloat();
        mapWaterSettings.SunGlow = readFloat();
        mapWaterSettings.TexPathCubemap = readStringNull();
        mapWaterSettings.TexPathWaterRamp = readStringNull();

        // waves
        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            mapWaterSettings.WaveTextures[i].NormalRepeat = readFloat();
        }

        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            mapWaterSettings.WaveTextures[i].NormalMovement = readVector2f();
            mapWaterSettings.WaveTextures[i].TexPath = readStringNull();
        }

        // wave generators
        if (readInt() != 0) {
            throw new UnsupportedEncodingException("File not valid Generated SCMap");
        }

        // terrain textures
        TerrainMaterials mapTerrainMaterials = new TerrainMaterials(new Material[0], new Material("", "", 0));
        for (int i = 0; i < 24; i++) {
            if (readByte() != 0) {
                throw new UnsupportedEncodingException("File not valid SCMap");
            }
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            mapTerrainMaterials.texturePaths[i] = readStringNull();
            mapTerrainMaterials.textureScales[i] = readFloat();
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            mapTerrainMaterials.normalPaths[i] = readStringNull();
            mapTerrainMaterials.normalScales[i] = readFloat();
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
            if (rotation != (float) StrictMath.atan2(-rotation2.x, rotation2.z)) {
                System.out.println(String.format("Prop %d: Rotation inconsistent\n",  i));
            }
            props[i] = new Prop(path, position, rotation);
        }

        in.close();

        SCMap map = new SCMap(widthInt,0,0,0);
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
            normalmapDataBuffer.setElem(i, normalMapData[i]);
        }
        DataBuffer textureMasksLowDataBuffer = map.getTextureMasksLow().getRaster().getDataBuffer();
        for (int i = 0; i < previewDataBuffer.getSize(); i++) {
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
        map.getBiome().setWaterSettings(mapWaterSettings);
        map.getBiome().setLightingSettings(mapLightingSettings);
        for (Prop prop : props) {
            map.addProp(prop);
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
        for (int i = 0; i < numBytes; i ++) {
            readBytes[i] = readByte();
        }
        return readBytes;
    }

    private static short[] readShorts(int numShorts) throws IOException {
        short[] readShorts = new short[numShorts];
        for (int i = 0; i < numShorts; i ++) {
            readShorts[i] = readShort();
        }
        return readShorts;
    }

    private static int[] readInts(int numInts) throws IOException {
        int[] readInts = new int[numInts];
        for (int i = 0; i < numInts; i ++) {
            readInts[i] = readInt();
        }
        return readInts;
    }

    private static String readStringNull() throws IOException {
        StringBuilder readString = new StringBuilder();
        byte read = readByte();
        while (read != 0){
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

