package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;
import static com.faforever.neroxis.map.Symmetry.POINT2;
import static com.faforever.neroxis.map.Symmetry.POINT3;
import com.faforever.neroxis.map.SymmetrySettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SymmetrySelector {

    public static SymmetrySettings getSymmetrySettingsFromTerrainSymmetry(Random random, Symmetry terrainSymmetry,
                                                                          int numTeams) {
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        List<Symmetry> spawns;
        List<Symmetry> teams;
        switch (terrainSymmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> {
                spawns = new ArrayList<>(
                        Arrays.asList(POINT2, POINT3, Symmetry.POINT4, Symmetry.POINT5, Symmetry.POINT6,
                                      Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10,
                                      Symmetry.POINT11, Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14,
                                      Symmetry.POINT15, Symmetry.POINT16));
                teams = new ArrayList<>(Arrays.asList(POINT2, POINT3, Symmetry.POINT4, Symmetry.POINT5, Symmetry.POINT6,
                                                      Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9,
                                                      Symmetry.POINT10, Symmetry.POINT11, Symmetry.POINT12,
                                                      Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15,
                                                      Symmetry.POINT16, Symmetry.XZ, Symmetry.ZX, Symmetry.X,
                                                      Symmetry.Z, Symmetry.QUAD, Symmetry.DIAG));
            }
            case QUAD -> {
                spawns = new ArrayList<>(Arrays.asList(POINT2, Symmetry.QUAD));
                teams = new ArrayList<>(Arrays.asList(POINT2, Symmetry.X, Symmetry.Z, Symmetry.QUAD));
            }
            case DIAG -> {
                spawns = new ArrayList<>(Arrays.asList(POINT2, Symmetry.DIAG));
                teams = new ArrayList<>(Arrays.asList(POINT2, Symmetry.XZ, Symmetry.ZX, Symmetry.DIAG));
            }
            default -> {
                spawns = new ArrayList<>(List.of(terrainSymmetry));
                teams = new ArrayList<>(List.of(terrainSymmetry));
            }
        }
        if (numTeams > 1) {
            spawns.removeIf(symmetry -> numTeams != symmetry.getNumSymPoints());
            teams.removeIf(symmetry -> numTeams != symmetry.getNumSymPoints());
        }
        spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
        teamSymmetry = teams.get(random.nextInt(teams.size()));
        return new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
    }
}
