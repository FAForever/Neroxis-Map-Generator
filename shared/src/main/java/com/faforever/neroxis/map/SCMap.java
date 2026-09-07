package com.faforever.neroxis.map;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.WaterSettings;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;

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
import java.util.Map;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.neroxis.util.ImageUtil.insertImageIntoNewImageOfSize;
import static com.faforever.neroxis.util.ImageUtil.rotateImage;
import static com.faforever.neroxis.util.ImageUtil.scaleImage;

@SuppressWarnings("unused")
@Data
public class SCMap {
    public static final String PBR_SHADER_NAME = "Terrain250";
    public static final String LEGACY_SHADER_NAME = "TTerrainXP";
    public static final int SIGNATURE = 443572557;
    public static final int VERSION_MAJOR = 2;
    public static final int WAVE_NORMAL_COUNT = 4;
    public static final float[] WAVE_NORMAL_REPEATS = {0.0009f, 0.009f, 0.05f, 0.5f};
    public static final Vector2[] WAVE_NORMAL_MOVEMENTS = {new Vector2(0.5f, -0.95f), new Vector2(0.05f,
                                                                                                  -0.095f), new Vector2(
            0.01f, 0.03f), new Vector2(0.0005f, 0.0009f)};
    public static final String[] WAVE_TEXTURE_PATHS = {"/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds", "/textures/engine/waves.dds"}; // always same?
    private final List<Spawn> spawns = new ArrayList<>();
    private final List<Marker> mexes = new ArrayList<>();
    private final List<Marker> hydros = new ArrayList<>();
    private final List<Marker> blankMarkers = new ArrayList<>();
    private final List<DecalGroup> decalGroups = new ArrayList<>();
    private final List<Decal> decals = new ArrayList<>();
    private final List<WaveGenerator> waveGenerators = new ArrayList<>();
    private final List<Prop> props = new ArrayList<>();
    private final List<Army> armies = new ArrayList<>();
    private final List<AIMarker> landAIMarkers = new ArrayList<>();
    private final List<AIMarker> amphibiousAIMarkers = new ArrayList<>();
    private final List<AIMarker> navyAIMarkers = new ArrayList<>();
    private final List<AIMarker> airAIMarkers = new ArrayList<>();
    private final List<AIMarker> rallyMarkers = new ArrayList<>();
    private final List<AIMarker> expansionAIMarkers = new ArrayList<>();
    private final List<AIMarker> largeExpansionAIMarkers = new ArrayList<>();
    private final List<AIMarker> navalAreaAIMarkers = new ArrayList<>();
    private final List<AIMarker> navalRallyMarkers = new ArrayList<>();
    private byte @Nullable [] compressedNormal;
    private byte @Nullable [] compressedShadows;
    private float heightMapScale = 1f / 128f;
    private String name = "";
    @Setter(AccessLevel.NONE)
    private int size; // must be a power of 2 when exported. 512 equals a 10x10km Map
    private Vector4 playableArea;
    private int minorVersion = 56;
    private String description = "";
    private String terrainShaderPath = LEGACY_SHADER_NAME;
    private String backgroundPath = "/textures/environment/defaultbackground.dds";
    private boolean generatePreview = true;
    private boolean isUnexplored;
    private float noRushRadius = 50;
    private String folderName = "";
    private String filePrefix = "";
    private String script = "";
    private String skyCubePath = "/textures/environment/defaultskycube.dds";
    private Biome biome;
    private SkyBox skyBox = new SkyBox();
    // always 256 x 256 px
    private BufferedImage preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    private BufferedImage heightmap;
    private BufferedImage normalMap;
    private BufferedImage textureMasksLow;
    private BufferedImage textureMasksHigh;
    private BufferedImage waterMap;
    private BufferedImage waterFoamMap;
    private BufferedImage waterShadowMap;
    private BufferedImage waterDepthBiasMap;
    private BufferedImage terrainType;
    private @Nullable BufferedImage mapInfoTexture;
    private @Nullable BufferedImage mapNormalTexture;
    private int cartographicContourInterval = 100;
    private int cartographicDeepWaterColor = new Color(71, 140, 181).getRGB();
    private int cartographicMapContourColor = new Color(0, 0, 0).getRGB();
    private int cartographicMapShoreColor = new Color(141, 200, 225).getRGB();
    private int cartographicMapLandStartColor = new Color(119, 101, 108).getRGB();
    private int cartographicMapLandEndColor = new Color(206, 206, 176).getRGB();

