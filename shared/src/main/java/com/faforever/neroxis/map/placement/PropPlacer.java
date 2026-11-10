package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.serial.biome.PropMaterials.PropMaterial;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.random.RandomGenerator;

public class PropPlacer {
    private final SCMap map;
    private final RandomGenerator.SplittableGenerator random;

    public PropPlacer(SCMap map, RandomGenerator.SplittableGenerator random) {
        this.map = map;
        this.random = random.split();
    }

    public void placeProps(BooleanMask spawnMask, List<PropMaterial> materials, float separation, boolean isBoulder) {
        placeProps(spawnMask, materials, separation, separation, isBoulder);
    }

    public void placeProps(BooleanMask spawnMask, List<PropMaterial> materials, float minSeparation, float maxSeparation,
                           boolean isBoulder) {
        if (!materials.isEmpty()) {
            spawnMask.limitToSymmetryRegion();
            List<Vector2> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.stream().map(Vector2::roundToNearestHalfPoint).forEach(location -> {
                PropMaterial material = materials.get(random.nextInt(materials.size()));
                Prop prop = new Prop(material.path(), location,
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
}
