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
        List<String> boulderPaths = propMaterials.boulders();
        List<String> rockPaths = propMaterials.rocks();
        List<String> treeGroupPaths = propMaterials.treeGroups();

        float boulderDensityFactor = 0.05f;
        float rockDensityFactor = 0.5f;
        float treeGroupDensityFactor = 0.1f;

        heatMap.loopInSymmetryRegion(SymmetryType.SPAWN, (x, y) -> {
            float heat = heatMap.get(x, y);
            if (heat > 0) {
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
                if (areAllPointsWithinPropBounds(origAndSymmetryPoints)) {
                    String propPath = null;
                    float propRotation = 0;
                    boolean propIsBoulder = false;

                    float boulder = bellCurve(heat, 1.0f, 0.1f);
                    float rock = bellCurve(heat, 0.7f, 0.04f);
                    float tree = bellCurve(heat, 0.2f, 0.1f);

                    if (random.nextFloat() < boulder &&
                        random.nextFloat() < boulderDensityFactor &&
                        random.nextFloat() < reclaimDensity) {
                        if (!boulderPaths.isEmpty()) {
                            propPath = boulderPaths.get(random.nextInt(boulderPaths.size()));
                            propRotation = random.nextFloat() * (float) StrictMath.PI;
                            propIsBoulder = true;
                        }
                    }
                    if (random.nextFloat() < rock &&
                        random.nextFloat() < rockDensityFactor &&
                        random.nextFloat() < reclaimDensity) {
                        if (!rockPaths.isEmpty()) {
                            propPath = rockPaths.get(random.nextInt(rockPaths.size()));
                            propRotation = random.nextFloat() * (float) StrictMath.PI;
                            propIsBoulder = false;
                        }
                    }
                    if (random.nextFloat() < tree &&
                        random.nextFloat() < treeGroupDensityFactor &&
                        random.nextFloat() < reclaimDensity) {
                        if (!treeGroupPaths.isEmpty()) {
                            propPath = treeGroupPaths.get(random.nextInt(treeGroupPaths.size()));
                            propRotation = random.nextFloat() * (float) StrictMath.PI;
                            propIsBoulder = false;
                        }
                    }


                    if (propPath != null) {
                        String finalPropPath = propPath;
                        float finalPropRotation = propRotation;
                        boolean finalPropIsBoulder = propIsBoulder;

                        origAndSymmetryPoints.forEach((point) -> {
                            Prop prop = new Prop(finalPropPath, point, finalPropRotation, finalPropIsBoulder);
                            map.addProp(prop);
                        });
                    }
                }
            }
        });
    }

    private static float bellCurve(float value, float center, float sigma) {
        if (value <= 0) {
            return 0;
        }
        float d = value - center;
        return (float) Math.exp(-(d * d) / (2f * sigma * sigma));
    }

    private boolean areAllPointsWithinPropBounds(List<Vector2> locations) {
        int mapSize = map.getSize();
        final int PADDING = 10;

        return locations
                .stream()
                .noneMatch(location -> location.x() < PADDING ||
                                       location.x() > mapSize - PADDING ||
                                       location.y() < PADDING ||
                                       location.y() > mapSize - PADDING);
    }
}
