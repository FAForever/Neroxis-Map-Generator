package bases;

import com.faforever.commons.lua.LuaLoader;
import lombok.Data;
import map.SCMap;
import map.Symmetry;
import map.Unit;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import util.Vector2f;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@Data
public class BaseTemplate {
    private final Vector2f center;
    private final Army army;
    private final String luaFile;
    private final LinkedHashMap<String, LinkedHashSet<Vector2f>> structures = new LinkedHashMap<>();

    public BaseTemplate(Vector2f center, Army army, String luaFile) throws IOException, URISyntaxException {
        this.center = center;
        this.army = army;
        this.luaFile = luaFile;
        loadUnits();
    }

    protected void loadUnits() throws IOException, URISyntaxException {
        LuaValue lua = LuaLoader.load(BaseTemplate.class.getResourceAsStream(luaFile));
        LuaTable units = lua.get("Units").checktable();
        LuaValue key = LuaValue.NIL;
        while (units.next(key) != LuaValue.NIL) {
            key = units.next(key).checkvalue(1);
            LuaValue unit = units.get(key);
            String type = unit.get("type").checkstring().toString();
            LuaTable posTable = unit.get("Position").checktable();
            Vector2f position = new Vector2f(posTable.get(1).tofloat(),posTable.get(3).tofloat());
            if (structures.containsKey(type)) {
                structures.get(type).add(position);
            } else {
                structures.put(type, new LinkedHashSet<>(Collections.singletonList(position)));
            }
        }
    }

    public void addUnits(SCMap map) {
        structures.forEach((name, positions) -> {
            positions.forEach(position -> {
                Unit unit = new Unit(name, position.add(center), 0);
                switch (army) {
                    case CIVILIAN:
                        map.addCiv(unit);
                        break;
                    case ENEMY:
                        map.addUnit(unit);
                        break;
                    case WRECK:
                        map.addWreck(unit);
                        break;
                }
            });
        });
    }

    public void flip(Symmetry symmetry) {
        structures.values().forEach(positions -> positions.forEach(position -> position.flip(new Vector2f(0, 0), symmetry)));
    }
}
