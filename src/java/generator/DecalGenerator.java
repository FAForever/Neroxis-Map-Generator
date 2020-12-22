package generator;

import map.*;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class DecalGenerator {
    public static final String[] EROSION = {
            "/env/Common/decals/erosion_normals.dds",
            "/env/Common/decals/erosion000_normals.dds",
            "/env/Common/decals/erosion002_normals.dds",
            "/env/Common/decals/erosion006_normals.dds",
            "/env/Common/decals/erosion007_normals.dds",
            "/env/Common/decals/erosion008_normals.dds",
            "/env/Common/decals/erosion009_normals.dds",
            "/env/Common/decals/erosion011_normals.dds",
            "/env/Common/decals/erosion011a_normals.dds",
            "/env/Common/decals/erosion011b_normals.dds",
            "/env/Common/decals/erosion011c_normals.dds",
            "/env/Common/decals/erosion016_normals.dds",
            "/env/Common/decals/erosion017_normals.dds",
            "/env/Common/decals/erosion018_normals.dds",
            "/env/Common/decals/erosion018a_normals.dds",
            "/env/Common/decals/erosion018b_normals.dds",
            "/env/Common/decals/erosion018c_normals.dds",
            "/env/Common/decals/erosion018d_normals.dds",
            "/env/Common/decals/erosion021a_normals.dds"

    };
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

    public void generateDecals(BinaryMask spawnable, String[] paths, float separation, float scale) {
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        spawnableCopy.limitToSymmetryRegion();
        LinkedList<Vector2f> coordinates = spawnableCopy.getRandomCoordinates(separation);
        boolean flipX;
        boolean flipZ;
        switch (spawnable.getSymmetrySettings().getTeamSymmetry()) {
            case XZ, ZX -> {
                flipX = true;
                flipZ = true;
            }
            case X -> {
                flipZ = true;
                flipX = false;
            }
            case Z -> {
                flipZ = false;
                flipX = true;
            }
            default -> {
                flipZ = false;
                flipX = false;
            }
        }
        coordinates.forEach((location) -> {
            location.add(.5f, .5f);
            Vector3f rotation = new Vector3f(0f, random.nextFloat() * (float) StrictMath.PI, 0f);
            Decal decal = new Decal(paths[random.nextInt(paths.length)], location, rotation, scale, 2400);
            map.addDecal(decal);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(decal.getPosition(), SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
            ArrayList<Float> symmetryRotation = spawnable.getSymmetryRotation(decal.getRotation().y);
            for (int i = 0; i < symmetryPoints.size(); i++) {
                Vector3f symVectorRotation = new Vector3f(decal.getRotation().x, symmetryRotation.get(i), decal.getRotation().z);
                Decal symDecal = new Decal(decal.getPath(), symmetryPoints.get(i).getLocation(), symVectorRotation, scale, decal.getCutOffLOD());
                map.addDecal(symDecal);
            }
        });
    }

    public void setDecalHeights() {
        for (Decal decal : map.getDecals()) {
            decal.setPosition(placeOnHeightmap(map, decal.getPosition()));
        }
    }
}
