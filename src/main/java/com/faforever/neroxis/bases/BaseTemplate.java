package com.faforever.neroxis.bases;

import com.faforever.commons.lua.LuaLoader;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.Vector2;
import lombok.Value;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@Value
public strictfp class BaseTemplate {
    Vector2 center;
    Army army;
    Group group;
    LinkedHashMap<String, LinkedHashSet<Vector2>> units;

    public BaseTemplate(Vector2 center, Army army, Group group, LinkedHashMap<String, LinkedHashSet<Vector2>> units) {
        this.center = center;
        this.army = army;
        this.group = group;
        this.units = units;
    }

    public static LinkedHashMap<String, LinkedHashSet<Vector2>> loadUnits(String luaFile) throws IOException {
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
                units.put(type, new LinkedHashSet<>(Collections.singletonList(position)));
            }
        }
        return units;
    }

    public void addUnits() {
        units.forEach((name, positions) ->
                positions.forEach(position ->
                        group.addUnit(new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), group.getUnitCount()), name, new Vector2(position).add(center), 0))));
    }

    public void flip(Symmetry symmetry) {
        units.values().forEach(positions -> positions.forEach(position -> position.flip(new Vector2(0, 0), symmetry)));
    }
}
