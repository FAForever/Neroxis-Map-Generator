package com.faforever.neroxis.bases;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.LuaLoader;
import com.faforever.neroxis.util.serial.biome.SCUnitSet;
import com.faforever.neroxis.util.vector.Vector2;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public record BaseTemplate(Vector2 center, LinkedHashMap<String, LinkedHashSet<Vector2>> units) {
    public static LinkedHashMap<String, LinkedHashSet<Vector2>> loadUnits(String file) throws IOException {
        if (file.endsWith(".lua")) {
            return loadUnitsFromLua(file);
        } else if (file.endsWith(".scunits")) {
            return loadUnitsFromSCUnits(file);
        }
        throw new IllegalArgumentException("File format not valid");
    }

    private static LinkedHashMap<String, LinkedHashSet<Vector2>> loadUnitsFromLua(String luaFile) throws IOException {
        LinkedHashMap<String, LinkedHashSet<Vector2>> units = new LinkedHashMap<>();
        LuaValue lua = LuaLoader.load(BaseTemplate.class.getResourceAsStream(luaFile));
        LuaTable luaUnits = lua.get("Units").checktable();
        LuaValue key = LuaValue.NIL;
        while (luaUnits.next(key) != LuaValue.NIL) {
            key = luaUnits.next(key).checkvalue(1);
            LuaValue unit = luaUnits.get(key);
            String type = unit.get("type").checkstring().toString();
            LuaTable posTable = unit.get("Position").checktable();
            Vector2 position = new Vector2(posTable.get(1).tofloat(), posTable.get(3).tofloat());
            if (units.containsKey(type)) {
                units.get(type).add(position);
            } else {
                units.put(type, new LinkedHashSet<>(List.of(position)));
            }
        }
        return units;
    }

    private static LinkedHashMap<String, LinkedHashSet<Vector2>> loadUnitsFromSCUnits(
            String scUnitsFile) throws IOException {
        LinkedHashMap<String, LinkedHashSet<Vector2>> units = new LinkedHashMap<>();
        SCUnitSet scUnitSet = FileUtil.deserialize(BaseTemplate.class.getResourceAsStream(scUnitsFile),
                                                   SCUnitSet.class);
        for (SCUnitSet.SCUnit unit : scUnitSet.Units()) {
            unit.pos().subtract(scUnitSet.Center()).multiply(10f).roundXYToNearestHalfPoint();
            if (units.containsKey(unit.ID())) {
                units.get(unit.ID()).add(new Vector2(unit.pos()));
            } else {
                units.put(unit.ID(), new LinkedHashSet<>(List.of(new Vector2(unit.pos()))));
            }
        }
        return units;
    }

    public void addUnits(Army army, Group group) {
        units.forEach((name, positions) -> positions.forEach(position -> group.addUnit(
                new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), group.getUnitCount()), name,
                         new Vector2(position).add(center), 0))));
    }

    public void flip(Symmetry symmetry) {
        units.values().forEach(positions -> positions.forEach(position -> position.flip(new Vector2(0, 0), symmetry)));
    }
}
