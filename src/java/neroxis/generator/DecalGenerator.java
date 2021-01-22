package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public strictfp class DecalGenerator {
    public static final String[] ROCKS = {
            "/env/Common/decals/Rock001_normals.dds",
            "/env/Common/decals/Rock002_normals.dds",
            "/env/Common/decals/Rock003_normals.dds",
            "/env/Common/decals/Rock004_normals.dds",
            "/env/Common/decals/Rock005_normals.dds",
            "/env/Common/decals/Rock006_normals.dds",
            "/env/Common/decals/Rock007_normals.dds",
            "/env/Common/decals/Rock008_normals.dds",
            "/env/Common/decals/Rock009_normals.dds",
            "/env/Common/decals/Rock010_normals.dds",
            "/env/Common/decals/Rock011_normals.dds",
            "/env/Common/decals/Rock012_normals.dds",
            "/env/Common/decals/Rock013_normals.dds",
    };
    public static final String[] INT = {
            "/env/Common/decals/Int001_normals.dds",
            "/env/Common/decals/Int002_normals.dds",
            "/env/Common/decals/Int003_normals.dds",
            "/env/Common/decals/Int004_normals.dds",
            "/env/Common/decals/Int005_normals.dds",
            "/env/Common/decals/Int006_normals.dds",
            "/env/Common/decals/Int007_normals.dds",
            "/env/Common/decals/Int008_normals.dds",
            "/env/Common/decals/Int009_normals.dds"
    };

    private final SCMap map;
    private final Random random;

    public DecalGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateDecals(BinaryMask spawnMask, String[] paths, float separation, float scale) {
        BinaryMask spawnMaskCopy = new BinaryMask(spawnMask, random.nextLong());
        spawnMaskCopy.limitToSymmetryRegion();
        LinkedList<Vector2f> coordinates = spawnMaskCopy.getRandomCoordinates(separation);
        coordinates.forEach((location) -> {
            location.add(.5f, .5f);
            Vector3f rotation = new Vector3f(0f, random.nextFloat() * (float) StrictMath.PI, 0f);
            Decal decal = new Decal(paths[random.nextInt(paths.length)], location, rotation, scale, 2400);
            map.addDecal(decal);
            ArrayList<SymmetryPoint> symmetryPoints = spawnMask.getSymmetryPoints(decal.getPosition(), SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
            ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(decal.getRotation().getY());
            for (int i = 0; i < symmetryPoints.size(); i++) {
                Vector3f symVectorRotation = new Vector3f(decal.getRotation().getX(), symmetryRotation.get(i), decal.getRotation().getZ());
                Decal symDecal = new Decal(decal.getPath(), symmetryPoints.get(i).getLocation(), symVectorRotation, scale, decal.getCutOffLOD());
                map.addDecal(symDecal);
            }
        });
    }

}
