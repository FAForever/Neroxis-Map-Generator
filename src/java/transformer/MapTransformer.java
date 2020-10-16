package transformer;

import exporter.SCMapExporter;
import exporter.SaveExporter;
import exporter.ScenarioExporter;
import importer.SCMapImporter;
import importer.SaveImporter;
import map.*;
import util.ArgumentParser;
import util.FileUtils;
import util.Placement;
import util.Vector3f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public strictfp class MapTransformer {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private String mapFolder;
    private String mapName;
    private SCMap map;

    //masks used in transformation
    private FloatMask heightmapBase;
    private FloatMask texture1;
    private FloatMask texture2;
    private FloatMask texture3;
    private FloatMask texture4;
    private FloatMask texture5;
    private FloatMask texture6;
    private FloatMask texture7;
    private FloatMask texture8;

    private boolean transformResources;
    private boolean transformProps;
    private boolean transformWrecks;
    private boolean transformDecals;
    private boolean transformCivilians;
    private boolean transformTerrain;
    private boolean reverseSide;
    private SymmetryHierarchy symmetryHierarchy;

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
        System.out.println("Terrain Symmetry: " + transformer.symmetryHierarchy.getTerrainSymmetry());
        System.out.println("Team Symmetry: " + transformer.symmetryHierarchy.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + transformer.symmetryHierarchy.getSpawnSymmetry());
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
                    "--symmetry arg         required, set the symmetry for the map(X, Z, XZ, ZX, POINT)\n" +
                    "--source arg           required, set which half to use as base for forced symmetry (TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)\n" +
                    "--spawns               optional, force spawn symmetry\n" +
                    "--resources            optional, force mex symmetry\n" +
                    "--props                optional, force prop symmetry\n" +
                    "--decals               optional, force decal symmetry\n" +
                    "--wrecks               optional, force wreck symmetry\n" +
                    "--civilians            optional, force civilian symmetry\n" +
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
        Symmetry teamSymmetry = null;
        switch (SymmetryHalf.valueOf(arguments.get("source"))) {
            case TOP -> {
                teamSymmetry = Symmetry.Z;
                reverseSide = false;
            }
            case BOTTOM -> {
                teamSymmetry = Symmetry.Z;
                reverseSide = true;
            }
            case LEFT -> {
                teamSymmetry = Symmetry.X;
                reverseSide = false;
            }
            case RIGHT -> {
                teamSymmetry = Symmetry.X;
                reverseSide = true;
            }
            case TOP_LEFT -> {
                teamSymmetry = Symmetry.ZX;
                reverseSide = false;
            }
            case TOP_RIGHT -> {
                teamSymmetry = Symmetry.XZ;
                reverseSide = false;
            }
            case BOTTOM_LEFT -> {
                teamSymmetry = Symmetry.XZ;
                reverseSide = true;
            }
            case BOTTOM_RIGHT -> {
                teamSymmetry = Symmetry.ZX;
                reverseSide = true;
            }
        }
        symmetryHierarchy = new SymmetryHierarchy(Symmetry.valueOf(arguments.get("symmetry")), teamSymmetry);
        symmetryHierarchy.setSpawnSymmetry(Symmetry.valueOf(arguments.get("symmetry")));
        transformResources = arguments.containsKey("resources") || arguments.containsKey("all");
        transformProps = arguments.containsKey("props") || arguments.containsKey("all");
        transformDecals = arguments.containsKey("decals") || arguments.containsKey("all");
        transformWrecks = arguments.containsKey("wrecks") || arguments.containsKey("all");
        transformCivilians = arguments.containsKey("civilians") || arguments.containsKey("all");
        transformTerrain = arguments.containsKey("terrain") || arguments.containsKey("all");
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
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void exportMap() {
        try {
            long startTime = System.currentTimeMillis();
            FileUtils.copyRecursiveIfExists(inMapPath, outFolderPath);
            Files.copy(inMapPath.resolve(mapName + "_script.lua"), outFolderPath.resolve(mapFolder).resolve(mapName + "_script.lua"), StandardCopyOption.REPLACE_EXISTING);
            SCMapExporter.exportSCMAP(outFolderPath.resolve(mapFolder), mapName, map);
            SaveExporter.exportSave(outFolderPath.resolve(mapFolder), mapName, map);
            ScenarioExporter.exportScenario(outFolderPath.resolve(mapFolder), mapName, map);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void transform() {

        heightmapBase = map.getHeightMask(symmetryHierarchy);

        if (transformTerrain) {
            FloatMask[] texturesMasks = map.getTextureMasks(symmetryHierarchy);
            texture1 = texturesMasks[0];
            texture2 = texturesMasks[1];
            texture3 = texturesMasks[2];
            texture4 = texturesMasks[3];
            texture5 = texturesMasks[4];
            texture6 = texturesMasks[5];
            texture7 = texturesMasks[6];
            texture8 = texturesMasks[7];

            heightmapBase.applySymmetry(reverseSide);
            texture1.applySymmetry(reverseSide);
            texture2.applySymmetry(reverseSide);
            texture3.applySymmetry(reverseSide);
            texture4.applySymmetry(reverseSide);
            texture5.applySymmetry(reverseSide);
            texture6.applySymmetry(reverseSide);
            texture7.applySymmetry(reverseSide);
            texture8.applySymmetry(reverseSide);

            map.setHeightmap(heightmapBase);
            map.setTextureMasksLow(texture1, texture2, texture3, texture4);
            map.setTextureMasksHigh(texture5, texture6, texture7, texture8);
        }
        if (transformResources) {
            ArrayList<Vector3f> spawns = new ArrayList<>(map.getSpawns());
            ArrayList<Vector3f> mexes = new ArrayList<>(map.getMexes());
            ArrayList<Vector3f> hydros = new ArrayList<>(map.getHydros());
            map.getSpawns().clear();
            map.getSpawns().addAll(getTransformedVector3fs(spawns));
            map.getMexes().clear();
            map.getMexes().addAll(getTransformedVector3fs(mexes));
            map.getHydros().clear();
            map.getHydros().addAll(getTransformedVector3fs(hydros));
        }

        if (transformWrecks) {
            ArrayList<Unit> wrecks = new ArrayList<>(map.getWrecks());
            map.getWrecks().clear();
            map.getWrecks().addAll(getTransformedUnits(wrecks));
        }

        if (transformCivilians) {
            ArrayList<Unit> civlians = new ArrayList<>(map.getCivs());
            ArrayList<Unit> units = new ArrayList<>(map.getUnits());
            map.getCivs().clear();
            map.getCivs().addAll(getTransformedUnits(civlians));
            map.getUnits().clear();
            map.getUnits().addAll(getTransformedUnits(units));
        }

        if (transformProps) {
            ArrayList<Prop> props = new ArrayList<>(map.getProps());
            map.getProps().clear();
            map.getProps().addAll(getTransformedProps(props));
        }

        if (transformDecals) {
            ArrayList<Decal> decals = new ArrayList<>(map.getDecals());
            map.getDecals().clear();
            map.getDecals().addAll(getTransformedDecals(decals));
        }
    }

    public ArrayList<Vector3f> getTransformedVector3fs(ArrayList<Vector3f> vectors) {
        ArrayList<Vector3f> transformedVectors = new ArrayList<>();
        vectors.forEach(loc -> {
            if (heightmapBase.inHalf(loc, reverseSide)) {
                transformedVectors.add(Placement.placeOnHeightmap(map, loc));
                transformedVectors.add(Placement.placeOnHeightmap(map, heightmapBase.getSymmetryPoint(loc)));
            }
        });
        return transformedVectors;
    }

    public ArrayList<Unit> getTransformedUnits(ArrayList<Unit> units) {
        ArrayList<Unit> transformedUnits = new ArrayList<>();
        units.forEach(unit -> {
            if (heightmapBase.inHalf(unit.getPosition(), reverseSide)) {
                transformedUnits.add(new Unit(unit.getType(), Placement.placeOnHeightmap(map, unit.getPosition()), unit.getRotation()));
                transformedUnits.add(new Unit(unit.getType(), Placement.placeOnHeightmap(map, heightmapBase.getSymmetryPoint(unit.getPosition())), heightmapBase.getReflectedRotation(unit.getRotation())));
            }
        });
        return transformedUnits;
    }

    public ArrayList<Prop> getTransformedProps(ArrayList<Prop> props) {
        ArrayList<Prop> transformedProps = new ArrayList<>();
        props.forEach(prop -> {
            if (heightmapBase.inHalf(prop.getPosition(), reverseSide)) {
                transformedProps.add(new Prop(prop.getPath(), Placement.placeOnHeightmap(map, prop.getPosition()), prop.getRotation()));
                transformedProps.add(new Prop(prop.getPath(), Placement.placeOnHeightmap(map, heightmapBase.getSymmetryPoint(prop.getPosition())), heightmapBase.getReflectedRotation(prop.getRotation())));
            }
        });
        return transformedProps;
    }

    public ArrayList<Decal> getTransformedDecals(ArrayList<Decal> decals) {
        ArrayList<Decal> transformedDecals = new ArrayList<>();
        boolean flipX;
        boolean flipZ;
        if (heightmapBase.getSymmetryHierarchy().getSpawnSymmetry() != Symmetry.POINT) {
            switch (heightmapBase.getSymmetryHierarchy().getTeamSymmetry()) {
                case Z, ZX -> {
                    flipZ = false;
                    flipX = true;
                }
                case X, XZ -> {
                    flipZ = true;
                    flipX = false;
                }
                default -> {
                    flipZ = false;
                    flipX = false;
                }
            }
        } else {
            flipX = false;
            flipZ = false;
        }
        decals.forEach(decal -> {
            if (heightmapBase.inHalf(decal.getPosition(), reverseSide)) {
                float rot = heightmapBase.getReflectedRotation(decal.getRotation().y);
                Vector3f newRotation = new Vector3f(decal.getRotation().x + (flipX ? (float) StrictMath.PI : 0f), rot, decal.getRotation().z + (flipZ ? (float) StrictMath.PI : 0f));
                transformedDecals.add(new Decal(decal.getPath(), Placement.placeOnHeightmap(map, decal.getPosition()), decal.getRotation(), decal.getScale(), decal.getCutOffLOD()));
                transformedDecals.add(new Decal(decal.getPath(), Placement.placeOnHeightmap(map, heightmapBase.getSymmetryPoint(decal.getPosition())), newRotation, decal.getScale(), decal.getCutOffLOD()));
            }
        });
        return transformedDecals;
    }
}
