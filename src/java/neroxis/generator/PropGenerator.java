package neroxis.generator;

import neroxis.map.BinaryMask;
import neroxis.map.Prop;
import neroxis.map.SCMap;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public strictfp class PropGenerator {

    private final SCMap map;
    private final Random random;

    public PropGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateProps(BinaryMask spawnMask, String[] paths, float separation) {
        generateProps(spawnMask, paths, separation, separation);
    }

    public void generateProps(BinaryMask spawnMask, String[] paths, float minSeparation, float maxSeparation) {
        if (paths != null && paths.length > 0) {
            spawnMask.limitToSymmetryRegion();
            LinkedList<Vector2f> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            coordinates.forEach((location) -> {
                location.add(.5f, .5f);
                Prop prop = new Prop(paths[random.nextInt(paths.length)], location, random.nextFloat() * (float) StrictMath.PI);
                map.addProp(prop);
                ArrayList<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
                ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(prop.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i), symmetryRotation.get(i));
                    map.addProp(symProp);
                }
            });
        }
    }

}
