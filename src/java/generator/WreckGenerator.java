package generator;

import map.BinaryMask;
import map.SCMap;
import map.Unit;
import util.Vector2f;

import java.util.LinkedHashSet;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

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
            //"UES0201", does not display in game for some reason
            "URS0201",
            //"UAS0201", does not display in game for some reason
            //"XSS0201" does not display in game for some reason
    };
    public static final String[] Navy_Factory = {
            "ZAB9503",
            "ZEB9503",
            "ZRB9503",
            "ZSB9503"
    };

    private final SCMap map;
    private final Random random;

    public WreckGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateWrecks(BinaryMask spawnable, String[] types, float separation) {
        LinkedHashSet<Vector2f> coordinates = spawnable.getRandomCoordinates(separation);
        String type = types[random.nextInt(types.length)];
        float rot = random.nextFloat() * 3.14159f;
        coordinates.forEach((location) -> {
            location.add(.5f, .5f);
            Vector2f symLocation = spawnable.getSymmetryPoint(location);
            Unit wreck1 = new Unit(type, location, rot);
            Unit wreck2 = new Unit(wreck1.getType(), symLocation, spawnable.getReflectedRotation(wreck1.getRotation()));
            map.addWreck(wreck1);
            map.addWreck(wreck2);
        });
    }

    public void setWreckHeights() {
        for (Unit wreck : map.getWrecks()) {
            wreck.setPosition(placeOnHeightmap(map, wreck.getPosition()));
        }
    }
}
