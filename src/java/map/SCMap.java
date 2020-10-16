package map;

import biomes.Biome;
import lombok.Data;
import lombok.SneakyThrows;
import util.Vector2f;
import util.Vector3f;

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

    public static final int WAVE_NORMAL_COUNT = 4;
    public static final float[] WAVE_NORMAL_REPEATS = {0.0009f, 0.009f, 0.05f, 0.5f};
    public static final Vector2f[] WAVE_NORMAL_MOVEMENTS = {new Vector2f(0.5f, -0.95f), new Vector2f(0.05f, -0.095f), new Vector2f(0.01f, 0.03f), new Vector2f(0.0005f, 0.0009f)};
    public static final String[] WAVE_TEXTURE_PATHS = {"/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds"}; // always same?

    private float heightMapScale = 1f / 128f;

    private final int size; // must be a power of 2. 512 equals a 10x10km Map
    private int minorVersion = 56;
    private String terrainShaderPath = "TTerrainXP";
    private String backgroundPath = "/textures/environment/defaultbackground.dds";
    private final ArrayList<DecalGroup> decalGroups;
    private int spawnCountInit;
    private int mexCountInit;
    private int hydroCountInit;
    private final ArrayList<Vector3f> spawns;
    private final ArrayList<Vector3f> mexes;
    private final ArrayList<Vector3f> hydros;
    private final ArrayList<Decal> decals;
    private final ArrayList<WaveGenerator> waveGenerators;
    private final ArrayList<Prop> props;
    private final ArrayList<Unit> units;
    private final ArrayList<Unit> civs;
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
    private final ArrayList<CubeMap> cubeMaps;
    private String skyCubePath = "/textures/environment/defaultskycube.dds";
    private Biome biome;
    private SkyBox skyBox;
    private BufferedImage preview;
    private BufferedImage heightmap;
    private BufferedImage normalMap;
    private BufferedImage textureMasksLow;
    private BufferedImage textureMasksHigh;

    private BufferedImage waterMap;
    private BufferedImage waterFoamMask;
    private BufferedImage waterFlatnessMask;
    private BufferedImage waterDepthBiasMask;
    private BufferedImage terrainType;

    private int miniMapContourInterval = 0;
    private int miniMapDeepWaterColor = 0;
    private int miniMapContourColor = 0;
    private int miniMapShoreColor = 0;
    private int miniMapLandStartColor = 0;
    private int miniMapLandEndColor = 0;

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
        decalGroups = new ArrayList<>();
        props = new ArrayList<>();
        units = new ArrayList<>();
        civs = new ArrayList<>();
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
        waveGenerators = new ArrayList<>();
        cubeMaps = new ArrayList<>();
        cubeMaps.add(new CubeMap("<default>", "/textures/environment/defaultenvcube.dds"));

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

    public int getDecalGroupCount() {
        return decalGroups.size();
    }

    public DecalGroup getDecalGroup(int i) {
        return decalGroups.get(i);
    }

    public void addDecalGroup(DecalGroup decalGroup) {
        decalGroups.add(decalGroup);
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

    public int getCivCount() {
        return civs.size();
    }

    public Unit getCiv(int i) {
        return civs.get(i);
    }

    public void addCiv(Unit unit) {
        civs.add(unit);
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

    public int getWaveGeneratorCount() {
        return waveGenerators.size();
    }

    public WaveGenerator getWaveGenerator(int i) {
        return waveGenerators.get(i);
    }

    public void addWaveGenerator(WaveGenerator waveGenerator) {
        waveGenerators.add(waveGenerator);
    }

    public int getCubeMapCount() {
        return cubeMaps.size();
    }

    public CubeMap getCubemap(int i) {
        return cubeMaps.get(i);
    }

    public void addCubeMap(CubeMap cubeMap) {
        cubeMaps.add(cubeMap);
    }

    public void setHeightImage(FloatMask heightmap) {
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                this.heightmap.getRaster().setPixel(x, y, new int[]{(short) (heightmap.get(x, y) / heightMapScale)});
            }
        }
    }

    public FloatMask getHeightMask(SymmetryHierarchy symmetryHierarchy) {
        FloatMask heightMask = new FloatMask(this.heightmap.getHeight(), null, symmetryHierarchy);
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                heightMask.set(x, y, this.heightmap.getRaster().getPixel(x, y, new int[1])[0] * heightMapScale);
            }
        }
        return heightMask;
    }

    public void setPreviewImage(FloatMask previewMask) {
        for (int y = 0; y < previewMask.getSize(); y++) {
            for (int x = 0; x < previewMask.getSize(); x++) {
                this.preview.setRGB(x, y, (int) previewMask.get(x, y));
            }
        }
    }

    public FloatMask getPreviewMask(SymmetryHierarchy symmetryHierarchy) {
        FloatMask previewMask = new FloatMask(this.preview.getHeight(), null, symmetryHierarchy);
        for (int y = 0; y < previewMask.getSize(); y++) {
            for (int x = 0; x < previewMask.getSize(); x++) {
                previewMask.set(x, y, this.preview.getRGB(x, y));
            }
        }
        return previewMask;
    }

    public void setTextureMasksLow(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getWidth(); x++) {
                int val0 = mask0.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask0.get(x, y)) * 127 + 128) : 0;
                int val1 = mask1.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask1.get(x, y)) * 127 + 128) : 0;
                int val2 = mask2.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask2.get(x, y)) * 127 + 128) : 0;
                int val3 = mask3.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask3.get(x, y)) * 127 + 128) : 0;
                textureMasksLow.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    public void setTextureMasksHigh(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getWidth(); x++) {
                int val0 = mask0.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask0.get(x, y)) * 127 + 128) : 0;
                int val1 = mask1.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask1.get(x, y)) * 127 + 128) : 0;
                int val2 = mask2.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask2.get(x, y)) * 127 + 128) : 0;
                int val3 = mask3.get(x, y) > 0f ? StrictMath.round(StrictMath.min(1f, mask3.get(x, y)) * 127 + 128) : 0;
                textureMasksHigh.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    public FloatMask[] getTextureMasks(SymmetryHierarchy symmetryHierarchy) {
        FloatMask mask0 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetryHierarchy);
        FloatMask mask1 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetryHierarchy);
        FloatMask mask2 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetryHierarchy);
        FloatMask mask3 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetryHierarchy);
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getHeight(); x++) {
                int[] valsLow = new int[4];
                textureMasksLow.getRaster().getPixel(x, y, valsLow);
                mask0.set(x, y, valsLow[0] > 0 ? (valsLow[0] - 128) / 127f : 0f);
                mask1.set(x, y, valsLow[1] > 0 ? (valsLow[1] - 128) / 127f : 0f);
                mask2.set(x, y, valsLow[2] > 0 ? (valsLow[2] - 128) / 127f : 0f);
                mask3.set(x, y, valsLow[3] > 0 ? (valsLow[3] - 128) / 127f : 0f);
            }
        }
        FloatMask mask4 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetryHierarchy);
        FloatMask mask5 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetryHierarchy);
        FloatMask mask6 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetryHierarchy);
        FloatMask mask7 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetryHierarchy);
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getHeight(); x++) {
                int[] valsHigh = new int[4];
                textureMasksHigh.getRaster().getPixel(x, y, valsHigh);
                mask4.set(x, y, valsHigh[0] > 0 ? (valsHigh[0] - 128) / 127f : 0f);
                mask5.set(x, y, valsHigh[1] > 0 ? (valsHigh[1] - 128) / 127f : 0f);
                mask6.set(x, y, valsHigh[2] > 0 ? (valsHigh[2] - 128) / 127f : 0f);
                mask7.set(x, y, valsHigh[3] > 0 ? (valsHigh[3] - 128) / 127f : 0f);
            }
        }
        return new FloatMask[]{mask0, mask1, mask2, mask3, mask4, mask5, mask6, mask7};
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
