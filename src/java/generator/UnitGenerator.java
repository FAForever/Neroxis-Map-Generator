package generator;

import map.BinaryMask;
import map.SCMap;
import map.Unit;
import util.Vector2f;
import util.Vector3f;

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
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        Vector2f location = spawnableCopy.getRandomPosition();
        Vector2f symLocation;
        String type = types[random.nextInt(types.length)];
        float rot = random.nextFloat();
        while (location != null) {
            symLocation = spawnableCopy.getSymmetryPoint(location);
            spawnableCopy.fillCircle(location, separation, false);
            spawnableCopy.fillCircle(symLocation, separation, false);
            Unit unit1 = new Unit(type, location, rot * (float) StrictMath.PI);
            Unit unit2 = new Unit(unit1.getType(), symLocation, unit1.getRotation() - (float) StrictMath.PI);
            map.addUnit(unit1);
            map.addUnit(unit2);
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void setUnitHeights() {
        for (Unit unit : map.getUnits()) {
            unit.setPosition(placeOnHeightmap(unit.getPosition()));
        }
    }
}
