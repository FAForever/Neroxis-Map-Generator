package com.faforever.neroxis.importer;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.LuaLoader;
import com.faforever.neroxis.util.vector.Vector2;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.luaj.vm2.LuaValue;

public strictfp class ScenarioImporter {
    public static void importScenario(Path folderPath, SCMap map) throws IOException {
        File dir = folderPath.toFile();

        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith("_scenario.lua"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No scenario file in map folder");
            return;
        }

        Path scenarioPath = mapFiles[0].toPath();

        LuaValue lua = LuaLoader.loadFile(scenarioPath).get("ScenarioInfo");
        map.setName(lua.get("name").checkjstring());
        map.setDescription(lua.get("description").checkjstring());
        map.setNoRushRadius((float) lua.get("norushradius").checkdouble());
        map.getSpawns().forEach(spawn -> {
            if (lua.get("norushoffsetX_" + spawn.getId()) != LuaValue.NIL
                && lua.get("norushoffsetY_" + spawn.getId()) != LuaValue.NIL) {
                float xOffset = (float) lua.get("norushoffsetX_" + spawn.getId()).checkdouble();
                float yOffset = (float) lua.get("norushoffsetY_" + spawn.getId()).checkdouble();
                spawn.setNoRushOffset(new Vector2(xOffset, yOffset));
            }
        });
    }
}
