package generator;

import bases.Army;
import bases.BaseTemplate;
import map.BinaryMask;
import map.SCMap;
import map.Unit;
import util.Vector2f;
import util.Vector3f;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Random;

public strictfp class UnitGenerator {
    public static final String[] MEDIUM_ENEMY = {
            "/base_template/UEFMedium.lua",
            "/base_template/AeonMedium.lua"
    };

    public static final String[] MEDIUM_RECLAIM = {
            "/base_template/CybranMediumReclaim.lua",
            "/base_template/UEFMediumReclaim.lua"
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

    public void generateBases(BinaryMask spawnable, String[] templates, Army army, float separation) {
        String luaFile = templates[random.nextInt(templates.length)];
        spawnable.fillHalf(false);
        LinkedHashSet<Vector2f> coordinates = spawnable.getRandomCoordinates(separation);
        coordinates.forEach((location) -> {
            Vector2f symLocation = spawnable.getSymmetryPoint(location);
            try {
                BaseTemplate base1 = new BaseTemplate(location, army, luaFile);
                BaseTemplate base2 = new BaseTemplate(symLocation, army, luaFile);
                base2.flip(spawnable.getSymmetryHierarchy().getSpawnSymmetry());
                base1.addUnits(map);
                base2.addUnits(map);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                System.out.printf("An error occured while parsing the following base: %s\n", luaFile);
                System.exit(1);
            }
        });
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
