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

    private Vector3f placeOnHeightmap(float x, float z, Vector3f v) {
        v.x = x;
        v.z = z;
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    private Vector3f placeOnHeightmap(float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    public void generateUnits(BinaryMask spawnable, String[] types, float separation) {
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        Vector2f location = spawnableCopy.getRandomPosition();
        String type = types[random.nextInt(types.length)];
        float rot = random.nextFloat();
        while (location != null) {
            spawnableCopy.fillCircle(location.x, location.y, separation, false);
            spawnableCopy.fillCircle(map.getSize() - location.x, map.getSize() - location.y, separation, false);
            Unit unit1 = new Unit(type, placeOnHeightmap(location.x, location.y), rot);
            Unit unit2 = new Unit(unit1.getType(), placeOnHeightmap(map.getSize() - location.x, map.getSize() - location.y), unit1.getRotation() + 0.5f);
            map.addUnit(unit1);
            map.addUnit(unit2);
            location = spawnableCopy.getRandomPosition();
        }
    }
}
