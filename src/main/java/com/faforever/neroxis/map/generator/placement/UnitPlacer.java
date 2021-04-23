package com.faforever.neroxis.map.generator.placement;

import com.faforever.neroxis.bases.BaseTemplate;
import com.faforever.neroxis.map.*;
import com.faforever.neroxis.util.Vector2f;

import java.io.IOException;
import java.util.*;

public strictfp class UnitPlacer {
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

    private final Random random;

    public UnitPlacer(long seed) {
        random = new Random(seed);
    }

    public void placeBases(BooleanMask spawnMask, String[] templates, Army army, Group group, float separation) throws IOException {
        if (templates != null && templates.length > 0) {
            String luaFile = templates[random.nextInt(templates.length)];
            if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
                spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
            }
            spawnMask.limitToSymmetryRegion();
            LinkedList<Vector2f> coordinates = spawnMask.getRandomCoordinates(separation);
            LinkedHashMap<String, LinkedHashSet<Vector2f>> units = BaseTemplate.loadUnits(luaFile);
            coordinates.forEach((location) -> {
                BaseTemplate base = new BaseTemplate(location, army, group, units);
                base.addUnits();
                List<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> {
                    BaseTemplate symBase = new BaseTemplate(symmetryPoint, army, group, base.getUnits());
                    if (!spawnMask.inTeam(symmetryPoint, false)) {
                        symBase.flip(spawnMask.getSymmetrySettings().getSpawnSymmetry());
                    }
                    symBase.addUnits();
                });
            });
        }
    }

    public void placeUnits(BooleanMask spawnMask, String[] types, Army army, Group group, float separation) {
        placeUnits(spawnMask, types, army, group, separation, separation);
    }

    public void placeUnits(BooleanMask spawnMask, String[] types, Army army, Group group, float minSeparation, float maxSeparation) {
        if (types != null && types.length > 0) {
            spawnMask.limitToSymmetryRegion();
            LinkedList<Vector2f> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation);
            String type = types[random.nextInt(types.length)];
            float rot = random.nextFloat() * 3.14159f;
            coordinates.forEach((location) -> {
                location.add(.5f, .5f);
                int groupID = group.getUnitCount();
                Unit unit = new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), groupID), type, location, rot);
                group.addUnit(unit);
                List<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(unit.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
                ArrayList<Float> symmetryRotation = spawnMask.getSymmetryRotation(unit.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    group.addUnit(new Unit(String.format("%s %s Unit %d sym %s", army.getId(), group.getId(), groupID, i), type, symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            });
        }
    }

}
