package com.faforever.neroxis.importer;

import com.faforever.neroxis.lua.Lua;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScenarioImporter {
    public static void importScenario(Path folderPath, SCMap map) throws IOException {
        File dir = folderPath.toFile();

        File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith("_scenario.lua"));
        assert mapFiles != null;
        if (mapFiles.length == 0) {
            System.out.println("No scenario file in map folder");
            return;
        }

        Path scenarioPath = mapFiles[0].toPath();
        try (InputStream inputStream = Files.newInputStream(scenarioPath)) {
            Lua.Value.Table luaScenarioInfo = Lua.parse(inputStream)
                                                 .statements()
                                                 .stream()
                                                 .filter(Lua.Statement.Assignment.class::isInstance)
                                                 .map(Lua.Statement.Assignment.class::cast)
                                                 .filter(ScenarioImporter::isScenarioInfoAssignment)
                                                 .map(Lua.Statement.Assignment::values)
                                                 .map(List::getFirst)
                                                 .map(Lua.Value.Table.class::cast)
                                                 .findFirst()
                                                 .orElseThrow();

            if (!(luaScenarioInfo.get("name") instanceof Lua.Value.String(String name))) {
                throw new IllegalArgumentException("ScenarioInfo.name is not a string");
            }
            map.setName(name);

            if (!(luaScenarioInfo.get("description") instanceof Lua.Value.String(String description))) {
                throw new IllegalArgumentException("ScenarioInfo.description is not a string");
            }
            map.setDescription(description);

            if (!(luaScenarioInfo.get("norushradius") instanceof Lua.Value.Number(double noRushRadius))) {
                throw new IllegalArgumentException("ScenarioInfo.norushradius is not a number");
            }
            map.setNoRushRadius((float) noRushRadius);

            map.getSpawns().forEach(spawn -> {
                if (luaScenarioInfo.get("norushoffsetX_" + spawn.getId()) instanceof Lua.Value.Number(double xOffset)
                    && luaScenarioInfo.get("norushoffsetY_" + spawn.getId()) instanceof Lua.Value.Number(
                        double yOffset
                )) {
                    spawn.setNoRushOffset(new Vector2((float) xOffset, (float) yOffset));
                }
            });
        }
    }

    private static boolean isScenarioInfoAssignment(Lua.Statement.Assignment assignment) {
        if (assignment.targets().size() != 1) {
            return false;
        }

        if (!(assignment.targets().getFirst() instanceof Lua.Receiver.Named(
                String name, List<Lua.MemberAccessor> memberAccessors
        )) || !name.equals("ScenarioInfo") || !memberAccessors.isEmpty()) {
            return false;
        }

        List<Lua.Expression> values = assignment.values();
        if (values.size() != 1) {
            return false;
        }

        return values.getFirst() instanceof Lua.Value.Table;
    }
}
