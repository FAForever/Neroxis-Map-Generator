package com.faforever.neroxis.transformer;

import com.faforever.neroxis.map.AIMarker;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Decal;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetrySource;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.map.WaveGenerator;
import com.faforever.neroxis.map.exporter.MapExporter;
import com.faforever.neroxis.map.importer.MapImporter;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.util.ArgumentParser;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public strictfp class MapTransformer {

    private Path inMapPath;
    private Path outFolderPath;
    private SCMap map;
    private String source;

    //masks used in transformation
    private IntegerMask heightMask;

    private boolean transformResources;
    private boolean transformProps;
    private boolean transformUnits;
    private boolean transformDecals;
    private boolean transformTerrain;
    private boolean useAngle;
    private boolean reverseSide;
    private float angle;
    private SymmetrySettings symmetrySettings;
    private int resize;
    private int mapBounds;
    private boolean resizeSet = false;
    private boolean mapBoundsSet = false;
    private int shiftX;
    private int shiftZ;
    private boolean shiftXSet = false;
    private boolean shiftZSet = false;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.ROOT);

        MapTransformer transformer = new MapTransformer();

        transformer.interpretArguments(args);
        if (transformer.inMapPath == null) {
            return;
        }

        System.out.println("Transforming map " + transformer.inMapPath);
        transformer.importMap();
        transformer.transform();
        transformer.exportMap();
        System.out.println("Saving map to " + transformer.outFolderPath.toAbsolutePath());
        System.out.println("Symmetry: " + transformer.symmetrySettings.getTerrainSymmetry());
        System.out.println("Source: " + transformer.source);
        System.out.println("Done");
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-transformer usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-folder-path arg   required, set the input folder for the map\n" +
                    "--out-folder-path arg  required, set the output folder for the transformed map\n" +
                    "--symmetry arg         optional, set the symmetry for the map(" + Arrays.toString(Symmetry.values()) + ")\n" +
                    "--source arg           required for symmetry, set which half to use as base for forced symmetry (" + Arrays.toString(SymmetrySource.values()) + ", {ANGLE})\n" +
                    "--marker               optional, force spawn, mex, hydro, and ai marker symmetry\n" +
                    "--props                optional, force prop symmetry\n" +
                    "--decals               optional, force decal symmetry\n" +
                    "--units                optional, force unit symmetry\n" +
                    "--terrain              optional, force terrain symmetry\n" +
                    "--all                  optional, force symmetry for all components\n" +
                    "--resize arg           optional, resize the whole map's placement of features/details to arg size (512 = 10 km x 10 km map)\n" +
                    "--map-size arg         optional, resize the map bounds to arg size (512 = 10 km x 10 km map) *Must be a power of 2\n" +
                    "--x arg                optional, set arg x-coordinate for the center of the map's placement of features/details\n" +
                    "--z arg                optional, set arg z-coordinate for the center of the map's placement of features/details\n" +
                    "--debug                optional, turn on debugging options\n");
            return;
        }

        if (arguments.containsKey("debug")) {
            DebugUtil.DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            return;
        }

        if (!arguments.containsKey("out-folder-path")) {
            System.out.println("Output Folder not Specified");
            return;
        }

        if (arguments.containsKey("symmetry") && !arguments.containsKey("source")) {
            System.out.println("Source not Specified");
            return;
        }

        if (arguments.containsKey("resize")) {
            resize = Integer.parseInt(arguments.get("resize"));
            resizeSet = true;
        }

        if (arguments.containsKey("map-size")) {
            mapBounds = Integer.parseInt(arguments.get("map-size"));
            mapBoundsSet = true;
        }

        if (arguments.containsKey("x")) {
            shiftX = Integer.parseInt(arguments.get("x"));
            shiftXSet = true;
        }

        if (arguments.containsKey("z")) {
            shiftZ = Integer.parseInt(arguments.get("z"));
            shiftZSet = true;
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        if (arguments.containsKey("symmetry")) {
            Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
            Symmetry symmetry = Symmetry.valueOf(arguments.get("symmetry"));
            source = arguments.get("source");
            if (pattern.matcher(source).matches()) {
                angle = (360f - Float.parseFloat(arguments.get("source"))) % 360f;
                useAngle = true;
            } else {
                if (symmetry == Symmetry.POINT2) {
                    useAngle = true;
                    switch (SymmetrySource.valueOf(source)) {
                        case TOP -> angle = 270;
                        case BOTTOM -> angle = 90;
                        case LEFT -> angle = 180;
                        case TOP_LEFT -> angle = 225;
                        case TOP_RIGHT -> angle = 315;
                        case BOTTOM_LEFT -> angle = 135;
                        case BOTTOM_RIGHT -> angle = 45;
                        default -> angle = 0;
                    }
                } else {
                    useAngle = false;
                    switch (SymmetrySource.valueOf(arguments.get("source"))) {
                        case BOTTOM, BOTTOM_RIGHT, BOTTOM_LEFT, RIGHT -> reverseSide = true;
                        default -> reverseSide = false;
                    }
                }
            }
            symmetrySettings = new SymmetrySettings(symmetry, symmetry, symmetry);
            transformResources = arguments.containsKey("resources") || arguments.containsKey("all");
            transformProps = arguments.containsKey("props") || arguments.containsKey("all");
            transformDecals = arguments.containsKey("decals") || arguments.containsKey("all");
            transformUnits = arguments.containsKey("units") || arguments.containsKey("all");
            transformTerrain = arguments.containsKey("terrain") || arguments.containsKey("all");

            if (transformDecals && !symmetrySettings.getSpawnSymmetry().equals(Symmetry.POINT2)) {
                System.out.println("This tool does not yet mirror decals");
            }
        } else {
            symmetrySettings = new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE);
        }
    }

    public void importMap() {
        try {
            map = MapImporter.importMap(inMapPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    public void exportMap() {
        long startTime = System.currentTimeMillis();
        MapExporter.exportMap(outFolderPath, map, true, false);
        System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);
    }

    public void transform() {
        if (symmetrySettings != null) {
            transformSymmetry();
        }

        if (mapBoundsSet || resizeSet) {
            transformSize();
        }
    }

    private void transformSize() {
        int mapSize = map.getSize();

        resize = resizeSet ? resize : mapSize;
        mapBounds = mapBoundsSet ? mapBounds : resize;
        shiftX = shiftXSet ? shiftX : mapBounds / 2;
        shiftZ = shiftZSet ? shiftZ : mapBounds / 2;

        if (mapBounds != mapSize || resize != mapSize) {
            map.changeMapSize(resize, mapBounds, new Vector2(shiftX, shiftZ));
        }
    }

    private void transformSymmetry() {
        heightMask = new IntegerMask(map.getHeightmap(), null, symmetrySettings, "heightMask");

        if (transformTerrain) {
            transformTerrain();
            transformWaveGenerators(map.getWaveGenerators());
            transformMarkers(map.getBlankMarkers());
            transformAIMarkers(map.getAirAIMarkers());
            transformAIMarkers(map.getLandAIMarkers());
            transformAIMarkers(map.getNavyAIMarkers());
            transformAIMarkers(map.getAmphibiousAIMarkers());
            transformAIMarkers(map.getRallyMarkers());
            transformAIMarkers(map.getExpansionAIMarkers());
            transformAIMarkers(map.getLargeExpansionAIMarkers());
            transformAIMarkers(map.getNavalAreaAIMarkers());
            transformAIMarkers(map.getNavalRallyMarkers());
        }

        if (transformResources) {
            transformSpawns(map.getSpawns());
            transformMarkers(map.getMexes());
            transformMarkers(map.getHydros());
        }

        if (transformUnits) {
            transformArmies();
        }

        if (transformProps) {
            transformProps(map.getProps());
        }

        if (transformDecals && symmetrySettings.getSpawnSymmetry().equals(Symmetry.POINT2)) {
            transformDecals(map.getDecals());
        }

        map.setHeights();
    }

    private void transformTerrain() {
        IntegerMask previewMask = new IntegerMask(map.getPreview(), null, symmetrySettings, "preview");
        IntegerMask normalMask = new IntegerMask(map.getNormalMap(), null, symmetrySettings, "normal");
        IntegerMask waterMask = new IntegerMask(map.getWaterMap(), null, symmetrySettings, "water");
        IntegerMask waterFoamMask = new IntegerMask(map.getWaterFoamMap(), null, symmetrySettings, "waterFoam");
        IntegerMask waterFlatnessMask = new IntegerMask(map.getWaterFlatnessMap(), null, symmetrySettings, "waterFlatness");
        IntegerMask waterDepthBiasMask = new IntegerMask(map.getWaterDepthBiasMap(), null, symmetrySettings, "waterDepthBias");
        IntegerMask terrainTypeMask = new IntegerMask(map.getTerrainType(), null, symmetrySettings, "terrainType");
        IntegerMask textureMasksLowMask = new IntegerMask(map.getTextureMasksLow(), null, symmetrySettings, "textureMasksLow");
        IntegerMask textureMasksHighMask = new IntegerMask(map.getTextureMasksHigh(), null, symmetrySettings, "textureMasksHigh");

        if (!useAngle) {
            previewMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            normalMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterFoamMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterFlatnessMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterDepthBiasMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            terrainTypeMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            textureMasksLowMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            textureMasksHighMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            heightMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
        } else {
            previewMask.forceSymmetry(angle);
            normalMask.forceSymmetry(angle);
            waterMask.forceSymmetry(angle);
            waterFoamMask.forceSymmetry(angle);
            waterFlatnessMask.forceSymmetry(angle);
            waterDepthBiasMask.forceSymmetry(angle);
            terrainTypeMask.forceSymmetry(angle);
            textureMasksLowMask.forceSymmetry(angle);
            textureMasksHighMask.forceSymmetry(angle);
            heightMask.forceSymmetry(angle);
        }
        previewMask.writeToImage(map.getPreview());
        normalMask.writeToImage(map.getNormalMap());
        waterMask.writeToImage(map.getWaterMap());
        waterFoamMask.writeToImage(map.getWaterFoamMap());
        waterFlatnessMask.writeToImage(map.getWaterFlatnessMap());
        waterDepthBiasMask.writeToImage(map.getWaterDepthBiasMap());
        terrainTypeMask.writeToImage(map.getTerrainType());
        textureMasksLowMask.writeToImage(map.getTextureMasksLow());
        textureMasksHighMask.writeToImage(map.getTextureMasksHigh());
        heightMask.writeToImage(map.getHeightmap());
    }

    private void transformSpawns(Collection<Spawn> spawns) {
        List<Spawn> transformedSpawns = new ArrayList<>();
        spawns.forEach(spawn -> {
            if (inSourceRegion(spawn.getPosition())) {
                transformedSpawns.add(new Spawn("", spawn.getPosition(), spawn.getNoRushOffset(), 0));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(spawn.getPosition(), SymmetryType.SPAWN);
                for (int i = 0; i < symmetryPoints.size(); ++i) {
                    Vector2 symmetryPoint = symmetryPoints.get(i);
                    Vector2 symmetricNoRushOffset = new Vector2(spawn.getNoRushOffset());
                    if (!heightMask.inTeam(symmetryPoint, false)) {
                        symmetricNoRushOffset.flip(new Vector2(0, 0), heightMask.getSymmetrySettings().getSpawnSymmetry());
                    }
                    transformedSpawns.add(new Spawn("", symmetryPoint, symmetricNoRushOffset, i + 1));
                }
            }
        });
        if (spawns.size() == transformedSpawns.size()) {
            matchToClosestMarkers(spawns, transformedSpawns);
        } else {
            transformedSpawns.forEach(spawn -> spawn.setId("ARMY_" + transformedSpawns.indexOf(spawn)));
        }
        transformedSpawns.sort(Comparator.comparingInt(spawn -> Integer.parseInt(spawn.getId().replace("ARMY_", ""))));
        spawns.clear();
        spawns.addAll(transformedSpawns);
    }

    private void transformMarkers(Collection<Marker> markers) {
        Collection<Marker> transformedMarkers = new ArrayList<>();
        markers.forEach(marker -> {
            if (inSourceRegion(marker.getPosition())) {
                transformedMarkers.add(new Marker(marker.getId(), marker.getPosition()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(marker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> transformedMarkers.add(new Marker(marker.getId() + " sym", symmetryPoint)));
            }
        });
        if (markers.size() == transformedMarkers.size()) {
            matchToClosestMarkers(markers, transformedMarkers);
        }
        markers.clear();
        markers.addAll(transformedMarkers);
    }

    private void transformAIMarkers(Collection<AIMarker> aiMarkers) {
        Collection<AIMarker> transformedAImarkers = new ArrayList<>();
        aiMarkers.forEach(aiMarker -> {
            if (inSourceRegion(aiMarker.getPosition())) {
                transformedAImarkers.add(new AIMarker(aiMarker.getId(), aiMarker.getPosition(), aiMarker.getNeighbors()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(aiMarker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> {
                    LinkedHashSet<String> newNeighbors = new LinkedHashSet<>();
                    aiMarker.getNeighbors().forEach(marker -> newNeighbors.add(String.format(marker + "s%d", symmetryPoints.indexOf(symmetryPoint))));
                    transformedAImarkers.add(new AIMarker(String.format(aiMarker.getId() + "s%d", symmetryPoints.indexOf(symmetryPoint)), symmetryPoint, newNeighbors));
                });
            }
        });
        if (aiMarkers.size() == transformedAImarkers.size()) {
            matchToClosestMarkers(aiMarkers, transformedAImarkers);
        }
        aiMarkers.clear();
        aiMarkers.addAll(transformedAImarkers);
    }

    private void transformArmies() {
        map.getArmies().forEach(this::transformArmy);
    }

    private void transformArmy(Army army) {
        army.getGroups().forEach(this::transformGroup);
    }

    private void transformGroup(Group group) {
        transformUnits(group.getUnits());
    }

    private void transformUnits(Collection<Unit> units) {
        Collection<Unit> transformedUnits = new ArrayList<>();
        units.forEach(unit -> {
            if (inSourceRegion(unit.getPosition())) {
                transformedUnits.add(new Unit(unit.getId(), unit.getType(), unit.getPosition(), unit.getRotation()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(unit.getPosition(), SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightMask.getSymmetryRotation(unit.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    transformedUnits.add(new Unit(unit.getId() + " sym", unit.getType(), symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            }
        });
        units.clear();
        units.addAll(transformedUnits);
    }

    private void transformProps(Collection<Prop> props) {
        Collection<Prop> transformedProps = new ArrayList<>();
        props.forEach(prop -> {
            if (inSourceRegion(prop.getPosition())) {
                transformedProps.add(new Prop(prop.getPath(), prop.getPosition(), prop.getRotation()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(prop.getPosition(), SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    transformedProps.add(new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            }
        });
        props.clear();
        props.addAll(transformedProps);
    }

    private void transformWaveGenerators(Collection<WaveGenerator> waveGenerators) {
        Collection<WaveGenerator> transformedWaveGenerators = new ArrayList<>();
        waveGenerators.forEach(waveGenerator -> {
            if (inSourceRegion(waveGenerator.getPosition())) {
                transformedWaveGenerators.add(new WaveGenerator(waveGenerator.getTextureName(), waveGenerator.getRampName(), waveGenerator.getPosition(), waveGenerator.getRotation(), waveGenerator.getVelocity()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(waveGenerator.getPosition(), SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(waveGenerator.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3 newPosition = new Vector3(symmetryPoints.get(i));
                    newPosition.setY(waveGenerator.getPosition().getY());
                    transformedWaveGenerators.add(new WaveGenerator(waveGenerator.getTextureName(), waveGenerator.getRampName(), newPosition, symmetryRotation.get(i), waveGenerator.getVelocity()));
                }
            }
        });
        waveGenerators.clear();
        waveGenerators.addAll(transformedWaveGenerators);
    }

    private void transformDecals(Collection<Decal> decals) {
        Collection<Decal> transformedDecals = new ArrayList<>();
        decals.forEach(decal -> {
            if (inSourceRegion(decal.getPosition())) {
                transformedDecals.add(new Decal(decal.getPath(), decal.getPosition(), decal.getRotation(), decal.getScale(), decal.getCutOffLOD()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(decal.getPosition(), SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(decal.getRotation().getY());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3 symVectorRotation = new Vector3(decal.getRotation().getX(), symmetryRotation.get(i), decal.getRotation().getZ());
                    transformedDecals.add(new Decal(decal.getPath(), new Vector3(symmetryPoints.get(i)), symVectorRotation, decal.getScale(), decal.getCutOffLOD()));
                }
            }
        });
        decals.clear();
        decals.addAll(transformedDecals);
    }

    private void matchToClosestMarkers(Collection<? extends Marker> sourceMarkers, Collection<? extends Marker> destMarkers) {
        List<Marker> sourceMarkersCopy = new ArrayList<>(sourceMarkers);
        for (Marker destMarker : destMarkers) {
            Marker matchingMarker = sourceMarkersCopy.stream().min(Comparator.comparingDouble(sourceMarker -> sourceMarker.getPosition().getXZDistance(destMarker.getPosition())))
                    .orElseThrow(() -> new IndexOutOfBoundsException("List sizes are different"));
            sourceMarkersCopy.remove(matchingMarker);
            destMarker.setId(matchingMarker.getId());
        }
    }

    private boolean inSourceRegion(Vector3 position) {
        return (!useAngle && heightMask.inTeamNoBounds(position, reverseSide)) || (useAngle && heightMask.inHalfNoBounds(position, angle));
    }
}
