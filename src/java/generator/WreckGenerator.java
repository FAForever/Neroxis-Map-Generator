package generator;

import map.BinaryMask;
import map.SCMap;
import map.Unit;
import util.Vector2f;
import util.Vector3f;

import java.util.Random;

public strictfp class WreckGenerator {
    public static final String[] T1_Land = {
            "UEL0201",
            "URL0107",
            "UAL0201",
            "XSL0201"
    };
    public static final String[] T2_Land = {
            "DRL0204",
            "URL0202",
            "DEL0204",
            "UEL0202",
            "UAL0202",
            "XAL0203",
            "XSL0203",
            "XSL0202"
    };
    public static final String[] T3_Land = {
            "XEL0305",
            "UEL0303",
            "URL0303",
            "XRL0305",
            "UAL0303",
            "XSL0303"
    };
    public static final String[] T2_Navy = {
            "UES0201",
            "URS0201",
            "XSS0201",
            "UAS0201"
    };
    public static final String[] Navy_Factory = {
            "UEB0103",
            "URB0103",
            "UAB0103",
            "XSB0103"
    };

    private final SCMap map;
    private final Random random;

    public WreckGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    private Vector3f placeOnHeightmap(float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[] { 0 })[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    public void generateWrecks(BinaryMask spawnable, String[] types, float separation) {
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        Vector2f location = spawnableCopy.getRandomPosition();
        String type = types[random.nextInt(types.length)];
        float rot = random.nextFloat()*3.14159f;
        while (location != null) {
            spawnableCopy.fillCircle(location.x, location.y, separation, false);
            spawnableCopy.fillCircle(map.getSize() - location.x, map.getSize() - location.y, separation, false);
            Unit wreck1 = new Unit(type, placeOnHeightmap(location.x, location.y), rot);
            Unit wreck2 = new Unit(wreck1.getType(), placeOnHeightmap(map.getSize() - location.x, map.getSize() - location.y), wreck1.getRotation() - 3.14159f);
            map.addWreck(wreck1);
            map.addWreck(wreck2);
            location = spawnableCopy.getRandomPosition();
        }
    }
}
