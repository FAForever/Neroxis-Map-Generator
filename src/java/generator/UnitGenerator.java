package generator;

import bases.BaseTemplate;
import map.*;
import util.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class UnitGenerator {
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

    public void generateBases(BinaryMask spawnable, String[] templates, Army army, Group group, float separation) {
        String luaFile = templates[random.nextInt(templates.length)];
        spawnable.limitToSpawnRegion();
        LinkedList<Vector2f> coordinates = spawnable.getRandomCoordinates(separation);
        coordinates.forEach((location) -> {
            try {
                BaseTemplate base = new BaseTemplate(location, army, group, luaFile);
                base.addUnits();
                ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(location);
                symmetryPoints.forEach(symmetryPoint -> {
                    BaseTemplate symBase = new BaseTemplate(symmetryPoint.getLocation(), army, group, base.getUnits());
                    symBase.flip(symmetryPoint.getSymmetry());
                    symBase.addUnits();
                });
            } catch (IOException e) {
                e.printStackTrace();
                System.out.printf("An error occured while parsing the following base: %s\n", luaFile);
                System.exit(1);
            }
        });
    }

    public void generateUnits(BinaryMask spawnable, String[] types, Army army, Group group, float separation) {
        spawnable.limitToSpawnRegion();
        LinkedList<Vector2f> coordinates = spawnable.getRandomCoordinates(separation);
        String type = types[random.nextInt(types.length)];
        float rot = random.nextFloat() * 3.14159f;
        coordinates.forEach((location) -> {
            location.add(.5f, .5f);
            int groupID = group.getUnitCount();
            Unit unit = new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), groupID), type, location, rot);
            group.addUnit(unit);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(unit.getPosition());
            ArrayList<Float> symmetryRotation = spawnable.getSymmetryRotation(unit.getRotation());
            for (int i = 0; i < symmetryPoints.size(); i++) {
                group.addUnit(new Unit(String.format("sym %s %s %s Unit %d", i, army.getId(), group.getId(), groupID), type, symmetryPoints.get(i).getLocation(), symmetryRotation.get(i)));
            }
        });
    }

    public void setUnitHeights() {
        for (Army army : map.getArmies()) {
            for (Group group : army.getGroups()) {
                for (Unit unit : group.getUnits()) {
                    unit.setPosition(placeOnHeightmap(map, unit.getPosition()));
                }
            }
        }
    }
}
