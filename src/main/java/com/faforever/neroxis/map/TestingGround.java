package com.faforever.neroxis.map;

import com.faforever.neroxis.map.exporter.MapExporter;
import com.faforever.neroxis.map.exporter.SCMapExporter;
import com.faforever.neroxis.map.generator.MapGenerator;
import com.faforever.neroxis.map.generator.style.StyleGenerator;
import com.faforever.neroxis.util.Pipeline;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.faforever.neroxis.map.Symmetry.POINT2;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 3; ++i) {
            int mapSize = (int) StrictMath.pow(2, 8 + i);
            for (StyleGenerator mapStyle : MapGenerator.MAP_STYLES.stream().filter(generator -> generator.getParameterConstraints().matches(mapSize, 2, 2))
                    .collect(Collectors.toList())) {
                SymmetrySettings symmetrySettings = setSymmetrySettings(getValidTerrainSymmetry(2, 2), 2);
                MapParameters mapParameters = mapStyle.getParameterConstraints().initParameters(new Random(), 2, mapSize, 2, false, false, false, symmetrySettings);
                SCMap map = mapStyle.generate(mapParameters, new Random().nextLong());
                map.setFolderName(String.format("%dkm_%s", (int) (mapSize / 51.2), mapStyle.getName().toLowerCase()));
                map.setFilePrefix(String.format("%dkm_%s", (int) (mapSize / 51.2), mapStyle.getName().toLowerCase()));
                MapExporter.exportMap(Paths.get("D:\\FAFProjects\\Neroxis-Map-Generator\\SampleMaps"), map, true, true);
                SCMapExporter.exportPreview(Paths.get("D:\\FAFProjects\\Neroxis-Map-Generator\\SampleMapPreviews"), map);
            }
        }
        Pipeline.shutdown();


//        Util.DEBUG = true;
//        Util.VISUALIZE = true;
//
//        Boolean[][] test1 = new Boolean[2048][2048];
//        boolean[][] test2 = new boolean[2048][2048];
//
//        for (int x = 0; x < 2048; ++x) {
//            for (int y = 0; y < 2048; ++y) {
//                test1[x][y] = false;
//                test2[x][y] = false;
//            }
//        }
    }

    public static SymmetrySettings setSymmetrySettings(Symmetry terrainSymmetry, int numTeams) {
        Random random = new Random();
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        List<Symmetry> spawns;
        List<Symmetry> teams;
        switch (terrainSymmetry) {
            case POINT2:
            case POINT3:
            case POINT4:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                spawns = new ArrayList<>(Arrays.asList(POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                        Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                        Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15, Symmetry.POINT16));
                teams = new ArrayList<>(Arrays.asList(POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                        Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                        Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15, Symmetry.POINT16,
                        Symmetry.XZ, Symmetry.ZX, Symmetry.X, Symmetry.Z, Symmetry.QUAD, Symmetry.DIAG));
                break;
            case QUAD:
                spawns = new ArrayList<>(Arrays.asList(POINT2, Symmetry.QUAD));
                teams = new ArrayList<>(Arrays.asList(POINT2, Symmetry.X, Symmetry.Z, Symmetry.QUAD));
                break;
            case DIAG:
                spawns = new ArrayList<>(Arrays.asList(POINT2, Symmetry.DIAG));
                teams = new ArrayList<>(Arrays.asList(POINT2, Symmetry.XZ, Symmetry.ZX, Symmetry.DIAG));
                break;
            default:
                spawns = new ArrayList<>(Collections.singletonList(terrainSymmetry));
                teams = new ArrayList<>(Collections.singletonList(terrainSymmetry));
                break;
        }
        if (numTeams > 1) {
            spawns.removeIf(symmetry -> numTeams != symmetry.getNumSymPoints());
            teams.removeIf(symmetry -> numTeams != symmetry.getNumSymPoints());
        }
        spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
        teamSymmetry = teams.get(random.nextInt(teams.size()));
        return new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
    }

    public static Symmetry getValidTerrainSymmetry(int spawnCount, int numTeams) {
        Random random = new Random();
        List<Symmetry> terrainSymmetries;
        switch (spawnCount) {
            case 2:
            case 4:
                terrainSymmetries = new ArrayList<>(Arrays.asList(POINT2, Symmetry.POINT4, Symmetry.POINT6,
                        Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG));
                break;
            default:
                terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.values()));
                break;
        }
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        if (numTeams > 1) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % numTeams != 0 || symmetry.getNumSymPoints() > spawnCount * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (numTeams == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        return terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
    }
}
