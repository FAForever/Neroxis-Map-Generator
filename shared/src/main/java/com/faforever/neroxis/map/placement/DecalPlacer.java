package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Decal;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DecalPlacer {
    private final SCMap map;
    private final Random random;

    public DecalPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeDecals(BooleanMask spawnMask, List<String> paths, float minSeparation, float maxSeparation,
                            float minScale, float maxScale) {
        if (paths != null && !paths.isEmpty()) {
            BooleanMask spawnMaskCopy = spawnMask.copy();
            spawnMaskCopy.limitToSymmetryRegion(SymmetryType.TEAM);
            List<Vector2> coordinates = spawnMaskCopy.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.forEach((location) -> {
                float scale = random.nextFloat() * (maxScale - minScale) + minScale;
                location.roundToNearestHalfPoint();
                Vector3 rotation = new Vector3(0f, random.nextFloat() * (float) StrictMath.PI, 0f);
                Decal decal = new Decal(paths.get(random.nextInt(paths.size())), location, rotation, scale, 1000);
                map.addDecal(decal);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(decal.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(decal.getRotation().getY());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector3 symVectorRotation = new Vector3(decal.getRotation().getX(), symmetryRotation.get(i),
                                                            decal.getRotation().getZ());
                    Decal symDecal = new Decal(decal.getPath(), symmetryPoints.get(i), symVectorRotation, scale,
                                               decal.getCutOffLOD());
                    map.addDecal(symDecal);
                }
            });
        }
    }
}
