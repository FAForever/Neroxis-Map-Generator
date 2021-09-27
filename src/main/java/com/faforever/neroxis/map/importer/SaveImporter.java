package com.faforever.neroxis.map.importer;

import com.faforever.neroxis.map.AIMarker;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.LuaLoader;
import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

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
        try {
            LuaTable areaTable = lua.get("Areas").get("AREA_1").get("rectangle").checktable();
            map.setPlayableArea(new Vector4(areaTable.get(1).tofloat(), areaTable.get(2).tofloat(), areaTable.get(3).tofloat(), areaTable.get(4).tofloat()));
        } catch (Exception ignored) {
        }

        LuaTable markers = lua.get("MasterChain").get("_MASTERCHAIN_").get("Markers").checktable();
        LuaValue key = LuaValue.NIL;
        while (markers.next(key) != LuaValue.NIL) {
            key = markers.next(key).checkvalue(1);
            String stringID = key.checkjstring();
            LuaTable marker = markers.get(key).checktable();
            addMarker(marker, stringID, map);
        }
        LuaTable armies = lua.get("Armies").checktable();
        key = LuaValue.NIL;
        while (armies.next(key) != LuaValue.NIL) {
            key = armies.next(key).checkvalue(1);
            String id = key.checkjstring();
            LuaTable armyTable = armies.get(key).checktable();
            addArmy(armyTable, id, map);
        }
    }

    public static void addMarker(LuaTable marker, String id, SCMap map) {
        LuaString type = marker.get("type").checkstring();
        LuaTable locTable;
        Vector3 location;
        List<String> neighbors;
        switch (type.toString()) {
            case "Mass":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addMex(new Marker(id, location));
                break;
            case "Hydrocarbon":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addHydro(new Marker(id, location));
                break;
            case "Blank Marker":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                if (id.contains("ARMY")) {
                    map.addSpawn(new Spawn(id, location, new Vector2(0, 0), 0));
                } else {
                    map.addBlank(new Marker(id, location));
                }
                break;
            case "Air Path Node":
                locTable = marker.get("position").checktable();
                neighbors = Arrays.asList(marker.get("adjacentTo").checkjstring().split(" "));
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addAirMarker(new AIMarker(id, location, new LinkedHashSet<>(neighbors.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()))));
                break;
            case "Amphibious Path Node":
                locTable = marker.get("position").checktable();
                neighbors = Arrays.asList(marker.get("adjacentTo").checkjstring().split(" "));
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addAmphibiousMarker(new AIMarker(id, location, new LinkedHashSet<>(neighbors.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()))));
                break;
            case "Water Path Node":
                locTable = marker.get("position").checktable();
                neighbors = Arrays.asList(marker.get("adjacentTo").checkjstring().split(" "));
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavyMarker(new AIMarker(id, location, new LinkedHashSet<>(neighbors.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()))));
                break;
            case "Naval Area":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavalAreaMarker(new AIMarker(id, location, new LinkedHashSet<>()));
                break;
            case "Expansion Area":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
                break;
            case "Large Expansion Area":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addLargeExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
                break;
            case "Land Path Node":
                locTable = marker.get("position").checktable();
                neighbors = Arrays.asList(marker.get("adjacentTo").checkjstring().split(" "));
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addLandMarker(new AIMarker(id, location, new LinkedHashSet<>(neighbors.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()))));
                break;
            case "Rally Point":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
                break;
            case "Naval Rally Point":
                locTable = marker.get("position").checktable();
                location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                map.addNavyRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
                break;
        }
    }

    public static void addArmy(LuaTable armyTable, String name, SCMap map) {
        Army army = new Army(name, new ArrayList<>());
        map.addArmy(army);
        LuaTable unitGroups = armyTable.get("Units").checktable().get("Units").checktable();
        LuaValue key = LuaValue.NIL;
        while (unitGroups.next(key) != LuaValue.NIL) {
            key = unitGroups.next(key).checkvalue(1);
            String id = key.checkjstring();
            LuaTable groupTable = unitGroups.get(key).checktable();
            addGroup(groupTable, id, army);
        }
    }

    public static void addGroup(LuaTable groupTable, String name, Army army) {
        Group group = new Group(name, new ArrayList<>());
        army.addGroup(group);
        LuaTable unitsTable = groupTable.get("Units").checktable();
        LuaValue key = LuaValue.NIL;
        while (unitsTable.next(key) != LuaValue.NIL) {
            key = unitsTable.next(key).checkvalue(1);
            String id = key.checkjstring();
            LuaTable unitTable = unitsTable.get(key).checktable();
            try {
                String type = unitTable.get("type").checkjstring();
                float rotation = unitTable.get("Orientation").checktable().get(2).tofloat();
                LuaTable locTable = unitTable.get("Position").checktable();
                Vector3 location = new Vector3(locTable.get(1).tofloat(), locTable.get(2).tofloat(), locTable.get(3).tofloat());
                group.addUnit(new Unit(id, type, location, rotation));
            } catch (LuaError e) {
                System.out.printf("Could not read unit %s%n", id);
            }
        }
    }
}
