package map;

import biomes.Biome;
import lombok.Data;
import lombok.SneakyThrows;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Data
public strictfp class SCMap {

    public static final int SIGNATURE = 443572557;
    public static final int VERSION_MAJOR = 2;
    public static final int VERSION_MINOR = 56;

    public static final float HEIGHTMAP_SCALE = 1f / 128f;
    public static final String TERRAIN_SHADER_PATH = "TTerrainXP";
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
    public static final float[] WAVE_NORMAL_REPEATS = {0.0009f, 0.009f, 0.05f, 0.5f};
    public static final Vector2f[] WAVE_NORMAL_MOVEMENTS = {new Vector2f(0.5f, -0.95f), new Vector2f(0.05f, -0.095f), new Vector2f(0.01f, 0.03f), new Vector2f(0.0005f, 0.0009f)};
    public static final String[] WAVE_TEXTURE_PATHS = {"/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds"}; // always same?
    private static final String DEFAULT_ENVIRONMENT = "evergreen";
    private final int size; // must be a power of 2. 512 equals a 10x10km Map
    public Biome biome;
    private int spawnCountInit;
    private int mexCountInit;
    private int hydroCountInit;
    private final ArrayList<Vector3f> spawns;
    private final ArrayList<Vector3f> mexes;
    private final ArrayList<Vector3f> hydros;
    private final ArrayList<Decal> decals;
    private final ArrayList<Prop> props;
    private final ArrayList<Unit> units;
    private final ArrayList<Unit> wrecks;
    private final ArrayList<AIMarker> landAIMarkers;
    private final ArrayList<AIMarker> amphibiousAIMarkers;
    private final ArrayList<AIMarker> navyAIMarkers;
    private final ArrayList<AIMarker> airAIMarkers;
    private final ArrayList<AIMarker> rallyMarkers;
    private final ArrayList<AIMarker> expansionAIMarkers;
    private final ArrayList<AIMarker> largeExpansionAIMarkers;
    private final ArrayList<AIMarker> navalAreaAIMarkers;
    private final ArrayList<AIMarker> navalRallyMarkers;


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

    public SCMap(int size, int spawnCount, int mexCount, int hydroCount, Biome biome) {
        this.size = size;
        this.biome = biome;
        this.spawnCountInit = spawnCount;
        this.mexCountInit = mexCount;
        this.hydroCountInit = hydroCount;
        spawns = new ArrayList<>();
        mexes = new ArrayList<>();
        hydros = new ArrayList<>();
        decals = new ArrayList<>();
        props = new ArrayList<>();
        units = new ArrayList<>();
        wrecks = new ArrayList<>();
        landAIMarkers = new ArrayList<>();
        amphibiousAIMarkers = new ArrayList<>();
        navyAIMarkers = new ArrayList<>();
        airAIMarkers = new ArrayList<>();
        rallyMarkers = new ArrayList<>();
        expansionAIMarkers = new ArrayList<>();
        largeExpansionAIMarkers = new ArrayList<>();
        navalAreaAIMarkers = new ArrayList<>();
        navalRallyMarkers = new ArrayList<>();


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
                waterFlatnessMask.getRaster().setPixel(x, y, new int[]{255});
            }
        }
        waterDepthBiasMask = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
        terrainType = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);
    }

    public int getSpawnCount() {
        return spawns.size();
    }

    public Vector3f getSpawn(int i) {
        return spawns.get(i);
    }

    public void addSpawn(Vector3f spawn) {
        spawns.add(spawn);
    }

    public int getMexCount() {
        return mexes.size();
    }

    public Vector3f getMex(int i) {
        return mexes.get(i);
    }

    public void addMex(Vector3f mex) {
        mexes.add(mex);
    }

    public int getHydroCount() {
        return hydros.size();
    }

    public Vector3f getHydro(int i) {
        return hydros.get(i);
    }

    public void addHydro(Vector3f hydro) {
        hydros.add(hydro);
    }

    public int getDecalCount() {
        return decals.size();
    }

    public Decal getDecal(int i) {
        return decals.get(i);
    }

    public void addDecal(Decal decal) {
        decals.add(decal);
    }

    public int getPropCount() {
        return props.size();
    }

    public Prop getProp(int i) {
        return props.get(i);
    }

    public void addProp(Prop prop) {
        props.add(prop);
    }

    public int getUnitCount() {
        return units.size();
    }

    public Unit getUnit(int i) {
        return units.get(i);
    }

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public int getWreckCount() {
        return wrecks.size();
    }

    public Unit getWreck(int i) {
        return wrecks.get(i);
    }

    public void addWreck(Unit wreck) {
        wrecks.add(wreck);
    }

    public int getLandMarkerCount() {
        return landAIMarkers.size();
    }

    public AIMarker getLandMarker(int i) {
        return landAIMarkers.get(i);
    }

    public void addLandMarker(AIMarker aiMarker) {
        landAIMarkers.add(aiMarker);
    }

    public int getAmphibiousMarkerCount() {
        return amphibiousAIMarkers.size();
    }

    public AIMarker getAmphibiousMarker(int i) {
        return amphibiousAIMarkers.get(i);
    }

    public void addAmphibiousMarker(AIMarker aiMarker) {
        amphibiousAIMarkers.add(aiMarker);
    }

    public int getNavyMarkerCount() {
        return navyAIMarkers.size();
    }

    public AIMarker getNavyMarker(int i) {
        return navyAIMarkers.get(i);
    }

    public void addNavyMarker(AIMarker aiMarker) {
        navyAIMarkers.add(aiMarker);
    }

    public int getAirMarkerCount() {
        return airAIMarkers.size();
    }

    public AIMarker getAirMarker(int i) {
        return airAIMarkers.get(i);
    }

    public void addAirMarker(AIMarker aiMarker) {
        airAIMarkers.add(aiMarker);
    }

    public int getRallyMarkerCount() {
        return rallyMarkers.size();
    }

    public AIMarker getRallyMarker(int i) {
        return rallyMarkers.get(i);
    }

    public void addRallyMarker(AIMarker aiMarker) {
        rallyMarkers.add(aiMarker);
    }

    public int getExpansionMarkerCount() {
        return expansionAIMarkers.size();
    }

    public AIMarker getExpansionMarker(int i) {
        return expansionAIMarkers.get(i);
    }

    public void addExpansionMarker(AIMarker aiMarker) {
        expansionAIMarkers.add(aiMarker);
    }

    public int getLargeExpansionMarkerCount() {
        return largeExpansionAIMarkers.size();
    }

    public AIMarker getLargeExpansionMarker(int i) {
        return largeExpansionAIMarkers.get(i);
    }

    public void addLargeExpansionMarker(AIMarker aiMarker) {
        largeExpansionAIMarkers.add(aiMarker);
    }

    public int getNavalAreaMarkerCount() {
        return navalAreaAIMarkers.size();
    }

    public AIMarker getNavalAreaMarker(int i) {
        return navalAreaAIMarkers.get(i);
    }

    public void addNavalAreaMarker(AIMarker aiMarker) {
        navalAreaAIMarkers.add(aiMarker);
    }

    public int getNavyRallyMarkerCount() {
        return navalRallyMarkers.size();
    }

    public AIMarker getNavyRallyMarker(int i) {
        return navalRallyMarkers.get(i);
    }

    public void addNavyRallyMarker(AIMarker aiMarker) {
        navalRallyMarkers.add(aiMarker);
    }

    public void setHeightmap(FloatMask heightmap) {
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                this.heightmap.getRaster().setPixel(x, y, new int[]{(short) (heightmap.get(x, y) / HEIGHTMAP_SCALE)});
            }
        }
    }

    public FloatMask getHeightMask(SymmetryHierarchy symmetryHierarchy) {
        FloatMask heightMask = new FloatMask(this.heightmap.getHeight(), null, symmetryHierarchy);
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                heightMask.getMask()[x][y] = this.heightmap.getRaster().getPixel(x, y, new int[1])[0] * HEIGHTMAP_SCALE;
            }
        }
        return heightMask;
    }

    public void setTextureMasksLow(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < size / 2; y++) {
            for (int x = 0; x < size / 2; x++) {
                textureMasksLow.getRaster().setPixel(x, y, new int[]{(int) (StrictMath.min(1f, mask0.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask1.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask2.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask3.get(x, y)) * 255f)});
            }
        }
    }

    public void setTextureMasksHigh(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < size / 2; y++) {
            for (int x = 0; x < size / 2; x++) {
                textureMasksHigh.getRaster().setPixel(x, y, new int[]{(int) (StrictMath.min(1f, mask0.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask1.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask2.get(x, y)) * 255f), (int) (StrictMath.min(1f, mask3.get(x, y)) * 255f)});
            }
        }
    }

    @SneakyThrows
    public void writeToFile(Path path) {
        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        FileOutputStream out = new FileOutputStream(outFile);
        out.write(toString().getBytes());
        out.flush();
        out.close();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("SCMap%n"));
        stringBuilder.append(String.format("Biome: %s%n", biome.getName()));
        stringBuilder.append(String.format("%s%n", biome.getLightingSettings().toString()));
        stringBuilder.append(String.format("%s%n", biome.getWaterSettings().toString()));
        stringBuilder.append(String.format("Terrain Materials: %s%n", biome.getTerrainMaterials().toString()));
        stringBuilder.append(String.format("Size: %d%n", size));
        for (int i = 0; i < decals.size(); i++) {
            stringBuilder.append(String.format("Decal %d: %s%n", i, decals.get(i).toString()));
        }
        for (int i = 0; i < props.size(); i++) {
            stringBuilder.append(String.format("Prop %d: %s%n", i, props.get(i).toString()));
        }

        return stringBuilder.toString();
    }
}
