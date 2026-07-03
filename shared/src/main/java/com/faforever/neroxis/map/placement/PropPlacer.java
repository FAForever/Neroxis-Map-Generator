package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.Random;

public class PropPlacer {
    protected final SCMap map;
    protected final Random random;

    public PropPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeProps(BooleanMask propMask, List<String> paths, float separation, boolean isBoulder) {
        placeProps(propMask, paths, separation, separation, isBoulder);
    }

    public void placeProps(BooleanMask propMask, List<String> paths, float minSeparation, float maxSeparation,
                           boolean isBoulder) {
        if (paths != null && !paths.isEmpty()) {
            propMask.limitToSymmetryRegion();
            List<Vector2> coordinates = propMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.stream().map(Vector2::roundToNearestHalfPoint).forEach(location -> {
                Prop prop = new Prop(paths.get(random.nextInt(paths.size())), location,
                                     random.nextFloat() * (float) StrictMath.PI, isBoulder);
                map.addProp(prop);
                List<Vector2> symmetryPoints = propMask.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector2::roundToNearestHalfPoint)
                                                        .toList();
                List<Float> symmetryRotation = propMask.getSymmetryRotations(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i), isBoulder);
                    map.addProp(symProp);
                }
            });
        }
    }
}
