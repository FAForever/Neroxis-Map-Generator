package neroxis.transformer;

import neroxis.exporter.MapExporter;
import neroxis.importer.MapImporter;
import neroxis.map.*;
import neroxis.util.*;

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
    private int resize;
    private int mapSize;
    private boolean resizeSet = false;
    private boolean mapSizeSet = false;
    private int oldMapSize;
    private int shiftX;
    private int shiftZ;
    private boolean shiftXSet = false;
    private boolean shiftZSet = false;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

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
                    "--resize arg           optional, resize the whole map's placement of features/details to arg size (256 = 5 km x 5 km map)\n" +
                    "--map-size arg         optional, resize the map bounds to arg size (256 = 5 km x 5 km map)\n" +
                    "--x arg                optional, set arg x-coordinate for the center of the map's placement of features/details\n" +
                    "--z arg                optional, set arg z-coordinate for the center of the map's placement of features/details\n" +
                    "--debug                optional, turn on debugging options\n");
            return;
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            return;
        }

        if (!arguments.containsKey("out-folder-path")) {
            System.out.println("Output Folder not Specified");
            return;
        }

        if (!arguments.containsKey("symmetry")) {
            System.out.println("Symmetry not Specified");
            return;
        }

        if (!arguments.containsKey("source")) {
            System.out.println("Source not Specified");
            return;
        }

        if (arguments.containsKey("resize")) {
            resize = Integer.parseInt(arguments.get("resize"));
            resizeSet = true;
        }

        if (arguments.containsKey("map-size")) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
            mapSizeSet = true;
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
            map = MapImporter.importMap(inMapPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    public void exportMap() {
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(outFolderPath, map, true);
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

            map.setPreviewImage(previewMask);
            map.setHeightImage(heightmapBase);
            map.setTextureMasksLowRaw(texture1, texture2, texture3, texture4);
            map.setTextureMasksHighRaw(texture5, texture6, texture7, texture8);

            ArrayList<Marker> blankMarkers = new ArrayList<>(map.getBlankMarkers());
            map.getBlankMarkers().clear();
            map.getBlankMarkers().addAll(getTransformedMarkers(blankMarkers));
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
        }

        if (transformResources) {
            List<Spawn> spawns = new ArrayList<>(map.getSpawns());
            List<Marker> mexes = new ArrayList<>(map.getMexes());
            List<Marker> hydros = new ArrayList<>(map.getHydros());
            map.getSpawns().clear();
            map.getSpawns().addAll(getTransformedSpawns(spawns));
            map.getMexes().clear();
            map.getMexes().addAll(getTransformedMarkers(mexes));
            map.getHydros().clear();
            map.getHydros().addAll(getTransformedMarkers(hydros));
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

        oldMapSize = map.getSize();
        if (!mapSizeSet) {
            if (resizeSet) {
                mapSize = resize;
            } else {
                mapSize = oldMapSize;
            }
        }
        if (!shiftXSet) {
            shiftX = mapSize / 2;
        }
        if (!shiftZSet) {
            shiftZ = mapSize / 2;
        }
        if (mapSize != oldMapSize || resize != oldMapSize) {
            map.resize(resize, mapSize, new Vector2f(shiftX, shiftZ));
        }
    }

    public List<Spawn> getTransformedSpawns(List<Spawn> spawns) {
        ArrayList<Spawn> transformedSpawns = new ArrayList<>();
        spawns.forEach(spawn -> {
            if (inSourceRegion(spawn.getPosition())) {
                transformedSpawns.add(new Spawn("", Placement.placeOnHeightmap(map, spawn.getPosition()), spawn.getNoRushOffset()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(spawn.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> {
                    Vector2f symmetricNoRushOffset = new Vector2f(spawn.getNoRushOffset());
                    symmetricNoRushOffset.flip(new Vector2f(0, 0), symmetryPoint.getSymmetry());
                    transformedSpawns.add(new Spawn("", Placement.placeOnHeightmap(map, symmetryPoint.getLocation()), symmetricNoRushOffset));
                });
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
        transformedSpawns.sort(Comparator.comparing(Marker::getId));
        return transformedSpawns;
    }

    public List<Marker> getTransformedMarkers(Collection<Marker> markers) {
        List<Marker> transformedMarkers = new ArrayList<>();
        markers.forEach(marker -> {
            if (inSourceRegion(marker.getPosition())) {
                transformedMarkers.add(new Marker(marker.getId(), Placement.placeOnHeightmap(map, marker.getPosition())));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(marker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> transformedMarkers.add(new Marker(marker.getId() + " sym", Placement.placeOnHeightmap(map, symmetryPoint.getLocation()))));
            }
        });
        transformedMarkers.forEach(marker -> {
            if (markers.size() == transformedMarkers.size()) {
                Marker[] closestMarker = {null};
                float[] minDistance = {Float.POSITIVE_INFINITY};
                markers.forEach(s -> {
                    float distance = marker.getPosition().getXZDistance(s.getPosition());
                    if (distance < minDistance[0]) {
                        closestMarker[0] = s;
                        minDistance[0] = distance;
                    }
                });
                marker.setId(closestMarker[0].getId());
            }
        });
        return transformedMarkers;
    }

    public List<AIMarker> getTransformedAIMarkers(List<AIMarker> aiMarkers) {
        ArrayList<AIMarker> transformedAImarkers = new ArrayList<>();
        aiMarkers.forEach(aiMarker -> {
            if (inSourceRegion(aiMarker.getPosition())) {
                transformedAImarkers.add(new AIMarker(aiMarker.getId(), Placement.placeOnHeightmap(map, aiMarker.getPosition()), aiMarker.getNeighbors()));
                ArrayList<SymmetryPoint> symmetryPoints = heightmapBase.getSymmetryPoints(aiMarker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> {
                    LinkedHashSet<String> newNeighbors = new LinkedHashSet<>();
                    aiMarker.getNeighbors().forEach(marker -> newNeighbors.add(String.format(marker + "s%d", symmetryPoints.indexOf(symmetryPoint))));
                    transformedAImarkers.add(new AIMarker(String.format(aiMarker.getId() + "s%d", symmetryPoints.indexOf(symmetryPoint)), Placement.placeOnHeightmap(map, symmetryPoint.getLocation()), newNeighbors));
                });
            }
        });
        return transformedAImarkers;
    }

    public void transformArmies() {
        map.getArmies().forEach(this::transformArmy);
    }

    public void transformArmy(Army army) {
        army.getGroups().forEach(this::transformGroup);
    }

    public void transformGroup(Group group) {
        List<Unit> units = new ArrayList<>(group.getUnits());
        group.getUnits().clear();
        group.getUnits().addAll(getTransformedUnits(units));
    }

    public List<Unit> getTransformedUnits(List<Unit> units) {
        ArrayList<Unit> transformedUnits = new ArrayList<>();
        units.forEach(unit -> {
            if (inSourceRegion(unit.getPosition())) {
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

    public List<Prop> getTransformedProps(List<Prop> props) {
        ArrayList<Prop> transformedProps = new ArrayList<>();
        props.forEach(prop -> {
            if (inSourceRegion(prop.getPosition())) {
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

    public List<Decal> getTransformedDecals(List<Decal> decals) {
        ArrayList<Decal> transformedDecals = new ArrayList<>();
        decals.forEach(decal -> {
            if (inSourceRegion(decal.getPosition())) {
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

    public boolean inSourceRegion(Vector3f position) {
        return (!useAngle && heightmapBase.inTeam(position, reverseSide)) || (useAngle && heightmapBase.inHalf(position, angle));
    }
}
