package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.importer.MapImporter;
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
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.toolsuite.cli.SourceCompletionCandidates;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(name = "force", mixinStandardHelpOptions = true, description = "Force symmetry on a map", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapForcer implements Callable<Integer> {
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @Mixin
    private OutputFolderMixin outputFolderMixin;
    @Mixin
    private DebugMixin debugMixin;
    @Option(names = "--symmetry", description = "Symmetry to force on the map. Values: ${COMPLETION-CANDIDATES}")
    private Symmetry symmetry;
    @Option(names = "--source", description = "Which part of the map to use as the base. Values: ${COMPLETION-CANDIDATES}", completionCandidates = SourceCompletionCandidates.class)
    private String source;
    private IntegerMask heightMask;
    private boolean useAngle;
    private boolean reverseSide;
    private float angle;

    @Override
    public Integer call() throws Exception {
        System.out.printf("Forcing map symmetry to `%s` on `%s`%n", symmetry, requiredMapPathMixin.getMapPath());
        SymmetrySettings symmetrySettings = getSymmetrySettings();
        SCMap map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        forceSymmetry(map, symmetrySettings);
        MapExporter.exportMap(outputFolderMixin.getOutputPath(), map, true);
        return 0;
    }

    private SymmetrySettings getSymmetrySettings() {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        if (pattern.matcher(source).matches()) {
            angle = (360f - Float.parseFloat("source")) % 360f;
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
                switch (SymmetrySource.valueOf(source)) {
                    case BOTTOM, BOTTOM_RIGHT, BOTTOM_LEFT, RIGHT -> reverseSide = true;
                    default -> reverseSide = false;
                }
            }
        }
        return new SymmetrySettings(symmetry, symmetry, symmetry);
    }

    private void forceSymmetry(SCMap map, SymmetrySettings symmetrySettings) {
        heightMask = new IntegerMask(map.getHeightmap(), null, symmetrySettings, "heightMask");

        forceTerrain(map);
        forceWaveGenerators(map.getWaveGenerators());
        forceMarkers(map.getBlankMarkers());
        forceAIMarkers(map.getAirAIMarkers());
        forceAIMarkers(map.getLandAIMarkers());
        forceAIMarkers(map.getNavyAIMarkers());
        forceAIMarkers(map.getAmphibiousAIMarkers());
        forceAIMarkers(map.getRallyMarkers());
        forceAIMarkers(map.getExpansionAIMarkers());
        forceAIMarkers(map.getLargeExpansionAIMarkers());
        forceAIMarkers(map.getNavalAreaAIMarkers());
        forceAIMarkers(map.getNavalRallyMarkers());
        forceSpawns(map.getSpawns());
        forceMarkers(map.getMexes());
        forceMarkers(map.getHydros());
        forceProps(map.getProps());
        map.getArmies().forEach(this::forceArmy);

        if (symmetrySettings.getSpawnSymmetry().equals(Symmetry.POINT2)) {
            forceDecals(map.getDecals());
        }

        map.setHeights();
    }

    private void forceTerrain(SCMap map) {
        SymmetrySettings symmetrySettings = heightMask.getSymmetrySettings();
        IntegerMask previewMask = new IntegerMask(map.getPreview(), null, symmetrySettings, "preview");
        IntegerMask normalMask = new IntegerMask(map.getNormalMap(), null, symmetrySettings, "normal");
        IntegerMask waterMask = new IntegerMask(map.getWaterMap(), null, symmetrySettings, "water");
        IntegerMask waterFoamMask = new IntegerMask(map.getWaterFoamMap(), null, symmetrySettings, "waterFoam");
        IntegerMask waterShadowMask = new IntegerMask(map.getWaterShadowMap(), null, symmetrySettings,
                                                        "waterFlatness");
        IntegerMask waterDepthBiasMask = new IntegerMask(map.getWaterDepthBiasMap(), null, symmetrySettings,
                                                         "waterDepthBias");
        IntegerMask terrainTypeMask = new IntegerMask(map.getTerrainType(), null, symmetrySettings, "terrainType");
        IntegerMask textureMasksLowMask = new IntegerMask(map.getTextureMasksLow(), null, symmetrySettings,
                                                          "textureMasksLow");
        IntegerMask textureMasksHighMask = new IntegerMask(map.getTextureMasksHigh(), null, symmetrySettings,
                                                           "textureMasksHigh");

        if (!useAngle) {
            previewMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            normalMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterFoamMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
            waterShadowMask.forceSymmetry(SymmetryType.SPAWN, reverseSide);
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
            waterShadowMask.forceSymmetry(angle);
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
        waterShadowMask.writeToImage(map.getWaterShadowMap());
        waterDepthBiasMask.writeToImage(map.getWaterDepthBiasMap());
        terrainTypeMask.writeToImage(map.getTerrainType());
        textureMasksLowMask.writeToImage(map.getTextureMasksLow());
        textureMasksHighMask.writeToImage(map.getTextureMasksHigh());
        heightMask.writeToImage(map.getHeightmap());
    }

    private void forceWaveGenerators(Collection<WaveGenerator> waveGenerators) {
        Collection<WaveGenerator> forceedWaveGenerators = new ArrayList<>();
        waveGenerators.forEach(waveGenerator -> {
            if (inSourceRegion(waveGenerator.getPosition())) {
                forceedWaveGenerators.add(new WaveGenerator(waveGenerator.getTextureName(), waveGenerator.getRampName(),
                                                            waveGenerator.getPosition(), waveGenerator.getRotation(),
                                                            waveGenerator.getVelocity()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(waveGenerator.getPosition(),
                                                                                           SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(waveGenerator.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3 newPosition = new Vector3(symmetryPoints.get(i));
                    newPosition.setY(waveGenerator.getPosition().getY());
                    forceedWaveGenerators.add(
                            new WaveGenerator(waveGenerator.getTextureName(), waveGenerator.getRampName(), newPosition,
                                              symmetryRotation.get(i), waveGenerator.getVelocity()));
                }
            }
        });
        waveGenerators.clear();
        waveGenerators.addAll(forceedWaveGenerators);
    }

    private boolean inSourceRegion(Vector3 position) {
        return (!useAngle && heightMask.inTeamNoBounds(position, reverseSide)) || (useAngle
                                                                                   && heightMask.inHalfNoBounds(
                position, angle));
    }

    private void forceMarkers(Collection<Marker> markers) {
        Collection<Marker> forceedMarkers = new ArrayList<>();
        markers.forEach(marker -> {
            if (inSourceRegion(marker.getPosition())) {
                forceedMarkers.add(new Marker(marker.getId(), marker.getPosition()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(marker.getPosition(),
                                                                                           SymmetryType.SPAWN);
                symmetryPoints.forEach(
                        symmetryPoint -> forceedMarkers.add(new Marker(marker.getId() + " sym", symmetryPoint)));
            }
        });
        if (markers.size() == forceedMarkers.size()) {
            matchToClosestMarkers(markers, forceedMarkers);
        }
        markers.clear();
        markers.addAll(forceedMarkers);
    }

    private void matchToClosestMarkers(Collection<? extends Marker> sourceMarkers,
                                       Collection<? extends Marker> destMarkers) {
        List<Marker> sourceMarkersCopy = new ArrayList<>(sourceMarkers);
        for (Marker destMarker : destMarkers) {
            Marker matchingMarker = sourceMarkersCopy.stream()
                                                     .min(Comparator.comparingDouble(
                                                             sourceMarker -> sourceMarker.getPosition()
                                                                                         .getXZDistance(
                                                                                                 destMarker.getPosition())))
                                                     .orElseThrow(() -> new IndexOutOfBoundsException(
                                                             "List sizes are different"));
            sourceMarkersCopy.remove(matchingMarker);
            destMarker.setId(matchingMarker.getId());
        }
    }

    private void forceAIMarkers(Collection<AIMarker> aiMarkers) {
        Collection<AIMarker> forceedAImarkers = new ArrayList<>();
        aiMarkers.forEach(aiMarker -> {
            if (inSourceRegion(aiMarker.getPosition())) {
                forceedAImarkers.add(new AIMarker(aiMarker.getId(), aiMarker.getPosition(), aiMarker.getNeighbors()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(aiMarker.getPosition(),
                                                                                           SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> {
                    LinkedHashSet<String> newNeighbors = new LinkedHashSet<>();
                    aiMarker.getNeighbors()
                            .forEach(marker -> newNeighbors.add(
                                    String.format(marker + "s%d", symmetryPoints.indexOf(symmetryPoint))));
                    forceedAImarkers.add(
                            new AIMarker(String.format(aiMarker.getId() + "s%d", symmetryPoints.indexOf(symmetryPoint)),
                                         symmetryPoint, newNeighbors));
                });
            }
        });
        if (aiMarkers.size() == forceedAImarkers.size()) {
            matchToClosestMarkers(aiMarkers, forceedAImarkers);
        }
        aiMarkers.clear();
        aiMarkers.addAll(forceedAImarkers);
    }

    private void forceSpawns(Collection<Spawn> spawns) {
        List<Spawn> forceedSpawns = new ArrayList<>();
        spawns.forEach(spawn -> {
            if (inSourceRegion(spawn.getPosition())) {
                forceedSpawns.add(new Spawn("", spawn.getPosition(), spawn.getNoRushOffset(), 0));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(spawn.getPosition(),
                                                                                           SymmetryType.SPAWN);
                for (int i = 0; i < symmetryPoints.size(); ++i) {
                    Vector2 symmetryPoint = symmetryPoints.get(i);
                    Vector2 symmetricNoRushOffset = new Vector2(spawn.getNoRushOffset());
                    if (!heightMask.inTeam(symmetryPoint, false)) {
                        symmetricNoRushOffset.flip(new Vector2(0, 0),
                                                   heightMask.getSymmetrySettings().getSpawnSymmetry());
                    }
                    forceedSpawns.add(new Spawn("", symmetryPoint, symmetricNoRushOffset, i + 1));
                }
            }
        });
        if (spawns.size() == forceedSpawns.size()) {
            matchToClosestMarkers(spawns, forceedSpawns);
        } else {
            forceedSpawns.forEach(spawn -> spawn.setId("ARMY_" + forceedSpawns.indexOf(spawn)));
        }
        forceedSpawns.sort(Comparator.comparingInt(spawn -> Integer.parseInt(spawn.getId().replace("ARMY_", ""))));
        spawns.clear();
        spawns.addAll(forceedSpawns);
    }

    private void forceProps(Collection<Prop> props) {
        Collection<Prop> forceedProps = new ArrayList<>();
        props.forEach(prop -> {
            if (inSourceRegion(prop.getPosition())) {
                forceedProps.add(new Prop(prop.getPath(), prop.getPosition(), prop.getRotation()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(prop.getPosition(),
                                                                                           SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    forceedProps.add(new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            }
        });
        props.clear();
        props.addAll(forceedProps);
    }

    private void forceArmy(Army army) {
        army.getGroups().forEach(this::forceGroup);
    }

    private void forceGroup(Group group) {
        forceUnits(group.getUnits());
    }

    private void forceUnits(Collection<Unit> units) {
        Collection<Unit> forceedUnits = new ArrayList<>();
        units.forEach(unit -> {
            if (inSourceRegion(unit.getPosition())) {
                forceedUnits.add(new Unit(unit.getId(), unit.getType(), unit.getPosition(), unit.getRotation()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(unit.getPosition(),
                                                                                           SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightMask.getSymmetryRotation(unit.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    forceedUnits.add(new Unit(unit.getId() + " sym", unit.getType(), symmetryPoints.get(i),
                                              symmetryRotation.get(i)));
                }
            }
        });
        units.clear();
        units.addAll(forceedUnits);
    }

    private void forceDecals(Collection<Decal> decals) {
        Collection<Decal> forceedDecals = new ArrayList<>();
        decals.forEach(decal -> {
            if (inSourceRegion(decal.getPosition())) {
                forceedDecals.add(new Decal(decal.getPath(), decal.getPosition(), decal.getRotation(), decal.getScale(),
                                            decal.getCutOffLOD()));
                List<Vector2> symmetryPoints = heightMask.getSymmetryPointsWithOutOfBounds(decal.getPosition(),
                                                                                           SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightMask.getSymmetryRotation(decal.getRotation().getY());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3 symVectorRotation = new Vector3(decal.getRotation().getX(), symmetryRotation.get(i),
                                                            decal.getRotation().getZ());
                    forceedDecals.add(new Decal(decal.getPath(), new Vector3(symmetryPoints.get(i)), symVectorRotation,
                                                decal.getScale(), decal.getCutOffLOD()));
                }
            }
        });
        decals.clear();
        decals.addAll(forceedDecals);
    }
}
