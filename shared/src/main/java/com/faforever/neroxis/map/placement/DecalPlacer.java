package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Decal;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.List;
import java.util.random.RandomGenerator;

public class DecalPlacer {
    private final SCMap map;
    private final RandomGenerator.SplittableGenerator random;

    public DecalPlacer(SCMap map, RandomGenerator.SplittableGenerator random) {
        this.map = map;
        this.random = random.split();
    }

    public void placeDecals(BooleanMask spawnMask, List<String> paths, float minSeparation, float maxSeparation,
                            float minScale, float maxScale) {
        if (!paths.isEmpty()) {
            BooleanMask spawnMaskCopy = spawnMask.copy();
            spawnMaskCopy.limitToSymmetryRegion();
            spawnMaskCopy.getRandomCoordinates(minSeparation, maxSeparation)
                         .stream()
                         .map(Vector2::roundToNearestHalfPoint)
                         .forEach((location) -> {
                             float scale = random.nextFloat() * (maxScale - minScale) + minScale;
                             Vector3 rotation = new Vector3(0f, random.nextFloat() * (float) StrictMath.PI, 0f);
                             Decal decal = new Decal(paths.get(random.nextInt(paths.size())),
                                                     location.roundToNearestHalfPoint(), rotation, scale, 1000);
                             map.addDecal(decal);
                             List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(decal.getPosition(),
                                                                                        SymmetryType.SPAWN)
                                                                     .stream()
                                                                     .map(Vector2::roundToNearestHalfPoint)
                                                                     .toList();
                             List<Float> symmetryRotation = spawnMask.getSymmetryRotations(decal.getRotation().y());
                             for (int i = 0; i < symmetryPoints.size(); i++) {
                                 Vector3 symVectorRotation = new Vector3(decal.getRotation().x(),
                                                                         symmetryRotation.get(i),
                                                                         decal.getRotation().z());
                                 Decal symDecal = new Decal(decal.getPath(),
                                                            symmetryPoints.get(i),
                                                            symVectorRotation, scale, decal.getCutOffLOD());
                                 map.addDecal(symDecal);
                             }
                         });
        }
    }
}
