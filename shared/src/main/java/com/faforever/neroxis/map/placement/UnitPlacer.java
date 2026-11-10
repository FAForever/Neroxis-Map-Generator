package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.bases.BaseTemplate;
import com.faforever.neroxis.bases.BaseTemplateLoader;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.random.RandomGenerator;

public class UnitPlacer {
    public static final String[] T1_Land = {"UEL0201", "URL0107", "UAL0201", "XSL0201"};
    public static final String[] T2_Land = {"DRL0204", "URL0202", "DEL0204", "UEL0202", "UAL0202", "XAL0203", "XSL0203",
            "XSL0202"};
    public static final String[] T3_Land = {"XEL0305", "UEL0303", "URL0303", "XRL0305", "UAL0303", "XSL0303"};
    public static final String[] T2_Navy = {
            //"UES0201", does not display in game for some reason
            "URS0201",
            //"UAS0201", does not display in game for some reason
            //"XSS0201" does not display in game for some reason
    };
    public static final String[] Navy_Factory = {"ZAB9503", "ZEB9503", "ZRB9503", "ZSB9503"};

    public record WreckageReclaim(double mass, double energy) {}

    // Mass and energy values are retrieved from the blueprint files in env.nx2 and faforever.faf
    // All listed blueprints currently specify MassMult = 0.9 and EnergyMult = 0.
    public static final Map<String, WreckageReclaim> WRECKAGE_RECLAIM = Map.ofEntries(
            Map.entry("UEL0201", new WreckageReclaim(50.4, 0)),
            Map.entry("URL0107", new WreckageReclaim(46.8, 0)),
            Map.entry("UAL0201", new WreckageReclaim(46.8, 0)),
            Map.entry("XSL0201", new WreckageReclaim(48.6, 0)),
            Map.entry("DRL0204", new WreckageReclaim(180, 0)),
            Map.entry("URL0202", new WreckageReclaim(178.2, 0)),
            Map.entry("DEL0204", new WreckageReclaim(180, 0)),
            Map.entry("UEL0202", new WreckageReclaim(178.2, 0)),
            Map.entry("UAL0202", new WreckageReclaim(324, 0)),
            Map.entry("XAL0203", new WreckageReclaim(198, 0)),
            Map.entry("XSL0203", new WreckageReclaim(198, 0)),
            Map.entry("XSL0202", new WreckageReclaim(324, 0)),
            Map.entry("XEL0305", new WreckageReclaim(1152, 0)),
            Map.entry("UEL0303", new WreckageReclaim(432, 0)),
            Map.entry("URL0303", new WreckageReclaim(432, 0)),
            Map.entry("XRL0305", new WreckageReclaim(1152, 0)),
            Map.entry("UAL0303", new WreckageReclaim(756, 0)),
            Map.entry("XSL0303", new WreckageReclaim(756, 0)),
            Map.entry("URS0201", new WreckageReclaim(2025, 0)),
            Map.entry("ZAB9503", new WreckageReclaim(990, 0)),
            Map.entry("ZEB9503", new WreckageReclaim(990, 0)),
            Map.entry("ZRB9503", new WreckageReclaim(990, 0)),
            Map.entry("ZSB9503", new WreckageReclaim(990, 0)));

    public static final String[] MEDIUM_ENEMY = {"/base_template/UEFMedium.lua", "/base_template/AeonMedium.lua"};
    public static final String[] MEDIUM_RECLAIM = {
            "/base_template/CybranMediumReclaim.lua",
            "/base_template/UEFMediumReclaim.lua",
            "/base_template/CybranBrick.lua",
            "/base_template/CybranMantisBase.lua",
            "/base_template/RadarAndPDBase.lua",
            "/base_template/UEFPegenBase.lua",
            "/base_template/UEFTitans.lua"
    };
    public static final int MAX_UNIT_COUNT = 800;
    private final RandomGenerator.SplittableGenerator random;

    public UnitPlacer(RandomGenerator.SplittableGenerator random) {
        this.random = random.split();
    }

    public void placeBases(BooleanMask spawnMask, String[] templates, Army army, Group group,
                           float separation) throws IOException {
        if (templates.length > 0) {
            String templateFile = templates[random.nextInt(templates.length)];
            if (!spawnMask.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
                spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
            }
            spawnMask.limitToSymmetryRegion();
            SequencedMap<String, SequencedSet<Vector2>> units = BaseTemplateLoader.loadUnits(templateFile);
            int numUnitsInTemplate = units.values().stream().mapToInt(Collection::size).sum();
            List<Vector2> coordinates = spawnMask.getRandomCoordinates(separation)
                                                 .stream()
                                                 .limit((MAX_UNIT_COUNT - army.getNumUnits()) / numUnitsInTemplate)
                                                 .map(Vector::roundToNearestHalfPoint)
                                                 .toList();
            for (Vector2 location : coordinates) {
                BaseTemplate base = new BaseTemplate(location, units);
                base.addUnits(army, group);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector::roundToNearestHalfPoint)
                                                        .toList();
                symmetryPoints.forEach(symmetryPoint -> {
                    BaseTemplate symBase = new BaseTemplate(symmetryPoint, base.units());
                    if (!spawnMask.inTeam(symmetryPoint, false)) {
                        symBase.flip(spawnMask.getSymmetrySettings().spawnSymmetry());
                    }
                    symBase.addUnits(army, group);
                });
            }
        }
    }

    public void placeUnits(BooleanMask spawnMask, String[] types, Army army, Group group, float separation) {
        placeUnits(spawnMask, types, army, group, separation, separation);
    }

    public void placeUnits(BooleanMask spawnMask, String[] types, Army army, Group group, float minSeparation,
                           float maxSeparation) {
        if (types != null && types.length > 0) {
            spawnMask.limitToSymmetryRegion();
            List<Vector2> coordinates = spawnMask.getRandomCoordinates(minSeparation, maxSeparation)
                                                 .stream()
                                                 .limit((MAX_UNIT_COUNT - army.getNumUnits())
                                                        / spawnMask.getSymmetrySettings()
                                                                   .spawnSymmetry()
                                                                   .getNumSymPoints())
                                                 .map(Vector2::roundToNearestHalfPoint)
                                                 .toList();
            String type = types[random.nextInt(types.length)];
            float rot = random.nextFloat() * 3.14159f;
            for (Vector2 location : coordinates) {
                int groupID = group.getUnitCount();
                Unit unit = new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), groupID), type,
                                     location, rot);
                group.addUnit(unit);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(unit.getPosition(), SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector2::roundToNearestHalfPoint)
                                                        .toList();
                List<Float> symmetryRotation = spawnMask.getSymmetryRotations(unit.getRotation());
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    group.addUnit(
                            new Unit(String.format("%s %s Unit %d sym %s", army.getId(), group.getId(), groupID, i),
                                     type, symmetryPoints.get(i), symmetryRotation.get(i)));
                }
            }
        }
    }
}
