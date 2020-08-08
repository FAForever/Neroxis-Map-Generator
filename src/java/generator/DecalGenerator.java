package generator;

import map.BinaryMask;
import map.Decal;
import map.SCMap;
import util.Vector2f;

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
        spawnableCopy.fillHalf(false);
        Vector2f location = spawnableCopy.getRandomPosition();
        Vector2f symLocation;
        while (location != null) {
            symLocation = spawnableCopy.getSymmetryPoint(location);
            spawnableCopy.fillCircle(location, separation, false);
            Decal decal1 = new Decal(paths[random.nextInt(paths.length)], location, random.nextFloat() * (float) StrictMath.PI, scale, 2400);
            Decal decal2 = new Decal(decal1.getPath(), symLocation, spawnableCopy.getReflectedRotation(decal1.getRotation()), scale, 2400);
            map.addDecal(decal1);
            map.addDecal(decal2);
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void setDecalHeights() {
        for (Decal decal : map.getDecals()) {
            decal.setPosition(placeOnHeightmap(map, decal.getPosition()));
        }
    }
}
