package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.mask.BooleanMask;

public class OnePerBaseHydroPlacer extends HydroPlacer {

    public OnePerBaseHydroPlacer(SCMap map, long seed) {
        super(map, seed);
    }

    @Override
    public void placeHydros(int hydroCount, BooleanMask allowedHydroMask) {
        this.allowedHydroMask = allowedHydroMask;
        map.getHydros().clear();

        if (!allowedHydroMask.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            allowedHydroMask.limitToCenteredCircle(allowedHydroMask.getSize() / 2f);
        }
        allowedHydroMask.fillCenter(64, false).limitToSymmetryRegion();

        map.getMexes()
           .stream()
           .filter(mex -> allowedHydroMask.inTeam(mex.getPosition(), false))
           .forEach(mex -> allowedHydroMask.fillCircle(mex.getPosition(), 10, false));

        for (int i = 0; i < map.getSpawnCount(); i += allowedHydroMask.getSymmetrySettings()
                                                                      .spawnSymmetry()
                                                                      .getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BooleanMask spawnHydroMask = new BooleanMask(allowedHydroMask.getSize(), random.nextLong(),
                                                         allowedHydroMask.getSymmetrySettings());
            spawnHydroMask.fillCircle(spawn.getPosition(), 25, true)
                          .fillCircle(spawn.getPosition(), 7, false)
                          .multiply(allowedHydroMask);

            placeIndividualHydros(spawnHydroMask, 1, hydroSpacing);
        }
    }

}
