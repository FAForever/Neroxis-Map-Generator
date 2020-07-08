package export;

import map.SCMap;
import map.TerrainMaterials;
import util.DDSHeader;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import javax.imageio.ImageIO;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.file.Path;

import static util.Swapper.swap;

public strictfp class SCMapExporter {

    public static File file;

    public static final byte[] DDS_HEADER_1 = {68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 65, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1,
            0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final byte[] DDS_HEADER_2 = {68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 4, 0, 0, 0, 68, 88, 84, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final byte[] DDS_HEADER_3 = {68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 4, 0, 0, 0, 68, 88, 84, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};

    private static DataOutputStream out;

    public static void exportSCMAP(Path folderPath, String mapname, SCMap map) throws IOException {
        file = folderPath.resolve(mapname).resolve(mapname + ".scmap").toFile();
        boolean status = file.createNewFile();
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        DDSHeader previewDDSHeader = new DDSHeader();
        previewDDSHeader.setWidth(256);
        previewDDSHeader.setHeight(256);
        previewDDSHeader.setRGBBitCount(32);
        previewDDSHeader.setRBitMask(0x00FF0000);
        previewDDSHeader.setGBitMask(0x0000FF00);
        previewDDSHeader.setBBitMask(0x000000FF);
        previewDDSHeader.setABitMask(0xFF000000);
        byte[] previewHeaderBytes = previewDDSHeader.toBytes();

        // header
        writeInt(SCMap.SIGNATURE);
        writeInt(SCMap.VERSION_MAJOR);
        writeInt(-1091567891); // unknown
        writeInt(2); // unknown
        writeFloat(map.getSize()); // width
        writeFloat(map.getSize()); // height
        writeInt(0); // unknown
        writeShort((short) 0); // unknown
        writeInt(previewHeaderBytes.length + map.getPreview().getWidth() * map.getPreview().getHeight() * 4); // preview image byte count
        writeBytes(previewHeaderBytes);
        writeInts(((DataBufferInt) map.getPreview().getData().getDataBuffer()).getData()); // preview image data
        writeInt(SCMap.VERSION_MINOR);

        // heightmap
        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height
        writeFloat(SCMap.HEIGHTMAP_SCALE);
        writeShorts(((DataBufferUShort) map.getHeightmap().getData().getDataBuffer()).getData()); // heightmap data
        writeByte((byte) 0); // unknown

        // textures
        writeStringNull(SCMap.TERRAIN_SHADER_PATH);
        writeStringNull(SCMap.BACKGROUND_PATH);
        writeStringNull(SCMap.SKYCUBE_PATH);
        writeInt(1); // cubemap count
        writeStringNull(SCMap.CUBEMAP_NAME);
        writeStringNull(SCMap.CUBEMAP_PATH);

        // lighting
        LightingSettings mapLightingSettings = map.biome.getLightingSettings();
        writeFloat(mapLightingSettings.LightingMultiplier);
        writeVector3f(mapLightingSettings.SunDirection);
        writeVector3f(mapLightingSettings.SunAmbience);
        writeVector3f(mapLightingSettings.SunColor);
        writeVector3f(mapLightingSettings.ShadowFillColor);
        writeVector4f(mapLightingSettings.SpecularColor);
        writeFloat(mapLightingSettings.Bloom);
        writeVector3f(mapLightingSettings.FogColor);
        writeFloat(mapLightingSettings.FogStart);
        writeFloat(mapLightingSettings.FogEnd);

        // water
        WaterSettings mapWaterSettings = map.biome.getWaterSettings();
        writeByte((byte) (mapWaterSettings.HasWater ? 1 : 0));
        writeFloat(mapWaterSettings.Elevation);
        writeFloat(mapWaterSettings.ElevationDeep);
        writeFloat(mapWaterSettings.ElevationAbyss);
        writeVector3f(mapWaterSettings.SurfaceColor);
        writeVector2f(mapWaterSettings.ColorLerp);
        writeFloat(mapWaterSettings.RefractionScale);
        writeFloat(mapWaterSettings.FresnelBias);
        writeFloat(mapWaterSettings.FresnelPower);
        writeFloat(mapWaterSettings.UnitReflection);
        writeFloat(mapWaterSettings.SkyReflection);
        writeFloat(mapWaterSettings.SunShininess);
        writeFloat(mapWaterSettings.SunStrength);
        writeVector3f(mapWaterSettings.SunDirection);
        writeVector3f(mapWaterSettings.SunColor);
        writeFloat(mapWaterSettings.SunReflection);
        writeFloat(mapWaterSettings.SunGlow);
        writeStringNull(mapWaterSettings.TexPathCubemap);
        writeStringNull(mapWaterSettings.TexPathWaterRamp);

        // waves
        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            writeFloat(mapWaterSettings.WaveTextures[i].NormalRepeat);
        }

        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            writeVector2f(mapWaterSettings.WaveTextures[i].NormalMovement);
            writeStringNull(mapWaterSettings.WaveTextures[i].TexPath);
        }

        // wave generators
        writeInt(0); // wave generator count

        // terrain textures
        TerrainMaterials mapTerrainMaterials = map.biome.getTerrainMaterials();
        for (int i = 0; i < 24; i++) {
            writeByte((byte) 0); // unknown
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.texturePaths[i]);
            writeFloat(mapTerrainMaterials.textureScales[i]);
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.normalPaths[i]);
            writeFloat(mapTerrainMaterials.normalScales[i]);
        }

        writeInt(0); // unknown
        writeInt(0); // unknown

        // decals
        writeInt(0); // decal count
        writeInt(0); // decal group count
        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height

        DDSHeader normalDDSHeader = new DDSHeader();
        normalDDSHeader.setWidth(map.getNormalMap().getWidth());
        normalDDSHeader.setHeight(map.getNormalMap().getHeight());
        normalDDSHeader.setFourCC("DXT5");
        byte[] normalHeaderBytes = normalDDSHeader.toBytes();

        // normal maps
        writeInt(1); // normal map count
        writeInt(normalHeaderBytes.length + map.getNormalMap().getWidth() * map.getNormalMap().getHeight() * 4); // normalmap byte count
        writeBytes(normalHeaderBytes); // dds header
        writeInts(((DataBufferInt) map.getNormalMap().getData().getDataBuffer()).getData()); // normalmap data

        DDSHeader textureMaskDDSHeader = new DDSHeader();
        textureMaskDDSHeader.setWidth(map.getTextureMasksLow().getWidth());
        textureMaskDDSHeader.setHeight(map.getTextureMasksLow().getHeight());
        textureMaskDDSHeader.setRGBBitCount(32);
        textureMaskDDSHeader.setRBitMask(0x00FF0000);
        textureMaskDDSHeader.setGBitMask(0x0000FF00);
        textureMaskDDSHeader.setBBitMask(0x000000FF);
        textureMaskDDSHeader.setABitMask(0xFF000000);
        byte[] textureHeaderBytes = textureMaskDDSHeader.toBytes();

        // texture maps
        writeInt(textureHeaderBytes.length + map.getTextureMasksLow().getWidth() * map.getTextureMasksLow().getHeight() * 4); // texture masks low byte count
        writeBytes(textureHeaderBytes); // dds header
        writeInts(((DataBufferInt) map.getTextureMasksLow().getData().getDataBuffer()).getData()); // texture masks low data
        writeInt(textureHeaderBytes.length + map.getTextureMasksHigh().getWidth() * map.getTextureMasksHigh().getHeight() * 4); // texture maks high byte count
        writeBytes(textureHeaderBytes); // dds header
        writeInts(((DataBufferInt) map.getTextureMasksHigh().getData().getDataBuffer()).getData()); // texture masks high data

        DDSHeader waterDDSHeader = new DDSHeader();
        waterDDSHeader.setWidth(map.getWaterMap().getWidth());
        waterDDSHeader.setHeight(map.getWaterMap().getHeight());
        waterDDSHeader.setFourCC("DXT5");
        byte[] waterHeaderBytes = waterDDSHeader.toBytes();

        // water maps
        writeInt(1); // unknown
        writeInt(waterHeaderBytes.length + map.getWaterMap().getWidth() * map.getWaterMap().getHeight()); // watermap byte count
        writeBytes(waterHeaderBytes); // dds header
        writeBytes(((DataBufferByte) map.getWaterMap().getData().getDataBuffer()).getData()); // watermap data
        writeBytes(((DataBufferByte) map.getWaterFoamMask().getData().getDataBuffer()).getData()); // water foam mask data
        writeBytes(((DataBufferByte) map.getWaterFlatnessMask().getData().getDataBuffer()).getData()); // water flatness mask data
        writeBytes(((DataBufferByte) map.getWaterDepthBiasMask().getData().getDataBuffer()).getData()); // water depth bias mask data

        // terrain type
        writeInts(((DataBufferInt) map.getTerrainType().getData().getDataBuffer()).getData()); // terrain type data

        // props
        writeInt(map.getPropCount());
        for (int i = 0; i < map.getPropCount(); i++) {
            writeStringNull(map.getProp(i).getPath());
            writeVector3f(map.getProp(i).getPosition());
            writeVector3f(new Vector3f((float) StrictMath.sin(map.getProp(i).getRotation()), 0f, (float) StrictMath.cos(map.getProp(i).getRotation())));
            writeVector3f(new Vector3f(0f, 1f, 0f));
            writeVector3f(new Vector3f((float) -StrictMath.cos(map.getProp(i).getRotation()), 0f, (float) StrictMath.sin(map.getProp(i).getRotation())));
            writeVector3f(new Vector3f(1f, 1f, 1f)); //scale
        }

        out.flush();
        out.close();

        final String fileFormat = "png";
        File previewFile = folderPath.resolve(mapname).resolve(mapname + "_preview." + fileFormat).toFile();
        RenderedImage renderedImage = map.getPreview();
        try {
            ImageIO.write(renderedImage, fileFormat, previewFile);
        } catch (IOException e) {
            System.out.print("Could not write the preview image\n" + e.toString());
        }
    }

    private static void writeFloat(float f) throws IOException {
        out.writeFloat(swap(f));
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

    private static void writeVector3f(Vector3f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
        writeFloat(v.z);
    }

    private static void writeVector4f(Vector4f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
        writeFloat(v.z);
        writeFloat(v.w);
    }

    private static void writeVector2f(Vector2f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
    }

}
