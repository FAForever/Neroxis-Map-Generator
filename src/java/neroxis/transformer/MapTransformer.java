package neroxis.transformer;

import neroxis.exporter.MapExporter;
import neroxis.importer.SCMapImporter;
import neroxis.importer.SaveImporter;
import neroxis.importer.ScenarioImporter;
import neroxis.map.*;
import neroxis.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public strictfp class MapTransformer {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private String mapFolder;
    private String mapName;
    private SCMap map;

    //masks used in transformation
    private FloatMask heightmapBase;

    private boolean transformResources;
    private boolean transformProps;
    private boolean transformUnits;
    private boolean transformDecals;
    private boolean transformTerrain;
    private boolean useAngle;
    private boolean reverseSide;
    private float angle;
    private SymmetrySettings symmetrySettings;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapTransformer transformer = new MapTransformer();

        transformer.interpretArguments(args);

        System.out.println("Transforming map " + transformer.inMapPath);
        transformer.importMap();
        transformer.transform();
        transformer.exportMap();
        System.out.println("Saving map to " + transformer.outFolderPath.toAbsolutePath());
        System.out.println("Terrain Symmetry: " + transformer.symmetrySettings.getTerrainSymmetry());
        System.out.println("Team Symmetry: " + transformer.symmetrySettings.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + transformer.symmetrySettings.getSpawnSymmetry());
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
                    "--symmetry arg         required, set the symmetry for the map(" + Arrays.toString(Symmetry.values()) + ")\n" +
                    "--source arg           required, set which half to use as base for forced symmetry (" + Arrays.toString(SymmetrySource.values()) + ", {ANGLE})\n" +
                    "--marker               optional, force spawn, mex, hydro, and ai marker symmetry\n" +
                    "--props                optional, force prop symmetry\n" +
                    "--decals               optional, force decal symmetry\n" +
                    "--units                optional, force unit symmetry\n" +
                    "--terrain              optional, force terrain symmetry\n" +
                    "--all                  optional, force symmetry for all components\n" +
                    "--debug                optional, turn on debugging options\n");
            System.exit(0);
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            System.exit(1);
        }

        if (!arguments.containsKey("out-folder-path")) {
            System.out.println("Output Folder not Specified");
            System.exit(2);
        }

        if (!arguments.containsKey("symmetry")) {
            System.out.println("Symmetry not Specified");
            System.exit(3);
        }

        if (!arguments.containsKey("source")) {
            System.out.println("Source not Specified");
            System.exit(4);
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Symmetry teamSymmetry;
        if (pattern.matcher(arguments.get("source")).matches()) {
            teamSymmetry = null;
            angle = (360f - Float.parseFloat(arguments.get("source"))) % 360f;
            useAngle = true;
        } else {
            useAngle = false;
            switch (SymmetrySource.valueOf(arguments.get("source"))) {
                case TOP:
                    teamSymmetry = Symmetry.Z;
                    reverseSide = false;
                    break;
                case BOTTOM:
                    teamSymmetry = Symmetry.Z;
                    reverseSide = true;
                    break;
                case LEFT:
                    teamSymmetry = Symmetry.X;
                    reverseSide = false;
                    break;
                case RIGHT:
                    teamSymmetry = Symmetry.X;
                    reverseSide = true;
                    break;
                case TOP_LEFT:
                    teamSymmetry = Symmetry.ZX;
                    reverseSide = false;
                    break;
                case TOP_RIGHT:
                    teamSymmetry = Symmetry.XZ;
                    reverseSide = false;
                    break;
                case BOTTOM_LEFT:
                    teamSymmetry = Symmetry.XZ;
                    reverseSide = true;
                    break;
                case BOTTOM_RIGHT:
                    teamSymmetry = Symmetry.ZX;
                    reverseSide = true;
                    break;
                default:
                    teamSymmetry = Symmetry.NONE;
                    reverseSide = false;
                    break;
            }
        }
        symmetrySettings = new SymmetrySettings(Symmetry.valueOf(arguments.get("symmetry")), teamSymmetry, Symmetry.valueOf(arguments.get("symmetry")));
        transformResources = arguments.containsKey("resources") || arguments.containsKey("all");
        transformProps = arguments.containsKey("props") || arguments.containsKey("all");
        transformDecals = arguments.containsKey("decals") || arguments.containsKey("all");
        transformUnits = arguments.containsKey("units") || arguments.containsKey("all");
        transformTerrain = arguments.containsKey("terrain") || arguments.containsKey("all");

        if (transformDecals && !symmetrySettings.getSpawnSymmetry().equals(Symmetry.POINT2)) {
            System.out.println("This tool does not yet mirror decals");
        }
    }

    public void importMap() {
        try {
            File dir = inMapPath.toFile();

            File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
            if (mapFiles == null || mapFiles.length == 0) {
                System.out.println("No scmap file in map folder");
                return;
            }
            File scmapFile = mapFiles[0];
            mapFolder = inMapPath.getFileName().toString();
            mapName = scmapFile.getName().replace(".scmap", "");
            map = SCMapImporter.loadSCMAP(inMapPath);
            SaveImporter.importSave(inMapPath, map);
            ScenarioImporter.importScenario(inMapPath, map);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void exportMap() {
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(outFolderPath.resolve(mapFolder), mapName, map, true);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void transform() {

        heightmapBase = map.getHeightMask(symmetrySettings);

        if (transformTerrain) {
            FloatMask previewMask = map.getPreviewMask(symmetrySettings);
            FloatMask[] texturesMasks = map.getTextureMasksRaw(symmetrySettings);
            FloatMask texture1 = texturesMasks[0];
            FloatMask texture2 = texturesMasks[1];
            FloatMask texture3 = texturesMasks[2];
            FloatMask texture4 = texturesMasks[3];
            FloatMask texture5 = texturesMasks[4];
            FloatMask texture6 = texturesMasks[5];
            FloatMask texture7 = texturesMasks[6];
            FloatMask texture8 = texturesMasks[7];

            if (!useAngle) {
                previewMask.applySymmetry(SymmetryType.SPAWN, reverseSide);
                heightmapBase.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture1.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture2.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture3.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture4.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture5.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture6.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture7.applySymmetry(SymmetryType.SPAWN, reverseSide);
                texture8.applySymmetry(SymmetryType.SPAWN, reverseSide);
            } else {
                previewMask.applySymmetry(angle);
                heightmapBase.applySymmetry(angle);
                texture1.applySymmetry(angle);
                texture2.applySymmetry(angle);
                texture3.applySymmetry(angle);
                texture4.applySymmetry(angle);
                texture5.applySymmetry(angle);
                texture6.applySymmetry(angle);
                texture7.applySymmetry(angle);
                texture8.applySymmetry(angle);
            }

            ArrayList<BlankMarker> blankMarkers = new ArrayList<>(map.getBlankMarkers());
            map.getBlankMarkers().clear();
            map.getBlankMarkers().addAll(getTransformedBlankMarkers(blankMarkers));
            ArrayList<AIMarker> aiMarkers = new ArrayList<>(map.getAirAIMarkers());
            map.getAirAIMarkers().clear();
            map.getAirAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getLandAIMarkers());
            map.getLandAIMarkers().clear();
            map.getLandAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getNavyAIMarkers());
            map.getNavyAIMarkers().clear();
            map.getNavyAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getAmphibiousAIMarkers());
            map.getAmphibiousAIMarkers().clear();
            map.getAmphibiousAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getRallyMarkers());
            map.getRallyMarkers().clear();
            map.getRallyMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getExpansionAIMarkers());
            map.getExpansionAIMarkers().clear();
            map.getExpansionAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getLargeExpansionAIMarkers());
            map.getLargeExpansionAIMarkers().clear();
            map.getLargeExpansionAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getNavalAreaAIMarkers());
            map.getNavalAreaAIMarkers().clear();
            map.getNavalAreaAIMarkers().addAll(getTransformedAIMarkers(aiMarkers));
            aiMarkers = new ArrayList<>(map.getNavalRallyMarkers());
            map.getNavalRallyMarkers().clear();
            map.getNavalRallyMarkers().addAll(getTransformedAIMarkers(aiMarkers));

            map.setPreviewImage(previewMask);
            map.setHeightImage(heightmapBase);
            map.setTextureMasksLowRaw(texture1, texture2, texture3, texture4);
            map.setTextureMasksHighRaw(texture5, texture6, texture7, texture8);
        }
        if (transformResources) {
            ArrayList<Spawn> spawns = new ArrayList<>(map.getSpawns());
            ArrayList<Mex> mexes = new ArrayList<>(map.getMexes());
            ArrayList<Hydro> hydros = new ArrayList<>(map.getHydros());
            map.getSpawns().clear();
            map.getSpawns().addAll(getTransformedSpawns(spawns));
            map.getMexes().clear();
            map.getMexes().addAll(getTransformedMexes(mexes));
            map.getHydros().clear();
            map.getHydros().addAll(getTransformedHydros(hydros));
        }

        if (transformUnits) {
            transformArmies();
        }

        if (transformProps) {
            ArrayList<Prop> props = new ArrayList<>(map.getProps());
            map.getProps().clear();
            map.getProps().addAll(getTransformedProps(props));
        }

        if (transformDecals && symmetrySettings.getSpawnSymmetry().equals(Symmetry.POINT2)) {
            ArrayList<Decal> decals = new ArrayList<>(map.getDecals());
            map.getDecals().clear();
            map.getDecals().addAll(getTransformedDecals(decals));
        }
    }

    public ArrayList<Spawn> getTransformedSpawns(ArrayList<Spawn> spawns) {
        ArrayList<Spawn> transformedSpawns = new ArrayList<>();
        spawns.forEach(spawn -> {
            if ((!useAngle && heightmapBase.inTeam(spawn.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(spawn.getPosition(), angle))) {
                transformedSpawns.add(new Spawn("", Placement.placeOnHeightmap(map, spawn.getPosition()), spawn.getNoRushOffset()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(spawn.getPosition(), SymmetryType.SPAWN);
                for (SymmetryPoint symmetryPoint : symmetryPoints) {
                    Vector2f symmetricNoRushOffset = new Vector2f(spawn.getNoRushOffset());
                    symmetricNoRushOffset.flip(new Vector2f(0, 0), symmetryPoint.getSymmetry());
                    transformedSpawns.add(new Spawn("", Placement.placeOnHeightmap(map, symmetryPoint.getLocation()), symmetricNoRushOffset));
                }
            }
        });
        transformedSpawns.forEach(spawn -> {
            if (spawns.size() == transformedSpawns.size()) {
                Spawn[] closestSpawn = {null};
                float[] minDistance = {Float.POSITIVE_INFINITY};
                spawns.forEach(s -> {
                    float distance = spawn.getPosition().getXZDistance(s.getPosition());
                    if (distance < minDistance[0]) {
                        closestSpawn[0] = s;
                        minDistance[0] = distance;
                    }
                });
                spawn.setId(closestSpawn[0].getId());
            } else {
                spawn.setId("ARMY_" + transformedSpawns.indexOf(spawn));
            }
        });
        return transformedSpawns;
    }

    public ArrayList<Mex> getTransformedMexes(ArrayList<Mex> mexes) {
        ArrayList<Mex> transformedMexes = new ArrayList<>();
        mexes.forEach(mex -> {
            if ((!useAngle && heightmapBase.inTeam(mex.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(mex.getPosition(), angle))) {
                transformedMexes.add(new Mex(mex.getId(), Placement.placeOnHeightmap(map, mex.getPosition())));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(mex.getPosition(), SymmetryType.SPAWN);
                for (SymmetryPoint symmetryPoint : symmetryPoints) {
                    transformedMexes.add(new Mex(mex.getId() + " sym", Placement.placeOnHeightmap(map, symmetryPoint.getLocation())));
                }
            }
        });
        return transformedMexes;
    }

    public ArrayList<Hydro> getTransformedHydros(ArrayList<Hydro> hydros) {
        ArrayList<Hydro> transformedHydros = new ArrayList<>();
        hydros.forEach(hydro -> {
            if ((!useAngle && heightmapBase.inTeam(hydro.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(hydro.getPosition(), angle))) {
                transformedHydros.add(new Hydro(hydro.getId(), Placement.placeOnHeightmap(map, hydro.getPosition())));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(hydro.getPosition(), SymmetryType.SPAWN);
                for (SymmetryPoint symmetryPoint : symmetryPoints) {
                    transformedHydros.add(new Hydro(hydro.getId() + " sym", Placement.placeOnHeightmap(map, symmetryPoint.getLocation())));
                }
            }
        });
        return transformedHydros;
    }

    public ArrayList<AIMarker> getTransformedAIMarkers(ArrayList<AIMarker> aiMarkers) {
        ArrayList<AIMarker> transformedAImarkers = new ArrayList<>();
        aiMarkers.forEach(aiMarker -> {
            if ((!useAngle && heightmapBase.inTeam(aiMarker.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(aiMarker.getPosition(), angle))) {
                transformedAImarkers.add(new AIMarker(aiMarker.getId(), Placement.placeOnHeightmap(map, aiMarker.getPosition()), aiMarker.getNeighbors()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(aiMarker.getPosition(), SymmetryType.SPAWN);
                for (SymmetryPoint symmetryPoint : symmetryPoints) {
                    LinkedHashSet<String> newNeighbors = new LinkedHashSet<>();
                    aiMarker.getNeighbors().forEach(marker -> newNeighbors.add(String.format(marker + "s%d", symmetryPoints.indexOf(symmetryPoint))));
                    transformedAImarkers.add(new AIMarker(String.format(aiMarker.getId() + "s%d", symmetryPoints.indexOf(symmetryPoint)), Placement.placeOnHeightmap(map, symmetryPoint.getLocation()), newNeighbors));
                }
            }
        });
        return transformedAImarkers;
    }

    public ArrayList<BlankMarker> getTransformedBlankMarkers(ArrayList<BlankMarker> blankMarkers) {
        ArrayList<BlankMarker> transformedBlanks = new ArrayList<>();
        blankMarkers.forEach(blank -> {
            if ((!useAngle && heightmapBase.inTeam(blank.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(blank.getPosition(), angle))) {
                transformedBlanks.add(new BlankMarker(blank.getId(), Placement.placeOnHeightmap(map, blank.getPosition())));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(blank.getPosition(), SymmetryType.SPAWN);
                for (SymmetryPoint symmetryPoint : symmetryPoints) {
                    transformedBlanks.add(new BlankMarker(blank.getId() + "s", Placement.placeOnHeightmap(map, symmetryPoint.getLocation())));
                }
            }
        });
        return transformedBlanks;
    }

    public void transformArmies() {
        map.getArmies().forEach(this::transformArmy);
    }

    public void transformArmy(Army army) {
        army.getGroups().forEach(this::transformGroup);
    }

    public void transformGroup(Group group) {
        ArrayList<Unit> units = new ArrayList<>(group.getUnits());
        group.getUnits().clear();
        group.getUnits().addAll(getTransformedUnits(units));
    }

    public ArrayList<Unit> getTransformedUnits(ArrayList<Unit> units) {
        ArrayList<Unit> transformedUnits = new ArrayList<>();
        units.forEach(unit -> {
            if ((!useAngle && heightmapBase.inTeam(unit.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(unit.getPosition(), angle))) {
                transformedUnits.add(new Unit(unit.getId(), unit.getType(), Placement.placeOnHeightmap(map, unit.getPosition()), unit.getRotation()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(unit.getPosition(), SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightmapBase.getSymmetryRotation(unit.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    transformedUnits.add(new Unit(unit.getId() + " sym", unit.getType(), Placement.placeOnHeightmap(map, symmetryPoints.get(i).getLocation()), symmetryRotation.get(i)));
                }
            }
        });
        return transformedUnits;
    }

    public ArrayList<Prop> getTransformedProps(ArrayList<Prop> props) {
        ArrayList<Prop> transformedProps = new ArrayList<>();
        props.forEach(prop -> {
            if ((!useAngle && heightmapBase.inTeam(prop.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(prop.getPosition(), angle))) {
                transformedProps.add(new Prop(prop.getPath(), Placement.placeOnHeightmap(map, prop.getPosition()), prop.getRotation()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightmapBase.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    transformedProps.add(new Prop(prop.getPath(), Placement.placeOnHeightmap(map, symmetryPoints.get(i).getLocation()), symmetryRotation.get(i)));
                }
            }
        });
        return transformedProps;
    }

    public ArrayList<Decal> getTransformedDecals(ArrayList<Decal> decals) {
        ArrayList<Decal> transformedDecals = new ArrayList<>();
        decals.forEach(decal -> {
            if ((!useAngle && heightmapBase.inTeam(decal.getPosition(), reverseSide)) || (useAngle && heightmapBase.inHalf(decal.getPosition(), angle))) {
                transformedDecals.add(new Decal(decal.getPath(), Placement.placeOnHeightmap(map, decal.getPosition()), decal.getRotation(), decal.getScale(), decal.getCutOffLOD()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(decal.getPosition(), SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightmapBase.getSymmetryRotation(decal.getRotation().y);
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3f symVectorRotation = new Vector3f(decal.getRotation().x, symmetryRotation.get(i), decal.getRotation().z);
                    transformedDecals.add(new Decal(decal.getPath(), Placement.placeOnHeightmap(map, symmetryPoints.get(i).getLocation()), symVectorRotation, decal.getScale(), decal.getCutOffLOD()));
                }
            }
        });
        return transformedDecals;
    }
}
