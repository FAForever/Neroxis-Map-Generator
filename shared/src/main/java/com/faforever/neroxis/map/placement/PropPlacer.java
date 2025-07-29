package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropPlacer {
    private final SCMap map;
    private final Random random;

    public PropPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeProps(BooleanMask spawnMask, List<String> paths, float separation, boolean isBoulder) {
        placeProps(spawnMask, paths, separation, separation, isBoulder);
    }

    public void placeProps(BooleanMask spawnMask, List<String> paths, float minSeparation, float maxSeparation,
                           boolean isBoulder) {
        if (paths != null && !paths.isEmpty()) {
            spawnMask.limitToSymmetryRegion();
            List<Vector2> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.stream().map(Vector2::roundToNearestHalfPoint).forEach(location -> {
                Prop prop = new Prop(paths.get(random.nextInt(paths.size())), location,
                                     random.nextFloat() * (float) StrictMath.PI, isBoulder);
                map.addProp(prop);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector2::roundToNearestHalfPoint)
                                                        .toList();
                List<Float> symmetryRotation = spawnMask.getSymmetryRotations(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i), isBoulder);
                    map.addProp(symProp);
                }
            });
        }
    }

    public void placeProps(FloatMask heatMap, PropMaterials propMaterials, float reclaimDensity) {
        FloatMask rockAreas = new FloatMask(heatMap.getSize(), random.nextLong(), heatMap.getSymmetrySettings(), "Rock Areas", null);
        rockAreas.addPerlinNoise(64, 1f);
        BooleanMask rockAreaMask = rockAreas.copyAsBooleanMask(0.7f);

        heatMap.loopInSymmetryRegion(SymmetryType.SPAWN, (x, y) -> {
            float heat = heatMap.get(x, y);
            float xJitter = x + random.nextFloat(1f) - 0.5f;
            float yJitter = y + random.nextFloat(1f) - 0.5f;
            Vector2 location = new Vector2(xJitter, yJitter);
            ArrayList<Vector2> origAndSymmetryPoints = new ArrayList<>(
                    heatMap.getSymmetryPoints(location, SymmetryType.SPAWN)
                           .stream()
                           .map(Vector::roundToNearestHalfPoint)
                           .toList()
            );
            origAndSymmetryPoints.add(location);

            String propPath = null;
            float propRotation = 0;
            boolean propIsBoulder = false;
            if (heat > 0.95f) {
                if (random.nextInt(1000) < 10 && random.nextFloat() < reclaimDensity) {
                    List<String> paths = propMaterials.boulders();
                    if (!paths.isEmpty()) {
                        propPath = paths.get(random.nextInt(paths.size()));
                        propRotation = random.nextFloat() * (float) StrictMath.PI;
                        propIsBoulder = true;
                    }
                }
            } else if (heat > 0.87f) {
                if (rockAreaMask.get(x, y) && random.nextInt(100) < 70 * reclaimDensity) {
                    List<String> paths = propMaterials.rocks();
                    if (!paths.isEmpty()) {
                        propPath = paths.get(random.nextInt(paths.size()));
                        propRotation = random.nextFloat() * (float) StrictMath.PI;
                        propIsBoulder = false;
                    }
                }
            } else if (heat > 0 && heat < 0.75f) {
                if (random.nextFloat(1f) < heat && random.nextInt(100) < 10 * reclaimDensity) {
                    List<String> paths = propMaterials.treeGroups();
                    if (!paths.isEmpty()) {
                        propPath = paths.get(random.nextInt(paths.size()));
                        propRotation = random.nextFloat() * (float) StrictMath.PI;
                        propIsBoulder = false;
                    }
                }
            }

            if (propPath != null) {
                String finalPropPath = propPath;
                float finalPropRotation = propRotation;
                boolean finalPropIsBoulder = propIsBoulder;

                if (areAllPointsWithinPropBounds(origAndSymmetryPoints)) {
                    origAndSymmetryPoints.forEach((point) -> {
                        Prop prop = new Prop(finalPropPath, new Vector2(point.x(), point.y()), finalPropRotation, finalPropIsBoulder);
                        map.addProp(prop);
                    });
                }
            }
        });
    }

    private boolean areAllPointsWithinPropBounds(List<Vector2> locations) {
        int mapSize = map.getSize();
        final int PADDING = 10;

        return locations
                .stream()
                .noneMatch(location -> location.x() < PADDING || location.x() > mapSize-PADDING || location.y() < PADDING || location.y() > mapSize-PADDING);
    }
}
