package importer;

import com.faforever.commons.lua.LuaLoader;
import map.SCMap;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import util.Vector3f;

import java.io.IOException;
import java.nio.file.Path;

public strictfp class SaveImporter {

    public static void importSave(Path mapPath, String mapName, SCMap map) throws IOException {
        LuaValue lua = LuaLoader.loadFile(mapPath.resolve(mapName + "_save.lua")).get("Scenario");
        LuaTable markers = lua.get("MasterChain").get("_MASTERCHAIN_").get("Markers").checktable();
        LuaValue key = LuaValue.NIL;
        while (markers.next(key) != LuaValue.NIL) {
            key = markers.next(key).checkvalue(1);
            LuaTable marker = markers.get(key).checktable();
            addMarker(marker, map);
        }
        LuaTable armies = lua.get("Armies").checktable();
    }

    public static void addMarker(LuaTable marker, SCMap map) {
        LuaString type = marker.get("type").checkstring();
        LuaTable locTable;
        Vector3f location;
        switch (type.toString()) {
            case "Mass":
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addMex(location);
                break;
            case "Hydrocarbon":
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addHydro(location);
                break;
            case "Blank Marker":
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addSpawn(location);
                break;
        }
    }
}
