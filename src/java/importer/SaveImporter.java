package importer;

import com.faforever.commons.lua.LuaLoader;
import map.AIMarker;
import map.SCMap;
import map.Unit;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import util.Vector3f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;

public strictfp class SaveImporter {

    public static void importSave(Path folderPath, SCMap map) throws IOException {
        File dir = folderPath.toFile();

        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith("_save.lua"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No save file in map folder");
            return;
        }

        Path savePath = mapFiles[0].toPath();

        LuaValue lua = LuaLoader.loadFile(savePath).get("Scenario");
        LuaTable markers = lua.get("MasterChain").get("_MASTERCHAIN_").get("Markers").checktable();
        LuaValue key = LuaValue.NIL;
        while (markers.next(key) != LuaValue.NIL) {
            key = markers.next(key).checkvalue(1);
            LuaTable marker = markers.get(key).checktable();
            addMarker(marker, key, map);
        }
        LuaTable armies = lua.get("Armies").checktable();
        key = LuaValue.NIL;
        while (armies.next(key) != LuaValue.NIL) {
            key = armies.next(key).checkvalue(1);
            LuaTable army = armies.get(key).checktable();
            addUnits(army, key, map);
        }
    }

    public static void addMarker(LuaTable marker, LuaValue key, SCMap map) {
        LuaString type = marker.get("type").checkstring();
        LuaTable locTable;
        Vector3f location;
        switch (type.toString()) {
            case "Mass" -> {
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addMex(location);
            }
            case "Hydrocarbon" -> {
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addHydro(location);
            }
            case "Blank Marker" -> {
                locTable = marker.get("position").checktable();
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addSpawn(location);
            }
            case "Air Path Node" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addAirMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Amphibious Path Node" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addAmphibiousMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Water Path Node" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Naval Area" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavalAreaMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Expansion Area" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Large Expansion Area" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addLargeExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Land Path Node" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addLandMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Rally Point" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
            case "Naval Rally Point" -> {
                locTable = marker.get("position").checktable();
                String stringID = key.checkjstring();
                int id = Integer.parseInt(stringID.substring(stringID.length() - 2));
                location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavyRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            }
        }
    }

    public static void addUnits(LuaTable army, LuaValue name, SCMap map) {
        LuaTable units = army.get("Units").checktable().get("Units").checktable();
        LuaValue wreckage = units.get("WRECKAGE");
        if (wreckage != LuaValue.NIL) {
            LuaTable wreckageTable = wreckage.checktable().get("Units").checktable();
            LuaValue key = LuaValue.NIL;
            while (wreckageTable.next(key) != LuaValue.NIL) {
                key = wreckageTable.next(key).checkvalue(1);
                LuaTable unit = wreckageTable.get(key).checktable();
                String type = unit.get("type").checkjstring();
                float rotation = unit.get("Orientation").checktable().get(2).tofloat();
                LuaTable locTable = unit.get("Position").checktable();
                Vector3f location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addWreck(new Unit(type, location, rotation));
            }
        }
        LuaValue initial = units.get("INITIAL");
        if (initial != LuaValue.NIL) {
            LuaTable initialTable = initial.checktable().get("Units").checktable();
            LuaValue key = LuaValue.NIL;
            while (initialTable.next(key) != LuaValue.NIL) {
                key = initialTable.next(key).checkvalue(1);
                LuaTable unit = initialTable.get(key).checktable();
                String type = unit.get("type").checkjstring();
                float rotation = unit.get("Orientation").checktable().get(2).tofloat();
                LuaTable locTable = unit.get("Position").checktable();
                Vector3f location = new Vector3f(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                if (name.toString().contains("ARMY")) {
                    map.addUnit(new Unit(type, location, rotation));
                } else {
                    map.addCiv(new Unit(type, location, rotation));
                }
            }
        }

    }
}
