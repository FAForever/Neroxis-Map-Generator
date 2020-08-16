package export;

import generator.Preview;
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
        writeFloat(mapLightingSettings.getLightingMultiplier());
        writeVector3f(mapLightingSettings.getSunDirection());
        writeVector3f(mapLightingSettings.getSunAmbience());
        writeVector3f(mapLightingSettings.getSunColor());
        writeVector3f(mapLightingSettings.getShadowFillColor());
        writeVector4f(mapLightingSettings.getSpecularColor());
        writeFloat(mapLightingSettings.getBloom());
        writeVector3f(mapLightingSettings.getFogColor());
        writeFloat(mapLightingSettings.getFogStart());
        writeFloat(mapLightingSettings.getFogEnd());

        // water
        WaterSettings mapWaterSettings = map.biome.getWaterSettings();
        writeByte((byte) (mapWaterSettings.isWaterPresent() ? 1 : 0));
        writeFloat(mapWaterSettings.getElevation());
        writeFloat(mapWaterSettings.getElevationDeep());
        writeFloat(mapWaterSettings.getElevationAbyss());
        writeVector3f(mapWaterSettings.getSurfaceColor());
        writeVector2f(mapWaterSettings.getColorLerp());
        writeFloat(mapWaterSettings.getRefractionScale());
        writeFloat(mapWaterSettings.getFresnelBias());
        writeFloat(mapWaterSettings.getFresnelPower());
        writeFloat(mapWaterSettings.getUnitReflection());
        writeFloat(mapWaterSettings.getSkyReflection());
        writeFloat(mapWaterSettings.getSunShininess());
        writeFloat(mapWaterSettings.getSunStrength());
        writeVector3f(mapWaterSettings.getSunDirection());
        writeVector3f(mapWaterSettings.getSunColor());
        writeFloat(mapWaterSettings.getSunReflection());
        writeFloat(mapWaterSettings.getSunGlow());
        writeStringNull(mapWaterSettings.getTexPathCubemap());
        writeStringNull(mapWaterSettings.getTexPathWaterRamp());

        // waves
        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            writeFloat(mapWaterSettings.getWaveTextures()[i].getNormalRepeat());
        }

        for (int i = 0; i < SCMap.WAVE_NORMAL_COUNT; i++) {
            writeVector2f(mapWaterSettings.getWaveTextures()[i].getNormalMovement());
            writeStringNull(mapWaterSettings.getWaveTextures()[i].getTexPath());
        }

        // wave generators
        writeInt(0); // wave generator count

        // terrain textures
        TerrainMaterials mapTerrainMaterials = map.biome.getTerrainMaterials();
        for (int i = 0; i < 24; i++) {
            writeByte((byte) 0); // unknown
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
        writeInt(map.getDecalCount());
        for (int i = 0; i < map.getDecalCount(); i++) {
            writeInt(i);
            writeInt(map.getDecal(i).getType());
            writeInt(2);
            writeString(map.getDecal(i).getPath());
            writeString("");
            writeVector3f(map.getDecal(i).getScale());
            writeVector3f(map.getDecal(i).getPosition());
            writeVector3f(new Vector3f(0f, map.getDecal(i).getRotation(), 0f));
            writeFloat(map.getDecal(i).getCutOffLOD());
            writeFloat(0);
            writeInt(-1);
        }

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
        RenderedImage renderedImage = Preview.addMarkers(map.getPreview(), map);
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

    private static void writeString(String s) throws IOException {
        writeInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.writeByte(s.charAt(i));
        }
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