    public SCMap(int size, Biome biome) {
        this.size = size;
        this.biome = biome;
        playableArea = new Vector4(0, 0, size, size);

        heightmap = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_USHORT_GRAY);
        normalMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        textureMasksLow = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_INT_ARGB);
        textureMasksHigh = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_INT_ARGB);

        waterMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_INT_ARGB);
        waterFoamMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
        waterShadowMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < size / 2; y++) {
            for (int x = 0; x < size / 2; x++) {
                waterShadowMap.getRaster().setPixel(x, y, new int[]{255});
            }
        }
        waterDepthBiasMap = new BufferedImage(size / 2, size / 2, BufferedImage.TYPE_BYTE_GRAY);

        terrainType = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
    }

    private static void checkImageSize(BufferedImage image, int size) {
        if (image.getWidth() != size) {
            throw new IllegalArgumentException("Image size does not match required size: Image size is " +
                                               image.getWidth() +
                                               " required size is " +
                                               size);
        }
    }

    private static void checkMaskSize(Mask<?, ?> mask, int size) {
        if (mask.getSize() != size) {
            throw new IllegalArgumentException("Image size does not match required size: Image size is " +
                                               mask.getSize() +
                                               " required size is " +
                                               size);
        }
    }

    public void setPreview(BufferedImage preview) {
        checkImageSize(preview, 256);
        this.preview = preview;
    }

    public @Nullable AIMarker getAmphibiousMarker(String id) {
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

    public @Nullable Army getArmy(String id) {
        return armies.stream().filter(army -> army.getId().equals(id)).findFirst().orElse(null);
    }

    public void addArmy(Army army) {
        armies.add(army);
    }

    public void setArmyOrder(SequencedCollection<String> armyIds) {
        Map<String, Spawn> spawnsById = spawns.stream()
                                              .collect(Collectors.toMap(Spawn::getId, Function.identity()));
        spawns.clear();
        armyIds.forEach(armyId -> spawns.add(spawnsById.get(armyId)));
    }

    public int getBlankCount() {
        return blankMarkers.size();
    }

    public Marker getBlank(int i) {
        return blankMarkers.get(i);
    }

    public @Nullable Marker getBlank(String id) {
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

    public @Nullable AIMarker getLandMarker(String id) {
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

    public void changeStratumSize(int stratumSize) {
        textureMasksHigh = scaleImage(textureMasksHigh, stratumSize, stratumSize);
        textureMasksLow = scaleImage(textureMasksLow, stratumSize, stratumSize);
    }

    public void rotateMap(float radians) {
        if ((radians / (float) (StrictMath.PI * 2)) % 1 == 0) {
            return;
        }

        rotateMapContent(radians);
        rotateObjects(radians);
    }

    private void rotateMapContent(float radians) {
        rotateBiome(radians);

        heightmap = rotateImage(heightmap, radians);
        normalMap = rotateImage(normalMap, radians);
        waterMap = rotateImage(waterMap, radians);
        waterFoamMap = rotateImage(waterFoamMap, radians);
        waterShadowMap = rotateImage(waterShadowMap, radians);
        waterDepthBiasMap = rotateImage(waterDepthBiasMap, radians);
        terrainType = rotateImage(terrainType, radians);
        textureMasksHigh = rotateImage(textureMasksHigh, radians);
        textureMasksLow = rotateImage(textureMasksLow, radians);
        preview = rotateImage(preview, radians);
        if (mapNormalTexture != null) {
            mapNormalTexture = rotateImage(mapNormalTexture, radians);
        }

        if (mapInfoTexture != null) {
            mapInfoTexture = rotateImage(mapInfoTexture, radians);
        }
    }

    private void rotateBiome(float radians) {
        WaterSettings oldWaterSettings = this.biome.waterSettings();
        WaterSettings newWaterSettings = new WaterSettings(oldWaterSettings.waterPresent(),
                                                           oldWaterSettings.elevation(),
                                                           oldWaterSettings.elevationDeep(),
                                                           oldWaterSettings.elevationAbyss(),
                                                           oldWaterSettings.surfaceColor(),
                                                           oldWaterSettings.colorLerp(),
                                                           oldWaterSettings.refractionScale(),
                                                           oldWaterSettings.fresnelBias(),
                                                           oldWaterSettings.fresnelPower(),
                                                           oldWaterSettings.unitReflection(),
                                                           oldWaterSettings.skyReflection(),
                                                           oldWaterSettings.sunShininess(),
                                                           oldWaterSettings.sunStrength(),
                                                           oldWaterSettings.sunDirection().rotateXZ(radians),
                                                           oldWaterSettings.sunColor(),
                                                           oldWaterSettings.sunReflection(), oldWaterSettings.sunGlow(),
                                                           oldWaterSettings.texPathCubemap(),
                                                           oldWaterSettings.texPathWaterRamp(),
                                                           oldWaterSettings.waveTextures()
                                                                           .stream()
                                                                           .map(waveTexture -> new WaterSettings.WaveTexture(
                                                                                   waveTexture.normalMovement()
                                                                                              .rotate(radians),
                                                                                   waveTexture.texPath(),
                                                                                   waveTexture.normalRepeat()))
                                                                           .toList());

        LightingSettings oldLightingSettings = this.biome.lightingSettings();
        LightingSettings newLightingSettings = new LightingSettings(oldLightingSettings.lightingMultiplier(),
                                                                    oldLightingSettings.sunDirection()
                                                                                       .rotateXZ(radians),
                                                                    oldLightingSettings.sunAmbience(),
                                                                    oldLightingSettings.sunColor(),
                                                                    oldLightingSettings.shadowFillColor(),
                                                                    oldLightingSettings.specularColor(),
                                                                    oldLightingSettings.bloom(),
                                                                    oldLightingSettings.fogColor(),
                                                                    oldLightingSettings.fogStart(),
                                                                    oldLightingSettings.fogEnd());

        this.biome = new Biome(this.biome.name(), this.biome.terrainMaterials(), this.biome.propMaterials(),
                               this.biome.decalMaterials(), newWaterSettings, newLightingSettings);
    }

    private void rotateObjects(float radians) {
        rotateObjects(spawns, radians);
        rotateObjects(airAIMarkers, radians);
        rotateObjects(amphibiousAIMarkers, radians);
        rotateObjects(expansionAIMarkers, radians);
        rotateObjects(largeExpansionAIMarkers, radians);
        rotateObjects(navalAreaAIMarkers, radians);
        rotateObjects(navyAIMarkers, radians);
        rotateObjects(landAIMarkers, radians);
        rotateObjects(navalRallyMarkers, radians);
        rotateObjects(rallyMarkers, radians);
        rotateObjects(blankMarkers, radians);
        rotateObjects(hydros, radians);
        rotateObjects(mexes, radians);
        rotateObjects(props, radians);
        rotateObjects(decals, radians);
        rotateObjects(waveGenerators, radians);
        spawns.forEach(spawn -> spawn.setNoRushOffset(spawn.getNoRushOffset().rotate(radians)));
        armies.forEach(
                army -> army.getGroups().forEach(group -> {
                    rotateObjects(group.getUnits(), radians);
                    group.getUnits().forEach(unit -> unit.setRotation(unit.getRotation() - radians));
                }));

        props.forEach(prop -> {
            prop.setRotation(prop.getRotation() - radians);
        });

        decals.forEach(decal -> {
            decal.setRotation(decal.getRotation().rotateXZ(radians));
            decal.setScale(decal.getScale().rotateXZ(radians));
        });

        waveGenerators.forEach(waveGenerator -> {
            waveGenerator.setRotation(waveGenerator.getRotation() - radians);
            waveGenerator.setVelocity(waveGenerator.getVelocity().rotateXZ(radians));
        });

        setHeights();
    }

    private <T extends PositionedObject> void rotateObjects(Collection<T> positionedObjects, float radians) {
        Vector2 halfPoint = new Vector2(size / 2f, size / 2f);

        Collection<T> repositionedObjects = new ArrayList<>();
        positionedObjects.forEach(positionedObject -> {
            Vector2 newPosition = new Vector2(positionedObject.getPosition()).subtract(halfPoint)
                                                                             .rotate(radians)
                                                                             .add(halfPoint);
            positionedObject.setPosition(new Vector3(newPosition));
            if (ImageUtil.inImageBounds(newPosition, heightmap)) {
                repositionedObjects.add(positionedObject);
            }
        });
        positionedObjects.clear();
        positionedObjects.addAll(repositionedObjects);
    }

    public void changeMapSize(int contentSize, int boundsSize, Vector2 boundOffset) {
        int oldSize = size;
        Vector2 topLeftOffset = new Vector2(boundOffset.x() - (float) contentSize / 2,
                                            boundOffset.y() - (float) contentSize / 2);
        float contentScale = (float) contentSize / (float) oldSize;
        float boundsScale = (float) boundsSize / (float) contentSize;

        if (contentScale != 1) {
            scaleMapContent(contentScale);
            this.size = contentSize;
            playableArea = playableArea.multiply(contentScale);
        }

        if (boundsScale != 1 && topLeftOffset.x() != 0 && topLeftOffset.y() != 0) {
            scaleMapBounds(boundsScale, topLeftOffset);
            playableArea = playableArea.add(topLeftOffset.x(), topLeftOffset.y(), topLeftOffset.x(), topLeftOffset.y());
            this.size = boundsSize;
        }

        if (contentScale != 1 || (boundsScale != 1 && topLeftOffset.x() != 0 && topLeftOffset.y() != 0)) {
            moveObjects(contentScale, topLeftOffset);
        }
    }

    private void scaleMapContent(float contentScale) {
        scaleBiome(contentScale);

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
        waterShadowMap = scaleImage(waterShadowMap, StrictMath.round(waterShadowMap.getWidth() * contentScale),
                                    StrictMath.round(waterShadowMap.getHeight() * contentScale));
        waterDepthBiasMap = scaleImage(waterDepthBiasMap, StrictMath.round(waterDepthBiasMap.getWidth() * contentScale),
                                       StrictMath.round(waterDepthBiasMap.getHeight() * contentScale));
        terrainType = scaleImage(terrainType, StrictMath.round(terrainType.getWidth() * contentScale),
                                 StrictMath.round(terrainType.getHeight() * contentScale));
        textureMasksHigh = scaleImage(textureMasksHigh, StrictMath.round(textureMasksHigh.getWidth() * contentScale),
                                      StrictMath.round(textureMasksHigh.getHeight() * contentScale));
        textureMasksLow = scaleImage(textureMasksLow, StrictMath.round(textureMasksLow.getWidth() * contentScale),
                                     StrictMath.round(textureMasksLow.getHeight() * contentScale));
        if (mapNormalTexture != null) {
            mapNormalTexture = scaleImage(mapNormalTexture,
                                          StrictMath.round(mapNormalTexture.getWidth() * contentScale),
                                          StrictMath.round(mapNormalTexture.getHeight() * contentScale));
        }

        if (mapInfoTexture != null) {
            mapInfoTexture = scaleImage(mapInfoTexture, StrictMath.round(mapInfoTexture.getWidth() * contentScale),
                                        StrictMath.round(mapInfoTexture.getHeight() * contentScale));
        }
    }

    private void scaleBiome(float contentScale) {
        WaterSettings oldWaterSettings = this.biome.waterSettings();
        WaterSettings newWaterSettings = new WaterSettings(oldWaterSettings.waterPresent(),
                                                           oldWaterSettings.elevation() * contentScale,
                                                           oldWaterSettings.elevationDeep() * contentScale,
                                                           oldWaterSettings.elevationAbyss() * contentScale,
                                                           oldWaterSettings.surfaceColor(),
                                                           oldWaterSettings.colorLerp(),
                                                           oldWaterSettings.refractionScale(),
                                                           oldWaterSettings.fresnelBias(),
                                                           oldWaterSettings.fresnelPower(),
                                                           oldWaterSettings.unitReflection(),
                                                           oldWaterSettings.skyReflection(),
                                                           oldWaterSettings.sunShininess(),
                                                           oldWaterSettings.sunStrength(),
                                                           oldWaterSettings.sunDirection(), oldWaterSettings.sunColor(),
                                                           oldWaterSettings.sunReflection(), oldWaterSettings.sunGlow(),
                                                           oldWaterSettings.texPathCubemap(),
                                                           oldWaterSettings.texPathWaterRamp(),
                                                           oldWaterSettings.waveTextures());

        this.biome = new Biome(this.biome.name(), this.biome.terrainMaterials(), this.biome.propMaterials(),
                               this.biome.decalMaterials(), newWaterSettings, this.biome.lightingSettings());
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
                                                  topLeftOffset.multiply(normalMapScale));
        waterMap = insertImageIntoNewImageOfSize(waterMap, StrictMath.round(waterMap.getWidth() * boundsScale),
                                                 StrictMath.round(waterMap.getHeight() * boundsScale),
                                                 topLeftOffset.multiply(waterMapScale));
        Vector2 halvedTopLeftOffset = topLeftOffset.multiply(.5f);
        waterFoamMap = insertImageIntoNewImageOfSize(waterFoamMap,
                                                     StrictMath.round(waterFoamMap.getWidth() * boundsScale),
                                                     StrictMath.round(waterFoamMap.getHeight() * boundsScale),
                                                     halvedTopLeftOffset);
        waterShadowMap = insertImageIntoNewImageOfSize(waterShadowMap,
                                                       StrictMath.round(waterShadowMap.getWidth() * boundsScale),
                                                       StrictMath.round(waterShadowMap.getHeight() * boundsScale),
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
                                                         topLeftOffset.multiply(textureMaskHighScale));
        textureMasksLow = insertImageIntoNewImageOfSize(textureMasksLow,
                                                        StrictMath.round(textureMasksLow.getWidth() * boundsScale),
                                                        StrictMath.round(textureMasksLow.getHeight() * boundsScale),
                                                        topLeftOffset.multiply(textureMaskLowScale));
        if (mapNormalTexture != null) {
            float mapNormalTextureScale = (float) mapNormalTexture.getWidth() / size;
            mapNormalTexture = insertImageIntoNewImageOfSize(mapNormalTexture, StrictMath.round(
                    mapNormalTexture.getWidth() * boundsScale), StrictMath.round(
                    mapNormalTexture.getHeight() * boundsScale), topLeftOffset.multiply(mapNormalTextureScale));
        }
        if (mapInfoTexture != null) {
            float mapInfoTextureScale = (float) mapInfoTexture.getWidth() / size;
            mapInfoTexture = insertImageIntoNewImageOfSize(mapInfoTexture,
                                                           StrictMath.round(mapInfoTexture.getWidth() * boundsScale),
                                                           StrictMath.round(mapInfoTexture.getHeight() * boundsScale),
                                                           topLeftOffset.multiply(mapInfoTextureScale));
        }
    }

    private void moveObjects(float contentScale, Vector2 offset) {
        repositionObjects(spawns, contentScale, offset);
        repositionObjects(airAIMarkers, contentScale, offset);
        repositionObjects(amphibiousAIMarkers, contentScale, offset);
        repositionObjects(expansionAIMarkers, contentScale, offset);
        repositionObjects(largeExpansionAIMarkers, contentScale, offset);
        repositionObjects(navalAreaAIMarkers, contentScale, offset);
        repositionObjects(navyAIMarkers, contentScale, offset);
        repositionObjects(landAIMarkers, contentScale, offset);
        repositionObjects(navalRallyMarkers, contentScale, offset);
        repositionObjects(rallyMarkers, contentScale, offset);
        repositionObjects(blankMarkers, contentScale, offset);
        repositionObjects(hydros, contentScale, offset);
        repositionObjects(mexes, contentScale, offset);
        repositionObjects(props, contentScale, offset);
        repositionObjects(decals, contentScale, offset);
        repositionObjects(waveGenerators, contentScale, offset);
        armies.forEach(
                army -> army.getGroups().forEach(group -> repositionObjects(group.getUnits(), contentScale, offset)));

        decals.forEach(decal -> {
            Vector3 scale = decal.getScale();
            decal.setScale(new Vector3(scale.x() * contentScale, scale.y(), scale.z() * contentScale));
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
                positionedObject.setPosition(new Vector3(position.x(), heightmap.getRaster()
                                                                                .getPixel((int) position.x(),
                                                                                          (int) position.y(),
                                                                                          new int[]{0})[0] *
                                                                       heightMapScale, position.y()));
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


    public void addAmphibiousMarker(AIMarker aiMarker) {
        amphibiousAIMarkers.add(aiMarker);
    }

    public int getNavyMarkerCount() {
        return navyAIMarkers.size();
    }

    public AIMarker getNavyMarker(int i) {
        return navyAIMarkers.get(i);
    }

    public @Nullable AIMarker getNavyMarker(String id) {
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

    public @Nullable AIMarker getAirMarker(String id) {
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

    public void setWaterShadowMap(BufferedImage waterShadowMap) {
        checkImageSize(waterShadowMap, size / 2);
        this.waterShadowMap = waterShadowMap;
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

    public void setTextureMasksScaled(BufferedImage textureMasks, Vector4Mask mask) {
        int textureMasksWidth = textureMasks.getWidth();
        checkMaskSize(mask, textureMasksWidth);
        for (int x = 0; x < textureMasksWidth; x++) {
            for (int y = 0; y < textureMasksWidth; y++) {
                Vector4 vector = mask.get(x, y);
                int val0 = convertToRawTextureValue(vector.x());
                int val1 = convertToRawTextureValue(vector.y());
                int val2 = convertToRawTextureValue(vector.z());
                int val3 = convertToRawTextureValue(vector.w());
                textureMasks.getRaster().setPixel(x, y, new int[]{val0, val1, val2, val3});
            }
        }
    }

    public void setTerrainType(BufferedImage terrainType, IntegerMask mask) {
        int terrainTypeWidth = terrainType.getWidth();
        checkMaskSize(mask, terrainTypeWidth);
        for (int x = 0; x < terrainTypeWidth; x++) {
            for (int y = 0; y < terrainTypeWidth; y++) {
                int val0 = mask.getPrimitive(x, y);
                terrainType.getRaster().setPixel(x, y, new int[]{val0});
            }
        }
    }

    public void setWaterShadowMap(BufferedImage waterShadowMap, FloatMask mask) {
        int waterShadowMapWidth = waterShadowMap.getWidth();
        checkMaskSize(mask, waterShadowMapWidth);
        for (int x = 0; x < waterShadowMapWidth; x++) {
            for (int y = 0; y < waterShadowMapWidth; y++) {
                int val0 = (int) (mask.getPrimitive(x, y) * 255);
                waterShadowMap.getRaster().setPixel(x, y, new int[]{val0});
            }
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
        stringBuilder.append(String.format("Biome: %s%n", biome.name()));
        stringBuilder.append(String.format("%s%n", biome.lightingSettings()));
        stringBuilder.append(String.format("%s%n", biome.waterSettings()));
        stringBuilder.append(String.format("Terrain Materials: %s%n", biome.terrainMaterials()));
        stringBuilder.append(String.format("Size: %d%n", size));
        int numDecals = decals.size();
        for (int i = 0; i < numDecals; i++) {
            stringBuilder.append(String.format("Decal %d: %s%n", i, decals.get(i)));
        }
        int numProps = props.size();
        for (int i = 0; i < numProps; i++) {
            stringBuilder.append(String.format("Prop %d: %s%n", i, props.get(i)));
        }

        return stringBuilder.toString();
    }
}
