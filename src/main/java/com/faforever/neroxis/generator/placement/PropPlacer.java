package com.faforever.neroxis.generator.placement;

import com.faforever.neroxis.map.BooleanMask;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public strictfp class PropPlacer {

    private final SCMap map;
    private final Random random;

    public PropPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeProps(BooleanMask spawnMask, String[] paths, float separation) {
        placeProps(spawnMask, paths, separation, separation);
    }

    public void placeProps(BooleanMask spawnMask, String[] paths, float minSeparation, float maxSeparation) {
        if (paths != null && paths.length > 0) {
            spawnMask.limitToSymmetryRegion();
            LinkedList<Vector2f> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.forEach((location) -> {
                location.add(.5f, .5f);
                Prop prop = new Prop(paths[random.nextInt(paths.length)], location, random.nextFloat() * (float) StrictMath.PI);
                map.addProp(prop);
                List<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
                ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i));
                    map.addProp(symProp);
                }
            });
        }
    }

}
