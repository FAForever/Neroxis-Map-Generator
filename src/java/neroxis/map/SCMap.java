package neroxis.map;

import lombok.Data;
import lombok.SneakyThrows;
import neroxis.biomes.Biome;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

import static neroxis.util.ImageUtils.insertImageIntoNewImageOfSize;
import static neroxis.util.ImageUtils.scaleImage;
import static neroxis.util.Placement.placeOnHeightmap;

@Data
public strictfp class SCMap {

    public static final int SIGNATURE = 443572557;
    public static final int VERSION_MAJOR = 2;

    public static final int WAVE_NORMAL_COUNT = 4;
    public static final float[] WAVE_NORMAL_REPEATS = {0.0009f, 0.009f, 0.05f, 0.5f};
    public static final Vector2f[] WAVE_NORMAL_MOVEMENTS = {new Vector2f(0.5f, -0.95f), new Vector2f(0.05f, -0.095f), new Vector2f(0.01f, 0.03f), new Vector2f(0.0005f, 0.0009f)};
    public static final String[] WAVE_TEXTURE_PATHS = {"/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds"}; // always same?

    private float heightMapScale = 1f / 128f;

    private final ArrayList<Spawn> spawns;
    private String name = "";
    private final ArrayList<Marker> mexes;
    private final ArrayList<Marker> hydros;
    private int size; // must be a power of 2. 512 equals a 10x10km Map
    private int minorVersion = 56;
    private final ArrayList<Marker> blankMarkers;
    private String description = "";
    private String terrainShaderPath = "TTerrainXP";
    private String backgroundPath = "/textures/environment/defaultbackground.dds";
    private final ArrayList<DecalGroup> decalGroups;
    private int spawnCountInit;
    private int mexCountInit;
    private int hydroCountInit;
    private boolean generatePreview;
    private boolean isUnexplored;
    private float noRushRadius = 50;
    private String folderName = "";
    private String filePrefix = "";
    private final ArrayList<Decal> decals;
    private final ArrayList<WaveGenerator> waveGenerators;
    private final ArrayList<Prop> props;
    private final ArrayList<Army> armies;
    private String script = "";
    private final ArrayList<AIMarker> landAIMarkers;
    private final ArrayList<AIMarker> amphibiousAIMarkers;
    private final ArrayList<AIMarker> navyAIMarkers;
    private final ArrayList<AIMarker> airAIMarkers;
    private final ArrayList<AIMarker> rallyMarkers;
    private final ArrayList<AIMarker> expansionAIMarkers;
    private final ArrayList<AIMarker> largeExpansionAIMarkers;
    private final ArrayList<AIMarker> navalAreaAIMarkers;
    private final ArrayList<AIMarker> navyRallyMarkers;
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
        armies = new ArrayList<>();
        blankMarkers = new ArrayList<>();
        landAIMarkers = new ArrayList<>();
        amphibiousAIMarkers = new ArrayList<>();
        navyAIMarkers = new ArrayList<>();
        airAIMarkers = new ArrayList<>();
        rallyMarkers = new ArrayList<>();
        expansionAIMarkers = new ArrayList<>();
        largeExpansionAIMarkers = new ArrayList<>();
        navalAreaAIMarkers = new ArrayList<>();
        navyRallyMarkers = new ArrayList<>();
        waveGenerators = new ArrayList<>();
        cubeMaps = new ArrayList<>();
        cubeMaps.add(new CubeMap("<default>", "/textures/environment/defaultenvcube.dds"));

        generatePreview = true;
        preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);// always 256 x 256 px
        heightmap = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_USHORT_GRAY);
        normalMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        textureMasksLow = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        textureMasksHigh = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

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

    public Spawn getSpawn(int i) {
        return spawns.get(i);
    }

    public void addSpawn(Spawn spawn) {
        spawns.add(spawn);
    }

    public int getMexCount() {
        return mexes.size();
    }

    public Marker getMex(int i) {
        return mexes.get(i);
    }

    public void addMex(Marker mex) {
        mexes.add(mex);
    }

    public int getHydroCount() {
        return hydros.size();
    }

    public Marker getHydro(int i) {
        return hydros.get(i);
    }

    public void addHydro(Marker hydro) {
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

    public int getArmyCount() {
        return armies.size();
    }

    public Army getArmy(int i) {
        return armies.get(i);
    }

    public Army getArmy(String id) {
        return armies.stream().filter(army -> army.getId().equals(id)).findFirst().orElse(null);
    }

    public void addArmy(Army army) {
        armies.add(army);
    }

    public int getBlankCount() {
        return blankMarkers.size();
    }

    public Marker getBlank(int i) {
        return blankMarkers.get(i);
    }

    public Marker getBlank(String id) {
        return blankMarkers.stream().filter(blankMarker -> blankMarker.getId().equals(id)).findFirst().orElse(null);
    }

    public void addBlank(Marker blank) {
        blankMarkers.add(blank);
    }

    public int getLandMarkerCount() {
        return landAIMarkers.size();
    }

    public AIMarker getLandMarker(int i) {
        return landAIMarkers.get(i);
    }

    public AIMarker getLandMarker(String id) {
        return landAIMarkers.stream().filter(landMarker -> landMarker.getId().equals(id)).findFirst().orElse(null);
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

    public AIMarker getAmphibiousMarker(String id) {
        return amphibiousAIMarkers.stream().filter(amphibiousMarker -> amphibiousMarker.getId().equals(id)).findFirst().orElse(null);
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

    public AIMarker getNavyMarker(String id) {
        return navyAIMarkers.stream().filter(navyMarker -> navyMarker.getId().equals(id)).findFirst().orElse(null);
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

    public AIMarker getAirMarker(String id) {
        return airAIMarkers.stream().filter(airMarker -> airMarker.getId().equals(id)).findFirst().orElse(null);
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
        return navyRallyMarkers.size();
    }

    public AIMarker getNavyRallyMarker(int i) {
        return navyRallyMarkers.get(i);
    }

    public void addNavyRallyMarker(AIMarker aiMarker) {
        navyRallyMarkers.add(aiMarker);
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

    public void resize(int resizeCurrentMapContentTo, int newMapBoundsSize, Vector2f locToPutCenterOfCurrentMapContent, float heightMultiplier) {
        int oldSize = getSize();
        Vector2f locToPutTopLeftOfCurrentMapContent = new Vector2f(locToPutCenterOfCurrentMapContent.x - (float) resizeCurrentMapContentTo / 2, locToPutCenterOfCurrentMapContent.y - (float) resizeCurrentMapContentTo / 2);
        float contentScaler = (float) resizeCurrentMapContentTo / (float) oldSize;
        float boundsScaler = (float) newMapBoundsSize / (float) oldSize / contentScaler;
        SymmetrySettings symmetrySettings = new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE);
        FloatMask scaledHeightmap = getHeightMask(symmetrySettings);
        scaledHeightmap.multiply(heightMultiplier);
        setHeightImage(scaledHeightmap);

        this.biome.getWaterSettings().setElevation(this.biome.getWaterSettings().getElevation() * heightMultiplier);
        this.biome.getWaterSettings().setElevationDeep(this.biome.getWaterSettings().getElevationDeep() * heightMultiplier);
        this.biome.getWaterSettings().setElevationAbyss(this.biome.getWaterSettings().getElevationAbyss() * heightMultiplier);

        heightmap = scaleImage(heightmap, StrictMath.round((heightmap.getWidth() - 1) * contentScaler / 64) * 64 + 1,  StrictMath.round((heightmap.getHeight() - 1) * contentScaler / 64) * 64 + 1);
        heightmap = insertImageIntoNewImageOfSize(heightmap, StrictMath.round(((heightmap.getWidth() - 1) * boundsScaler) / 64) * 64 + 1,  StrictMath.round((heightmap.getHeight() - 1) * boundsScaler / 64) * 64 + 1, locToPutTopLeftOfCurrentMapContent);
        normalMap = scaleImage(normalMap, StrictMath.round(normalMap.getWidth() * contentScaler),  StrictMath.round(normalMap.getHeight() * contentScaler));
        normalMap = insertImageIntoNewImageOfSize(normalMap, StrictMath.round(normalMap.getWidth() * boundsScaler),  StrictMath.round(normalMap.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);
        waterMap = scaleImage(waterMap, StrictMath.round(waterMap.getWidth() * contentScaler),  StrictMath.round(waterMap.getHeight() * contentScaler));
        waterMap = insertImageIntoNewImageOfSize(waterMap, StrictMath.round(waterMap.getWidth() * boundsScaler),  StrictMath.round(waterMap.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);
        waterFoamMask = scaleImage(waterFoamMask, StrictMath.round(waterFoamMask.getWidth() * contentScaler),  StrictMath.round(waterFoamMask.getHeight() * contentScaler));
        waterFoamMask = insertImageIntoNewImageOfSize(waterFoamMask, StrictMath.round(waterFoamMask.getWidth() * boundsScaler),  StrictMath.round(waterFoamMask.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);
        waterFlatnessMask = scaleImage(waterFlatnessMask, StrictMath.round(waterFlatnessMask.getWidth() * contentScaler),  StrictMath.round(waterFlatnessMask.getHeight() * contentScaler));
        waterFlatnessMask = insertImageIntoNewImageOfSize(waterFlatnessMask, StrictMath.round(waterFlatnessMask.getWidth() * boundsScaler),  StrictMath.round(waterFlatnessMask.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);
        waterDepthBiasMask = scaleImage(waterDepthBiasMask, StrictMath.round(waterDepthBiasMask.getWidth() * contentScaler),  StrictMath.round(waterDepthBiasMask.getHeight() * contentScaler));
        waterDepthBiasMask = insertImageIntoNewImageOfSize(waterDepthBiasMask, StrictMath.round(waterDepthBiasMask.getWidth() * boundsScaler),  StrictMath.round(waterDepthBiasMask.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);
        terrainType = scaleImage(terrainType, StrictMath.round(terrainType.getWidth() * contentScaler),  StrictMath.round(terrainType.getHeight() * contentScaler));
        terrainType = insertImageIntoNewImageOfSize(terrainType, StrictMath.round(terrainType.getWidth() * boundsScaler),  StrictMath.round(terrainType.getHeight() * boundsScaler), locToPutTopLeftOfCurrentMapContent);

        FloatMask[] texturesMasks = getTextureMasksScaled(symmetrySettings);
        FloatMask oldLayer1 = texturesMasks[0];
        FloatMask oldLayer2 = texturesMasks[1];
        FloatMask oldLayer3 = texturesMasks[2];
        FloatMask oldLayer4 = texturesMasks[3];
        FloatMask oldLayer5 = texturesMasks[4];
        FloatMask oldLayer6 = texturesMasks[5];
        FloatMask oldLayer7 = texturesMasks[6];
        FloatMask oldLayer8 = texturesMasks[7];

        setTextureMasksLow(new BufferedImage(newMapBoundsSize, newMapBoundsSize, BufferedImage.TYPE_INT_ARGB));
        setTextureMasksHigh(new BufferedImage(newMapBoundsSize, newMapBoundsSize, BufferedImage.TYPE_INT_ARGB));

        oldLayer1.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer2.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer3.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer4.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer5.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer6.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer7.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);
        oldLayer8.min(0f).max(1f).setSize2(resizeCurrentMapContentTo, true);

        FloatMask layer1 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer1.addWithOffset(oldLayer1, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer2 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer2.addWithOffset(oldLayer2, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer3 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer3.addWithOffset(oldLayer3, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer4 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer4.addWithOffset(oldLayer4, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer5 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer5.addWithOffset(oldLayer5, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer6 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer6.addWithOffset(oldLayer6, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer7 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer7.addWithOffset(oldLayer7, locToPutCenterOfCurrentMapContent, true);
        FloatMask layer8 =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        layer8.addWithOffset(oldLayer8, locToPutCenterOfCurrentMapContent, true);

        setTextureMasksLowScaled(layer1, layer2, layer3, layer4);
        setTextureMasksHighScaled(layer5, layer6, layer7, layer8);

        this.size = newMapBoundsSize;

        FloatMask smallHeightmapBase = getHeightMask(symmetrySettings);
        FloatMask heightmapBase =  new FloatMask(newMapBoundsSize, new Random().nextLong(), symmetrySettings);
        heightmapBase.addWithOffset(smallHeightmapBase, locToPutCenterOfCurrentMapContent, true);
        int shiftX = (int) locToPutCenterOfCurrentMapContent.x - resizeCurrentMapContentTo / 2;
        int shiftZ = (int) locToPutCenterOfCurrentMapContent.y - resizeCurrentMapContentTo / 2;
        Vector2f shiftXAndZ = new Vector2f(shiftX, shiftZ);

        repositionItems(getSpawns(), contentScaler, shiftXAndZ);
        repositionItems(getAirAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getAmphibiousAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getExpansionAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getLargeExpansionAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getNavalAreaAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getNavyAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getLandAIMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getNavyRallyMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getRallyMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getBlankMarkers(), contentScaler, shiftXAndZ);
        repositionItems(getHydros(), contentScaler, shiftXAndZ);
        repositionItems(getMexes(), contentScaler, shiftXAndZ);
        repositionItems(getProps(), contentScaler, shiftXAndZ);
        repositionItems(getDecals(), contentScaler, shiftXAndZ);

        for (int i = 0; i < getDecalCount(); i++) {
            Vector3f scale = getDecal(i).getScale();
            getDecal(i).setScale(new Vector3f(scale.x * contentScaler, scale.y, scale.z * contentScaler));
            getDecal(i).setCutOffLOD(getDecal(i).getCutOffLOD() * contentScaler);
        }

        for (int i = 0; i < getArmyCount(); i++) {
            Army army = getArmy(i);
            for (int a = 0; a < army.getGroupCount(); a++) {
                Group group = army.getGroup(a);
                repositionItems(group.getUnits(), contentScaler, shiftXAndZ);
            }
        }
    }

    public void repositionItems(ArrayList<? extends PositionedObject> arrayListOfPositionedObjects, float distanceScaler, Vector2f shiftXAndZ) {
        for (PositionedObject positionedObject : arrayListOfPositionedObjects) {
            Vector2f newPosition = new Vector2f(positionedObject.getPosition().x, positionedObject.getPosition().z);
            newPosition.multiply(distanceScaler).add(shiftXAndZ).roundToNearestHalfPoint();
            positionedObject.setPosition(placeOnHeightmap(this, newPosition));
        }
    }

    public void setHeightImage(FloatMask heightmap) {
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                this.heightmap.getRaster().setPixel(x, y, new int[]{(short) (heightmap.getValueAt(x, y) / heightMapScale)});
            }
        }
    }

    public FloatMask getHeightMask(SymmetrySettings symmetrySettings) {
        FloatMask heightMask = new FloatMask(this.heightmap.getHeight(), null, symmetrySettings);
        for (int y = 0; y < size + 1; y++) {
            for (int x = 0; x < size + 1; x++) {
                heightMask.setValueAt(x, y, this.heightmap.getRaster().getPixel(x, y, new int[1])[0] * heightMapScale);
            }
        }
        return heightMask;
    }

    public void setPreviewImage(FloatMask previewMask) {
        for (int y = 0; y < previewMask.getSize(); y++) {
            for (int x = 0; x < previewMask.getSize(); x++) {
                this.preview.setRGB(x, y, previewMask.getValueAt(x, y).intValue());
            }
        }
    }

    public FloatMask getPreviewMask(SymmetrySettings symmetrySettings) {
        FloatMask previewMask = new FloatMask(this.preview.getHeight(), null, symmetrySettings);
        for (int y = 0; y < previewMask.getSize(); y++) {
            for (int x = 0; x < previewMask.getSize(); x++) {
                previewMask.setValueAt(x, y, (float) this.preview.getRGB(x, y));
            }
        }
        return previewMask;
    }

    public void setTextureMasksLowScaled(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getWidth(); x++) {
                int val0 = convertToRawTextureValue(mask0.getValueAt(x, y));
                int val1 = convertToRawTextureValue(mask1.getValueAt(x, y));
                int val2 = convertToRawTextureValue(mask2.getValueAt(x, y));
                int val3 = convertToRawTextureValue(mask3.getValueAt(x, y));
                textureMasksLow.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    public void setTextureMasksHighScaled(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getWidth(); x++) {
                int val0 = convertToRawTextureValue(mask0.getValueAt(x, y));
                int val1 = convertToRawTextureValue(mask1.getValueAt(x, y));
                int val2 = convertToRawTextureValue(mask2.getValueAt(x, y));
                int val3 = convertToRawTextureValue(mask3.getValueAt(x, y));
                textureMasksHigh.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    public void setTextureMasksLowRaw(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getWidth(); x++) {
                float val0 = mask0.getValueAt(x, y);
                float val1 = mask1.getValueAt(x, y);
                float val2 = mask2.getValueAt(x, y);
                float val3 = mask3.getValueAt(x, y);
                textureMasksLow.getRaster().setPixel(x, y, new float[]{val0, val1, val2, val3});
            }
        }
    }

    public void setTextureMasksHighRaw(FloatMask mask0, FloatMask mask1, FloatMask mask2, FloatMask mask3) {
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getWidth(); x++) {
                float val0 = mask0.getValueAt(x, y);
                float val1 = mask1.getValueAt(x, y);
                float val2 = mask2.getValueAt(x, y);
                float val3 = mask3.getValueAt(x, y);
                textureMasksHigh.getRaster().setPixel(x, y, new float[]{val0, val1, val2, val3});
            }
        }
    }

    public FloatMask[] getTextureMasksScaled(SymmetrySettings symmetrySettings) {
        FloatMask mask0 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask1 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask2 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask3 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getHeight(); x++) {
                int[] valsLow = new int[4];
                textureMasksLow.getRaster().getPixel(x, y, valsLow);
                mask0.setValueAt(x, y, convertToScaledTextureValue(valsLow[0]));
                mask1.setValueAt(x, y, convertToScaledTextureValue(valsLow[1]));
                mask2.setValueAt(x, y, convertToScaledTextureValue(valsLow[2]));
                mask3.setValueAt(x, y, convertToScaledTextureValue(valsLow[3]));
            }
        }
        FloatMask mask4 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask5 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask6 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask7 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getHeight(); x++) {
                int[] valsHigh = new int[4];
                textureMasksHigh.getRaster().getPixel(x, y, valsHigh);
                mask4.setValueAt(x, y, convertToScaledTextureValue(valsHigh[0]));
                mask5.setValueAt(x, y, convertToScaledTextureValue(valsHigh[1]));
                mask6.setValueAt(x, y, convertToScaledTextureValue(valsHigh[2]));
                mask7.setValueAt(x, y, convertToScaledTextureValue(valsHigh[3]));
            }
        }
        return new FloatMask[]{mask0, mask1, mask2, mask3, mask4, mask5, mask6, mask7};
    }

    public FloatMask[] getTextureMasksRaw(SymmetrySettings symmetrySettings) {
        FloatMask mask0 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask1 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask2 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        FloatMask mask3 = new FloatMask(this.textureMasksLow.getHeight(), null, symmetrySettings);
        for (int y = 0; y < textureMasksLow.getHeight(); y++) {
            for (int x = 0; x < textureMasksLow.getHeight(); x++) {
                float[] valsLow = new float[4];
                textureMasksLow.getRaster().getPixel(x, y, valsLow);
                mask0.setValueAt(x, y, valsLow[0]);
                mask1.setValueAt(x, y, valsLow[1]);
                mask2.setValueAt(x, y, valsLow[2]);
                mask3.setValueAt(x, y, valsLow[3]);
            }
        }
        FloatMask mask4 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask5 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask6 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        FloatMask mask7 = new FloatMask(this.textureMasksHigh.getHeight(), null, symmetrySettings);
        for (int y = 0; y < textureMasksHigh.getHeight(); y++) {
            for (int x = 0; x < textureMasksHigh.getHeight(); x++) {
                float[] valsHigh = new float[4];
                textureMasksHigh.getRaster().getPixel(x, y, valsHigh);
                mask4.setValueAt(x, y, valsHigh[0]);
                mask5.setValueAt(x, y, valsHigh[1]);
                mask6.setValueAt(x, y, valsHigh[2]);
                mask7.setValueAt(x, y, valsHigh[3]);
            }
        }
        return new FloatMask[]{mask0, mask1, mask2, mask3, mask4, mask5, mask6, mask7};
    }

    private float convertToScaledTextureValue(float value) {
        return value > 0 ? (value - 128) / 127f : 0f;
    }

    private int convertToRawTextureValue(float value) {
        return value > 0f ? StrictMath.round(StrictMath.min(1f, value) * 127 + 128) : 0;
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
