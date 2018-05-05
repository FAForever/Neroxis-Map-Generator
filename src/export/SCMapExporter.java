package export;

import static util.Swapper.swap;

import java.io.*;
import java.awt.image.*;

import map.*;
import util.*;

public strictfp class SCMapExporter {

	public static final byte[] DDS_HEADER_1 = { 68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 65, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1,
			2, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public static final byte[] DDS_HEADER_2 = { 68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 4, 0, 0, 0, 68, 88, 84, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public static final byte[] DDS_HEADER_3 = { 68, 68, 83, 32, 124, 0, 0, 0, 7, 16, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 4, 0, 0, 0, 68, 88, 84, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };

	private static DataOutputStream out;

	public static void exportSCMAP(String folderPath, String mapname, SCMap map) throws IOException {
		File file = new File(folderPath + mapname + File.separator + mapname + ".scmap");
		file.createNewFile();
		out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

		// header
		writeInt(map.SIGNATURE);
		writeInt(map.VERSION_MAJOR);
		writeInt(-1091567891); // unknown
		writeInt(2); // unknown
		writeFloat(map.getSize()); // width
		writeFloat(map.getSize()); // height
		writeInt(0); // unknown
		writeShort((short) 0); // unknown
		writeInt(DDS_HEADER_1.length + map.getPreview().getWidth() * map.getPreview().getHeight() * 4); // preview image byte count
		writeBytes(DDS_HEADER_1);
		writeInts(((DataBufferInt) map.getPreview().getData().getDataBuffer()).getData()); // preview image data
		writeInt(map.VERSION_MINOR);

		// heightmap
		writeInt(map.getSize()); // width
		writeInt(map.getSize()); // height
		writeFloat(map.HEIGHTMAP_SCALE);
		writeShorts(((DataBufferUShort) map.getHeightmap().getData().getDataBuffer()).getData()); // heightmap data
		writeByte((byte) 0); // unknown

		// textures
		writeStringNull(map.TERRAIN_SHADER_PATH);
		writeStringNull(map.BACKGROUND_PATH);
		writeStringNull(map.SKYCUBE_PATH);
		writeInt(1); // cubemap count
		writeStringNull(map.CUBEMAP_NAME);
		writeStringNull(map.CUBEMAP_PATH);
		writeFloat(map.LIGHTING_MULTIPLIER);
		writeVector3f(map.SUN_DIRECTION);
		writeVector3f(map.SUN_AMBIANCE_COLOR);
		writeVector3f(map.SUN_COLOR);
		writeVector3f(map.SHADOW_COLOR);
		writeVector4f(map.SPECULAR_COLOR);
		writeFloat(map.BLOOM);
		writeVector3f(map.FOG_COLOR);
		writeFloat(map.FOG_START);
		writeFloat(map.FOG_END);

		// water
		writeByte((byte) 1); // map has water
		writeFloat(map.WATER_HEIGHT);
		writeFloat(map.WATER_DEEP_HEIGHT);
		writeFloat(map.WATER_ABYSS_HEIGHT);
		writeVector3f(map.WATER_SURFACE_COLOR);
		writeVector2f(map.WATER_COLOR_LERP);
		writeFloat(map.WATER_REFRACTION);
		writeFloat(map.WATER_FRESNEL_BIAS);
		writeFloat(map.WATER_FRESNEL_POWER);
		writeFloat(map.WATER_UNIT_REFLECTION);
		writeFloat(map.WATER_SKY_REFLECTION);
		writeFloat(map.WATER_SUN_SHININESS);
		writeFloat(map.WATER_SUN_STRENGH);
		writeVector3f(map.WATER_SUN_DIRECTION);
		writeVector3f(map.WATER_SUN_COLOR);
		writeFloat(map.WATER_SUN_REFLECTION);
		writeFloat(map.WATER_SUN_GLOW);
		writeStringNull(map.WATER_CUBEMAP_PATH);
		writeStringNull(map.WATER_RAMP_PATH);

		// waves
		for (int i = 0; i < map.WAVE_NORMAL_COUNT; i++) {
			writeFloat(map.WAVE_NORMAL_REPEATS[i]);
		}
		for (int i = 0; i < map.WAVE_NORMAL_COUNT; i++) {
			writeVector2f(map.WAVE_NORMAL_MOVEMENTS[i]);
			writeStringNull(map.WAVE_TEXTURE_PATHS[i]);
		}

		// wave generators
		writeInt(0); // wave generator count

		// terrain textures
		for (int i = 0; i < 24; i++) {
			writeByte((byte) 0); // unknown
		}
		for (int i = 0; i < map.TERRAIN_TEXTURE_COUNT; i++) {
			writeStringNull(map.TERRAIN_TEXTURE_PATHS[i]);
			writeFloat(map.TERRAIN_TEXTURE_SCALES[i]);
		}
		for (int i = 0; i < map.TERRAIN_NORMALS_COUNT; i++) {
			writeStringNull(map.TERRAIN_NORMALS_PATHS[i]);
			writeFloat(map.TERRAIN_NORMALS_SCALES[i]);
		}

		writeInt(0); // unknown
		writeInt(0); // unknown

		// decals
		writeInt(0); // decal count
		writeInt(0); // decal group count
		writeInt(map.getSize()); // width
		writeInt(map.getSize()); // height

		// normal maps
		writeInt(1); // normal map count
		writeInt(DDS_HEADER_2.length + map.getNormalMap().getWidth() * map.getNormalMap().getHeight() * 4); // normalmap byte count
		writeBytes(DDS_HEADER_2); // dds header
		writeInts(((DataBufferInt) map.getNormalMap().getData().getDataBuffer()).getData()); // normalmap data

		// texture maps
		writeInt(DDS_HEADER_1.length + map.getTextureMasksLow().getWidth() * map.getTextureMasksLow().getHeight() * 4); // texture masks low byte count
		writeBytes(DDS_HEADER_1); // dds header
		writeInts(((DataBufferInt) map.getTextureMasksLow().getData().getDataBuffer()).getData()); // texture masks low data
		writeInt(DDS_HEADER_1.length + map.getTextureMasksHigh().getWidth() * map.getTextureMasksHigh().getHeight() * 4); // texture maks high byte count
		writeBytes(DDS_HEADER_1); // dds header
		writeInts(((DataBufferInt) map.getTextureMasksHigh().getData().getDataBuffer()).getData()); // texture masks high data

		// water maps
		writeInt(1); // unknown
		writeInt(DDS_HEADER_3.length + map.getWaterMap().getWidth() * map.getWaterMap().getHeight()); // watermap byte count
		writeBytes(DDS_HEADER_3); // dds header
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
			writeVector3f(new Vector3f((float) StrictMath.sin(map.getProp(i).getRotation() * 2 * StrictMath.PI), 0f, (float) StrictMath.cos(map.getProp(i).getRotation() * 2 * StrictMath.PI)));
			writeVector3f(new Vector3f(0f, 1f, 0f));
			writeVector3f(new Vector3f((float) -StrictMath.cos(map.getProp(i).getRotation() * 2 * StrictMath.PI), 0f, (float) StrictMath.sin(map.getProp(i).getRotation() * 2 * StrictMath.PI)));
			writeVector3f(new Vector3f(1f, 1f, 1f)); //scale
		}

		out.flush();
		out.close();
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
		for (int i = 0; i < s.length; i++) {
			writeShort(s[i]);
		}
	}

	private static void writeInts(int[] data) throws IOException {
		for (int i = 0; i < data.length; i++) {
			writeInt(data[i]);
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
