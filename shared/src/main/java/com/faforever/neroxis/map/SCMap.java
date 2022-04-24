package com.faforever.neroxis.map;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.ImageUtil;
import static com.faforever.neroxis.util.ImageUtil.insertImageIntoNewImageOfSize;
import static com.faforever.neroxis.util.ImageUtil.scaleImage;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;

@SuppressWarnings("unused")
@Data
public strictfp class SCMap {

    public static final int SIGNATURE = 443572557;
    public static final int VERSION_MAJOR = 2;
    public static final int WAVE_NORMAL_COUNT = 4;
    public static final float[] WAVE_NORMAL_REPEATS = {0.0009f, 0.009f, 0.05f, 0.5f};
    public static final Vector2[] WAVE_NORMAL_MOVEMENTS = {new Vector2(0.5f, -0.95f), new Vector2(0.05f,
                                                                                                  -0.095f), new Vector2(
            0.01f, 0.03f), new Vector2(0.0005f, 0.0009f)};
    public static final String[] WAVE_TEXTURE_PATHS = {"/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds"}; // always same?
    private final List<Spawn> spawns;
    private final List<Marker> mexes;
    private final List<Marker> hydros;
    private final List<Marker> blankMarkers;
    private final List<DecalGroup> decalGroups;
    private final List<Decal> decals;
    private final List<WaveGenerator> waveGenerators;
    private final List<Prop> props;
    private final List<Army> armies;
    private final List<AIMarker> landAIMarkers;
    private final List<AIMarker> amphibiousAIMarkers;
    private final List<AIMarker> navyAIMarkers;
    private final List<AIMarker> airAIMarkers;
    private final List<AIMarker> rallyMarkers;
    private final List<AIMarker> expansionAIMarkers;
    private final List<AIMarker> largeExpansionAIMarkers;
    private final List<AIMarker> navalAreaAIMarkers;
    private final List<AIMarker> navalRallyMarkers;
    private final List<CubeMap> cubeMaps;
    private byte[] compressedNormal;
    private byte[] compressedShadows;
    private float heightMapScale = 1f / 128f;
    private String name = "";
    @Setter(AccessLevel.NONE)
    private int size; // must be a power of 2 when exported. 512 equals a 10x10km Map
    private Vector4 playableArea;
    private int minorVersion = 56;
    private String description = "";
    private String terrainShaderPath = "TTerrainXP";
    private String backgroundPath = "/textures/environment/defaultbackground.dds";
    private boolean generatePreview;
    private boolean isUnexplored;
    private float noRushRadius = 50;
    private String folderName = "";
    private String filePrefix = "";
    private String script = "";
    private String skyCubePath = "/textures/environment/defaultskycube.dds";
    private Biome biome;
    private SkyBox skyBox;
    private BufferedImage preview;
    private BufferedImage heightmap;
    private BufferedImage normalMap;
    private BufferedImage textureMasksLow;
    private BufferedImage textureMasksHigh;
    private BufferedImage waterMap;
    private BufferedImage waterFoamMap;
    private BufferedImage waterFlatnessMap;
    private BufferedImage waterDepthBiasMap;
    private BufferedImage terrainType;
    private int cartographicContourInterval = 1000;
    private int cartographicDeepWaterColor = new Color(0, 34, 255).getRGB();
    private int cartographicMapContourColor = new Color(255, 0, 0).getRGB();
    private int cartographicMapShoreColor = new Color(161, 192, 255).getRGB();
    private int cartographicMapLandStartColor = new Color(255, 255, 255).getRGB();
    private int cartographicMapLandEndColor = new Color(0, 0, 0).getRGB();

    public SCMap(int size, Biome biome) {
        this.size = size;
        this.biome = biome;
        playableArea = new Vector4(0, 0, size, size);
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
        navalRallyMarkers = new ArrayList<>();
        waveGenerators = new ArrayList<>();
        cubeMaps = new ArrayList<>();
        cubeMaps.add(new CubeMap("<default>", "/textures/environment/defaultenvcube.dds"));
        skyBox = new SkyBox();

        generatePreview = true;
        preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);// always 256 x 256 px
        heightmap = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_USHORT_GRAY);
        normalMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        textureMasksLow = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_INT_ARGB);
        textureMasksHigh = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_INT_ARGB);

        waterMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);
        waterFoamMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
        waterFlatnessMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < size / 2; y++) {
            for (int x = 0; x < size / 2; x++) {
                waterFlatnessMap.getRaster().setPixel(x, y, new int[]{255});
            }
        }
        waterDepthBiasMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);

        terrainType = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
    }

    public void setPreview(BufferedImage preview) {
        checkImageSize(preview, 256);
        this.preview = preview;
    }

    public AIMarker getAmphibiousMarker(String id) {
        return amphibiousAIMarkers.stream()
                                  .filter(amphibiousMarker -> amphibiousMarker.getId().equals(id))
                                  .findFirst()
                                  .orElse(null);
    }

    public void setHeightmap(BufferedImage heightmap) {
        checkImageSize(heightmap, size + 1);
        this.heightmap = heightmap;
    }

    public void setWaterFoamMap(BufferedImage waterFoamMap) {
        checkImageSize(waterFoamMap, size / 2);
        this.waterFoamMap = waterFoamMap;
    }

    public void setTerrainType(BufferedImage terrainType) {
        checkImageSize(terrainType, size);
        this.terrainType = terrainType;
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

    public void changeMapSize(int contentSize, int boundsSize, Vector2 boundOffset) {
        int oldSize = size;
        Vector2 topLeftOffset = new Vector2(boundOffset.getX() - (float) contentSize / 2,
                                            boundOffset.getY() - (float) contentSize / 2);
        float contentScale = (float) contentSize / (float) oldSize;
        float boundsScale = (float) boundsSize / (float) contentSize;

        if (contentScale != 1) {
            scaleMapContent(contentScale);
            this.size = contentSize;
            playableArea.multiply(contentScale);
        }

        if (boundsScale != 1 && topLeftOffset.getX() != 0 && topLeftOffset.getY() != 0) {
            scaleMapBounds(boundsScale, topLeftOffset);
            playableArea.add(topLeftOffset.getX(), topLeftOffset.getY(), topLeftOffset.getX(), topLeftOffset.getY());
            this.size = boundsSize;
        }

        if (contentScale != 1 || (boundsScale != 1 && topLeftOffset.getX() != 0 && topLeftOffset.getY() != 0)) {
            moveObjects(contentScale, topLeftOffset);
        }
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

    private void scaleMapContent(float contentScale) {
        this.biome.getWaterSettings().setElevation(this.biome.getWaterSettings().getElevation() * contentScale);
        this.biome.getWaterSettings().setElevationDeep(this.biome.getWaterSettings().getElevationDeep() * contentScale);
        this.biome.getWaterSettings()
                  .setElevationAbyss(this.biome.getWaterSettings().getElevationAbyss() * contentScale);

        RescaleOp heightRescale = new RescaleOp(contentScale, 0, null);
        heightRescale.filter(heightmap, heightmap);
        heightmap = scaleImage(heightmap, StrictMath.round((heightmap.getWidth() - 1) * contentScale) + 1,
                               StrictMath.round((heightmap.getHeight() - 1) * contentScale) + 1);
        normalMap = scaleImage(normalMap, StrictMath.round(normalMap.getWidth() * contentScale),
                               StrictMath.round(normalMap.getHeight() * contentScale));
        waterMap = scaleImage(waterMap, StrictMath.round(waterMap.getWidth() * contentScale),
                              StrictMath.round(waterMap.getHeight() * contentScale));
        waterFoamMap = scaleImage(waterFoamMap, StrictMath.round(waterFoamMap.getWidth() * contentScale),
                                  StrictMath.round(waterFoamMap.getHeight() * contentScale));
        waterFlatnessMap = scaleImage(waterFlatnessMap, StrictMath.round(waterFlatnessMap.getWidth() * contentScale),
                                      StrictMath.round(waterFlatnessMap.getHeight() * contentScale));
        waterDepthBiasMap = scaleImage(waterDepthBiasMap, StrictMath.round(waterDepthBiasMap.getWidth() * contentScale),
                                       StrictMath.round(waterDepthBiasMap.getHeight() * contentScale));
        terrainType = scaleImage(terrainType, StrictMath.round(terrainType.getWidth() * contentScale),
                                 StrictMath.round(terrainType.getHeight() * contentScale));
        textureMasksHigh = scaleImage(textureMasksHigh, StrictMath.round(textureMasksHigh.getWidth() * contentScale),
                                      StrictMath.round(textureMasksHigh.getHeight() * contentScale));
        textureMasksLow = scaleImage(textureMasksLow, StrictMath.round(textureMasksLow.getWidth() * contentScale),
                                     StrictMath.round(textureMasksLow.getHeight() * contentScale));
    }

    private void scaleMapBounds(float boundsScale, Vector2 topLeftOffset) {
        float normalMapScale = (float) normalMap.getWidth() / size;
        float waterMapScale = (float) waterMap.getWidth() / size;
        float textureMaskHighScale = (float) textureMasksHigh.getWidth() / size;
        float textureMaskLowScale = (float) textureMasksLow.getWidth() / size;
        preview = scaleImage(preview, StrictMath.round(256 / boundsScale), StrictMath.round(256 / boundsScale));
        Vector2 previewOffset = boundsScale > 1 ? new Vector2(128 - 128 / boundsScale,
                                                              128 - 128 / boundsScale) : new Vector2(-64 / boundsScale,
                                                                                                     -64 / boundsScale);
        preview = insertImageIntoNewImageOfSize(preview, 256, 256, previewOffset);
        heightmap = insertImageIntoNewImageOfSize(heightmap,
                                                  StrictMath.round(((heightmap.getWidth() - 1) * boundsScale)) + 1,
                                                  StrictMath.round((heightmap.getHeight() - 1) * boundsScale) + 1,
                                                  topLeftOffset);
        normalMap = insertImageIntoNewImageOfSize(normalMap, StrictMath.round(normalMap.getWidth() * boundsScale),
                                                  StrictMath.round(normalMap.getHeight() * boundsScale),
                                                  new Vector2(topLeftOffset).multiply(normalMapScale));
        waterMap = insertImageIntoNewImageOfSize(waterMap, StrictMath.round(waterMap.getWidth() * boundsScale),
                                                 StrictMath.round(waterMap.getHeight() * boundsScale),
                                                 new Vector2(topLeftOffset).multiply(waterMapScale));
        Vector2 halvedTopLeftOffset = new Vector2(topLeftOffset).multiply(.5f);
        waterFoamMap = insertImageIntoNewImageOfSize(waterFoamMap,
                                                     StrictMath.round(waterFoamMap.getWidth() * boundsScale),
                                                     StrictMath.round(waterFoamMap.getHeight() * boundsScale),
                                                     halvedTopLeftOffset);
        waterFlatnessMap = insertImageIntoNewImageOfSize(waterFlatnessMap,
                                                         StrictMath.round(waterFlatnessMap.getWidth() * boundsScale),
                                                         StrictMath.round(waterFlatnessMap.getHeight() * boundsScale),
                                                         halvedTopLeftOffset);
        waterDepthBiasMap = insertImageIntoNewImageOfSize(waterDepthBiasMap,
                                                          StrictMath.round(waterDepthBiasMap.getWidth() * boundsScale),
                                                          StrictMath.round(waterDepthBiasMap.getHeight() * boundsScale),
                                                          halvedTopLeftOffset);
        terrainType = insertImageIntoNewImageOfSize(terrainType, StrictMath.round(terrainType.getWidth() * boundsScale),
                                                    StrictMath.round(terrainType.getHeight() * boundsScale),
                                                    topLeftOffset);
        textureMasksHigh = insertImageIntoNewImageOfSize(textureMasksHigh,
                                                         StrictMath.round(textureMasksHigh.getWidth() * boundsScale),
                                                         StrictMath.round(textureMasksHigh.getHeight() * boundsScale),
                                                         new Vector2(topLeftOffset).multiply(textureMaskHighScale));
        textureMasksLow = insertImageIntoNewImageOfSize(textureMasksLow,
                                                        StrictMath.round(textureMasksLow.getWidth() * boundsScale),
                                                        StrictMath.round(textureMasksLow.getHeight() * boundsScale),
                                                        new Vector2(topLeftOffset).multiply(textureMaskLowScale));
    }

    private void moveObjects(float contentScale, Vector2 offset) {
        repositionObjects(getSpawns(), contentScale, offset);
        repositionObjects(getAirAIMarkers(), contentScale, offset);
        repositionObjects(getAmphibiousAIMarkers(), contentScale, offset);
        repositionObjects(getExpansionAIMarkers(), contentScale, offset);
        repositionObjects(getLargeExpansionAIMarkers(), contentScale, offset);
        repositionObjects(getNavalAreaAIMarkers(), contentScale, offset);
        repositionObjects(getNavyAIMarkers(), contentScale, offset);
        repositionObjects(getLandAIMarkers(), contentScale, offset);
        repositionObjects(getNavalRallyMarkers(), contentScale, offset);
        repositionObjects(getRallyMarkers(), contentScale, offset);
        repositionObjects(getBlankMarkers(), contentScale, offset);
        repositionObjects(getHydros(), contentScale, offset);
        repositionObjects(getMexes(), contentScale, offset);
        repositionObjects(getProps(), contentScale, offset);
        repositionObjects(getDecals(), contentScale, offset);
        repositionObjects(getWaveGenerators(), contentScale, offset);
        armies.forEach(
                army -> army.getGroups().forEach(group -> repositionObjects(group.getUnits(), contentScale, offset)));

        decals.forEach(decal -> {
            Vector3 scale = decal.getScale();
            decal.setScale(new Vector3(scale.getX() * contentScale, scale.getY(), scale.getZ() * contentScale));
            decal.setCutOffLOD(decal.getCutOffLOD() * contentScale);
        });

        setHeights();
    }

    private <T extends PositionedObject> void repositionObjects(Collection<T> positionedObjects, float distanceScale,
                                                                Vector2 offset) {
        Collection<T> repositionedObjects = new ArrayList<>();
        positionedObjects.forEach(positionedObject -> {
            Vector2 newPosition = new Vector2(positionedObject.getPosition()).multiply(distanceScale)
                                                                             .add(offset)
                                                                             .roundToNearestHalfPoint();
            positionedObject.setPosition(new Vector3(newPosition));
            if (ImageUtil.inImageBounds(newPosition, heightmap)) {
                repositionedObjects.add(positionedObject);
            }
        });
        positionedObjects.clear();
        positionedObjects.addAll(repositionedObjects);
    }

    private void setObjectHeights(Collection<? extends PositionedObject> positionedObjects) {
        positionedObjects.forEach(positionedObject -> {
            Vector2 position = new Vector2(positionedObject.getPosition());
            if (ImageUtil.inImageBounds(position, heightmap)) {
                positionedObject.getPosition()
                                .setY(heightmap.getRaster()
                                               .getPixel((int) position.getX(), (int) position.getY(), new int[]{0})[0]
                                      * heightMapScale);
            }
        });
    }

    public void setHeights() {
        setObjectHeights(getSpawns());
        setObjectHeights(getAirAIMarkers());
        setObjectHeights(getAmphibiousAIMarkers());
        setObjectHeights(getExpansionAIMarkers());
        setObjectHeights(getLargeExpansionAIMarkers());
        setObjectHeights(getNavalAreaAIMarkers());
        setObjectHeights(getNavyAIMarkers());
        setObjectHeights(getLandAIMarkers());
        setObjectHeights(getNavalRallyMarkers());
        setObjectHeights(getRallyMarkers());
        setObjectHeights(getBlankMarkers());
        setObjectHeights(getHydros());
        setObjectHeights(getMexes());
        setObjectHeights(getProps());
        setObjectHeights(getDecals());
        setObjectHeights(getWaveGenerators());
        armies.forEach(army -> army.getGroups().forEach(group -> setObjectHeights(group.getUnits())));
    }

    public void setWaterFlatnessMap(BufferedImage waterFlatnessMap) {
        checkImageSize(waterFlatnessMap, size / 2);
        this.waterFlatnessMap = waterFlatnessMap;
    }

    private static void checkImageSize(BufferedImage image, int size) {
        if (image.getWidth() != size) {
            throw new IllegalArgumentException("Image size does not match required size: Image size is "
                                               + image.getWidth()
                                               + " required size is "
                                               + size);
        }
    }

    public void setWaterDepthBiasMap(BufferedImage waterDepthBiasMap) {
        checkImageSize(waterDepthBiasMap, size / 2);
        this.waterDepthBiasMap = waterDepthBiasMap;
    }

    public void setTextureMasksScaled(BufferedImage textureMasks, FloatMask mask0, FloatMask mask1, FloatMask mask2,
                                      FloatMask mask3) {
        int textureMasksWidth = textureMasks.getWidth();
        checkMaskSize(mask0, textureMasksWidth);
        checkMaskSize(mask1, textureMasksWidth);
        checkMaskSize(mask2, textureMasksWidth);
        checkMaskSize(mask3, textureMasksWidth);
        for (int x = 0; x < textureMasksWidth; x++) {
            for (int y = 0; y < textureMasksWidth; y++) {
                int val0 = convertToRawTextureValue(mask0.getPrimitive(x, y));
                int val1 = convertToRawTextureValue(mask1.getPrimitive(x, y));
                int val2 = convertToRawTextureValue(mask2.getPrimitive(x, y));
                int val3 = convertToRawTextureValue(mask3.getPrimitive(x, y));
                textureMasks.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    private static void checkMaskSize(Mask<?, ?> mask, int size) {
        if (mask.getSize() != size) {
            throw new IllegalArgumentException("Image size does not match required size: Image size is "
                                               + mask.getSize()
                                               + " required size is "
                                               + size);
        }
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
        int numDecals = decals.size();
        for (int i = 0; i < numDecals; i++) {
            stringBuilder.append(String.format("Decal %d: %s%n", i, decals.get(i).toString()));
        }
        int numProps = props.size();
        for (int i = 0; i < numProps; i++) {
            stringBuilder.append(String.format("Prop %d: %s%n", i, props.get(i).toString()));
        }

        return stringBuilder.toString();
    }
}
