package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PropPlacer {
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
            List<Vector2> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.forEach((location) -> {
                location.roundToNearestHalfPoint();
                Prop prop = new Prop(paths[random.nextInt(paths.length)], location,
                                     random.nextFloat() * (float) StrictMath.PI);
                map.addProp(prop);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i));
                    map.addProp(symProp);
                }
            });
        }
    }
}
