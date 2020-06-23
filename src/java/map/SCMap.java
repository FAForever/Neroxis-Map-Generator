package map;

import biomes.Biome;

import lombok.Getter;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

@Getter
public strictfp class SCMap {

	public static final int SIGNATURE = 443572557;
	public static final int VERSION_MAJOR = 2;
	public static final int VERSION_MINOR = 56;

	public static final float HEIGHTMAP_SCALE = 1f / 128f;
	public static final String TERRAIN_SHADER_PATH = "TTerrain";
	public static final String BACKGROUND_PATH = "/textures/environment/defaultbackground.dds";
	public static final String SKYCUBE_PATH = "/textures/environment/defaultskycube.dds";
	public static final String CUBEMAP_NAME = "<default>";
	public static final String CUBEMAP_PATH = "/textures/environment/defaultenvcube.dds";
	public static final float LIGHTING_MULTIPLIER = 1.5f;
	public static final Vector3f SUN_DIRECTION = new Vector3f(0.707f, 0.707f, 0f);
	public static final Vector3f SUN_AMBIANCE_COLOR = new Vector3f(0.2f, 0.2f, 0.2f);
	public static final Vector3f SUN_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
	public static final Vector3f SHADOW_COLOR = new Vector3f(0.7f, 0.7f, 0.75f);
	public static final Vector4f SPECULAR_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
	public static final float BLOOM = 0.08f;
	public static final Vector3f FOG_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
	public static final float FOG_START = 0f;
	public static final float FOG_END = 1000f;

	public static final float WATER_HEIGHT = 25f;
	public static final float WATER_DEEP_HEIGHT = 20f;
	public static final float WATER_ABYSS_HEIGHT = 10f;
	public static final Vector3f WATER_SURFACE_COLOR = new Vector3f(0.0f, 0.7f, 1.5f);
	public static final Vector2f WATER_COLOR_LERP = new Vector2f(0.064f, 0.119f);
	public static final float WATER_REFRACTION = 0.375f;
	public static final float WATER_FRESNEL_BIAS = 0.15f;
	public static final float WATER_FRESNEL_POWER = 1.5f;
	public static final float WATER_UNIT_REFLECTION = 0.5f;
	public static final float WATER_SKY_REFLECTION = 1.5f;
	public static final float WATER_SUN_SHININESS = 50f;
	public static final float WATER_SUN_STRENGH = 10f;
	public static final Vector3f WATER_SUN_DIRECTION = new Vector3f(0.09954818f, -0.9626309f, 0.2518569f); // why != SUN_DIRECTION
	public static final Vector3f WATER_SUN_COLOR = new Vector3f(0.81274265f, 0.47409984f, 0.33864275f);
	public static final float WATER_SUN_REFLECTION = 5f;
	public static final float WATER_SUN_GLOW = 0.1f;
	public static final String WATER_CUBEMAP_PATH = "/textures/engine/waterCubemap.dds";
	public static final String WATER_RAMP_PATH = "/textures/engine/waterramp.dds";

	public static final int WAVE_NORMAL_COUNT = 4;
	public static final float[] WAVE_NORMAL_REPEATS = { 0.0009f, 0.009f, 0.05f, 0.5f };
	public static final Vector2f[] WAVE_NORMAL_MOVEMENTS = { new Vector2f(0.5f, -0.95f), new Vector2f(0.05f, -0.095f), new Vector2f(0.01f, 0.03f), new Vector2f(0.0005f, 0.0009f) };
	public static final String[] WAVE_TEXTURE_PATHS = { "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds" }; // always same?

	public final Biome biome;

	private final int size; // must be a power of 2. 512 equals a 10x10km Map

	private static final String DEFAULT_ENVIRONMENT ="evergreen";
	private final Vector3f[] spawns;
	private final Vector3f[] mexes;
	private final Vector3f[] hydros;
	private final ArrayList<Prop> props;
	private final ArrayList<Unit> units;
	private final ArrayList<Unit> wrecks;

	private final BufferedImage preview;
	private final BufferedImage heightmap;
	private final BufferedImage normalMap;
	private final BufferedImage textureMasksLow;
	private final BufferedImage textureMasksHigh;

	private final BufferedImage waterMap;
	private final BufferedImage waterFoamMask;
	private final BufferedImage waterFlatnessMask;
	private final BufferedImage waterDepthBiasMask;
	private final BufferedImage terrainType;

	public SCMap(int size, int spawnCount, int mexCount, int hydroCount) {
		this.size = size;
		spawns = new Vector3f[spawnCount];
		mexes = new Vector3f[mexCount];
		hydros = new Vector3f[hydroCount];
		props = new ArrayList<>();
		units = new ArrayList<>();
		wrecks = new ArrayList<>();

		preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);// always 256 x 256 px
		heightmap = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_USHORT_GRAY);
		normalMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);
		textureMasksLow = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);
		textureMasksHigh = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);

		waterMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
		waterFoamMask = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
		waterFlatnessMask = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
		for (int y = 0; y < size / 2; y++) {
			for (int x = 0; x < size / 2; x++) {
				waterFlatnessMask.getRaster().setPixel(x, y, new int[] { 255 });
			}
		}
		waterDepthBiasMask = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
		terrainType = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);

		biome = new Biome(
				"Default",
				new TerrainMaterials(
						new Material[]{
								new Material(DEFAULT_ENVIRONMENT, "SandLight", 4f),
								new Material(DEFAULT_ENVIRONMENT, "grass001", 4f),
								new Material(DEFAULT_ENVIRONMENT, "Dirt001", 4f),
								new Material(DEFAULT_ENVIRONMENT, "RockMed", 4f),
								new Material(DEFAULT_ENVIRONMENT, "snow001", 4f)
						},
						new Material(DEFAULT_ENVIRONMENT, "macrotexture000", 128f)
				),
				new WaterSettings(),
				new LightingSettings()
		);
	}

	public int getSize() {
		return size;
	}

	public Vector3f[] getSpawns() {
		return spawns;
	}

	public Vector3f[] getMexes() {
		return mexes;
	}

	public Vector3f[] getHydros() {
		return hydros;
	}
	
	public int getPropCount() {
		return props.size();
	}
	
	public Prop getProp(int i){
		return props.get(i);
	}
	
	public void addProp(Prop prop){
		props.add(prop);
	}

	public int getUnitCount() {
		return units.size();
	}

	public Unit getUnit(int i){
		return units.get(i);
	}

	public void addUnit(Unit unit){
		units.add(unit);
	}

	public int getWreckCount() {
		return wrecks.size();
	}

	public Unit getWreck(int i){
		return wrecks.get(i);
	}

	public void addWreck(Unit wreck){
		wrecks.add(wreck);
	}

	public void setHeightmap(FloatMask heightmap) {
		for (int y = 0; y < size + 1; y++) {
			for (int x = 0; x < size + 1; x++) {
				this.heightmap.getRaster().setPixel(x, y, new int[] { (short) (heightmap.get(x, y) / HEIGHTMAP_SCALE) });
			}
		}
	}

	public void setTextureMaskLow(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
		for (int y = 0; y < size / 2; y++) {
			for (int x = 0; x < size / 2; x++) {
				textureMasksLow.getRaster().setPixel(x, y, new int[] { (byte) (mask0.get(x, y) * 256f), (byte) (mask1.get(x, y) * 256f), (byte) (mask2.get(x, y) * 256f), (byte) (mask3.get(x, y) * 256f) });
			}
		}
	}

}
