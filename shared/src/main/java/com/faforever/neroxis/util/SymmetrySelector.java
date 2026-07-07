package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import static com.faforever.neroxis.map.Symmetry.POINT2;

public class SymmetrySelector {
    public static Symmetry getValidTerrainSymmetry(RandomGenerator random, int spawnCount,
                                                   int numTeams) {
        List<Symmetry> terrainSymmetries = switch (spawnCount) {
            case 2, 4, 8 -> new ArrayList<>(
                    Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6, Symmetry.POINT8, Symmetry.QUAD,
                                  Symmetry.DIAG));
            default -> new ArrayList<>(Arrays.asList(Symmetry.values()));
        };
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        if (numTeams > 1) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % numTeams != 0 ||
                                                   symmetry.getNumSymPoints() > spawnCount * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (numTeams == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        return terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
    }

    public static SymmetrySettings getSymmetrySettings(RandomGenerator random, int spawnCount, int numTeams) {
        return getSymmetrySettingsFromTerrainSymmetry(random, getValidTerrainSymmetry(random, spawnCount,
                                                                                      numTeams),
                                                      spawnCount, numTeams);
    }

    public static SymmetrySettings getSymmetrySettingsFromTerrainSymmetry(RandomGenerator random,
                                                                          Symmetry terrainSymmetry, int spawnCount,
                                                                          int numTeams) {
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        List<Symmetry> spawns;
        List<Symmetry> teams;
        switch (terrainSymmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13,
                 POINT14, POINT15, POINT16 -> {
                spawns = new ArrayList<>(
                        List.of(Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5, Symmetry.POINT6,
                                Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                                Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15,
                                Symmetry.POINT16));
                teams = new ArrayList<>(
                        List.of(Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5, Symmetry.POINT6,
                                Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                                Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15,
                                Symmetry.POINT16, Symmetry.XZ, Symmetry.ZX, Symmetry.X, Symmetry.Z, Symmetry.QUAD,
                                Symmetry.DIAG));
            }
            case QUAD -> {
                spawns = new ArrayList<>(List.of(POINT2, Symmetry.QUAD));
                teams = new ArrayList<>(List.of(Symmetry.X, Symmetry.Z, Symmetry.QUAD));
            }
            case DIAG -> {
                spawns = new ArrayList<>(List.of(POINT2, Symmetry.DIAG));
                teams = new ArrayList<>(List.of(Symmetry.XZ, Symmetry.ZX, Symmetry.DIAG));
            }
            default -> {
                spawns = new ArrayList<>(List.of(terrainSymmetry));
                teams = new ArrayList<>(List.of(terrainSymmetry));
            }
        }
        if (numTeams > 1) {
            spawns.removeIf(symmetry -> {
                int numSymPoints = symmetry.getNumSymPoints();
                return (numSymPoints % numTeams != 0 || spawnCount % numSymPoints != 0) ||
                       terrainSymmetry.getNumSymPoints() % numSymPoints != 0 ||
                       (!symmetry.isPerfectSymmetry() && numSymPoints != numTeams);
            });
            spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
            teams.removeIf(symmetry -> {
                int numSymPoints = symmetry.getNumSymPoints();
                int spawnNumSymPoints = spawnSymmetry.getNumSymPoints();
                return numSymPoints % spawnNumSymPoints != 0 ||
                       numSymPoints % numTeams != 0 ||
                       numSymPoints > spawnNumSymPoints;
            });
            teamSymmetry = teams.get(random.nextInt(teams.size()));
        } else {
            spawnSymmetry = Symmetry.NONE;
            teamSymmetry = Symmetry.NONE;
        }

        return new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
    }
}
