package neroxis.transformer;

import neroxis.exporter.MapExporter;
import neroxis.importer.MapImporter;
import neroxis.map.*;
import neroxis.util.ArgumentParser;
import neroxis.util.FileUtils;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

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
    private int mapBounds;
    private boolean resizeSet = false;
    private boolean mapBoundsSet = false;
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
                    "--symmetry arg         optional, set the symmetry for the map(" + Arrays.toString(Symmetry.values()) + ")\n" +
                    "--source arg           required for symmetry, set which half to use as base for forced symmetry (" + Arrays.toString(SymmetrySource.values()) + ", {ANGLE})\n" +
                    "--marker               optional, force spawn, mex, hydro, and ai marker symmetry\n" +
                    "--props                optional, force prop symmetry\n" +
                    "--decals               optional, force decal symmetry\n" +
                    "--units                optional, force unit symmetry\n" +
                    "--terrain              optional, force terrain symmetry\n" +
                    "--all                  optional, force symmetry for all components\n" +
                    "--resize arg           optional, resize the whole map's placement of features/details to arg size (512 = 10 km x 10 km map)\n" +
                    "--map-size arg       optional, resize the map bounds to arg size (512 = 10 km x 10 km map)\n" +
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
            map.changeMapSize(resize, mapBounds, new Vector2f(shiftX, shiftZ));
        }
    }

    private void transformSymmetry() {
        heightmapBase = map.getHeightMask(symmetrySettings);

        if (transformTerrain) {
            transformTerrain();
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
        FloatMask previewMask = map.getPreviewMask(symmetrySettings);
        FloatMask[] texturesMasks = map.getTextureMasksRaw(symmetrySettings);

        if (!useAngle) {
            previewMask.applySymmetry(SymmetryType.SPAWN, reverseSide);
            heightmapBase.applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[0].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[1].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[2].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[3].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[4].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[5].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[6].applySymmetry(SymmetryType.SPAWN, reverseSide);
            texturesMasks[7].applySymmetry(SymmetryType.SPAWN, reverseSide);
        } else {
            previewMask.applySymmetry(angle);
            heightmapBase.applySymmetry(angle);
            texturesMasks[0].applySymmetry(angle);
            texturesMasks[1].applySymmetry(angle);
            texturesMasks[2].applySymmetry(angle);
            texturesMasks[3].applySymmetry(angle);
            texturesMasks[4].applySymmetry(angle);
            texturesMasks[5].applySymmetry(angle);
            texturesMasks[6].applySymmetry(angle);
            texturesMasks[7].applySymmetry(angle);
        }

        map.setPreviewImage(previewMask);
        map.setHeightImage(heightmapBase);
        map.setTextureMasksLowRaw(texturesMasks[0], texturesMasks[1], texturesMasks[2], texturesMasks[3]);
        map.setTextureMasksHighRaw(texturesMasks[4], texturesMasks[5], texturesMasks[6], texturesMasks[7]);
    }

    private void transformSpawns(Collection<Spawn> spawns) {
        List<Spawn> transformedSpawns = new ArrayList<>();
        spawns.forEach(spawn -> {
            if (inSourceRegion(spawn.getPosition())) {
                transformedSpawns.add(new Spawn("", spawn.getPosition(), spawn.getNoRushOffset(), 0));
                List<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(spawn.getPosition(), SymmetryType.SPAWN);
                for (int i = 0; i < symmetryPoints.size(); ++i) {
                    Vector2f symmetryPoint = symmetryPoints.get(i);
                    Vector2f symmetricNoRushOffset = new Vector2f(spawn.getNoRushOffset());
                    if (!heightmapBase.inTeam(symmetryPoint, false)) {
                        symmetricNoRushOffset.flip(new Vector2f(0, 0), heightmapBase.getSymmetrySettings().getSpawnSymmetry());
                    }
                    transformedSpawns.add(new Spawn("", symmetryPoint, symmetricNoRushOffset, i + 1));
                }
            }
        });
        if (spawns.size() == transformedSpawns.size()) {
            transformedSpawns.forEach(spawn -> matchToClosestMarker(spawn, spawns));
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
                ArrayList<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(marker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> transformedMarkers.add(new Marker(marker.getId() + " sym", symmetryPoint)));
            }
        });
        if (markers.size() == transformedMarkers.size()) {
            transformedMarkers.forEach(marker -> matchToClosestMarker(marker, markers));
        }
        markers.clear();
        markers.addAll(transformedMarkers);
    }

    private void transformAIMarkers(Collection<AIMarker> aiMarkers) {
        Collection<AIMarker> transformedAImarkers = new ArrayList<>();
        aiMarkers.forEach(aiMarker -> {
            if (inSourceRegion(aiMarker.getPosition())) {
                transformedAImarkers.add(new AIMarker(aiMarker.getId(), aiMarker.getPosition(), aiMarker.getNeighbors()));
                ArrayList<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(aiMarker.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> {
                    LinkedHashSet<String> newNeighbors = new LinkedHashSet<>();
                    aiMarker.getNeighbors().forEach(marker -> newNeighbors.add(String.format(marker + "s%d", symmetryPoints.indexOf(symmetryPoint))));
                    transformedAImarkers.add(new AIMarker(String.format(aiMarker.getId() + "s%d", symmetryPoints.indexOf(symmetryPoint)), symmetryPoint, newNeighbors));
                });
            }
        });
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
                ArrayList<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(unit.getPosition(), SymmetryType.SPAWN);
                ArrayList<Float> symmetryRotation = heightmapBase.getSymmetryRotation(unit.getRotation());
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
                List<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightmapBase.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    transformedProps.add(new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            }
        });
        props.clear();
        props.addAll(transformedProps);
    }

    private void transformDecals(Collection<Decal> decals) {
        Collection<Decal> transformedDecals = new ArrayList<>();
        decals.forEach(decal -> {
            if (inSourceRegion(decal.getPosition())) {
                transformedDecals.add(new Decal(decal.getPath(), decal.getPosition(), decal.getRotation(), decal.getScale(), decal.getCutOffLOD()));
                List<Vector2f> symmetryPoints = heightmapBase.getSymmetryPoints(decal.getPosition(), SymmetryType.SPAWN);
                List<Float> symmetryRotation = heightmapBase.getSymmetryRotation(decal.getRotation().getY());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3f symVectorRotation = new Vector3f(decal.getRotation().getX(), symmetryRotation.get(i), decal.getRotation().getZ());
                    transformedDecals.add(new Decal(decal.getPath(), new Vector3f(symmetryPoints.get(i)), symVectorRotation, decal.getScale(), decal.getCutOffLOD()));
                }
            }
        });
        decals.clear();
        decals.addAll(transformedDecals);
    }

    private void matchToClosestMarker(Marker marker, Collection<? extends Marker> markers) {
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

    private boolean inSourceRegion(Vector3f position) {
        return (!useAngle && heightmapBase.inTeam(position, reverseSide)) || (useAngle && heightmapBase.inHalf(position, angle));
    }
}
