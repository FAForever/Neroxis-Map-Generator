package generator;

import map.BinaryMask;
import map.SCMap;
import map.Unit;
import util.Vector2f;
import util.Vector3f;

import java.util.LinkedHashSet;
import java.util.Random;

public strictfp class UnitGenerator {
    public static final String[] STRUCTURES = {
            //Add Civilian Structures here
    };

    private final SCMap map;
    private final Random random;

    public UnitGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    private Vector3f placeOnHeightmap(Vector2f v) {
        return placeOnHeightmap(v.x, v.y);
    }

    private Vector3f placeOnHeightmap(Vector3f v) {
        return placeOnHeightmap(v.x, v.z);
    }


    private Vector3f placeOnHeightmap(float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    public void generateUnits(BinaryMask spawnable, String[] types, float separation) {
        spawnable.fillHalf(false);
        LinkedHashSet<Vector2f> coordinates = spawnable.getRandomCoordinates(separation);
        coordinates.forEach((location) -> {
            Vector2f symLocation = spawnable.getSymmetryPoint(location);
            Unit unit1 = new Unit(types[random.nextInt(types.length)], location, random.nextFloat() * (float) StrictMath.PI);
            Unit unit2 = new Unit(unit1.getType(), symLocation, spawnable.getReflectedRotation(unit1.getRotation()));
            map.addUnit(unit1);
            map.addUnit(unit2);
        });
    }

    public void setUnitHeights() {
        for (Unit unit : map.getUnits()) {
            unit.setPosition(placeOnHeightmap(unit.getPosition()));
        }
    }
}
