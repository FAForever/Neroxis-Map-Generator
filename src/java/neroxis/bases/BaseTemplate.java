package neroxis.bases;

import com.faforever.commons.lua.LuaLoader;
import lombok.Value;
import neroxis.map.Army;
import neroxis.map.Group;
import neroxis.map.Symmetry;
import neroxis.map.Unit;
import neroxis.util.Vector2f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@Value
public class BaseTemplate {
    Vector2f center;
    Army army;
    Group group;
    LinkedHashMap<String, LinkedHashSet<Vector2f>> units;

    public BaseTemplate(Vector2f center, Army army, Group group, LinkedHashMap<String, LinkedHashSet<Vector2f>> units) {
        this.center = center;
        this.army = army;
        this.group = group;
        this.units = units;
    }

    public BaseTemplate(Vector2f center, Army army, Group group, String luaFile) throws IOException {
        this.center = center;
        this.army = army;
        this.group = group;
        this.units = new LinkedHashMap<>(loadUnits(luaFile));
    }

    public static LinkedHashMap<String, LinkedHashSet<Vector2f>> loadUnits(String luaFile) throws IOException {
        LinkedHashMap<String, LinkedHashSet<Vector2f>> units = new LinkedHashMap<>();
        LuaValue lua = LuaLoader.load(BaseTemplate.class.getResourceAsStream(luaFile));
        LuaTable luaUnits = lua.get("Units").checktable();
        LuaValue key = LuaValue.NIL;
        while (luaUnits.next(key) != LuaValue.NIL) {
            key = luaUnits.next(key).checkvalue(1);
            LuaValue unit = luaUnits.get(key);
            String type = unit.get("type").checkstring().toString();
            LuaTable posTable = unit.get("Position").checktable();
            Vector2f position = new Vector2f(posTable.get(1).tofloat(), posTable.get(3).tofloat());
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
                        group.addUnit(new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), group.getUnitCount()), name, new Vector2f(position).add(center), 0))));
    }

    public void flip(Symmetry symmetry) {
        units.values().forEach(positions -> positions.forEach(position -> position.flip(new Vector2f(0, 0), symmetry)));
    }
}
