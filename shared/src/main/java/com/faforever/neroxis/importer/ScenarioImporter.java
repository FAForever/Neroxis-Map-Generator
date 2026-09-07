package com.faforever.neroxis.importer;

import com.faforever.neroxis.lua.Lua;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ScenarioImporter {
    public static void importScenario(Path folderPath, SCMap map) throws IOException {
        Path scenarioPath;
        try (Stream<Path> paths = Files.list(folderPath)) {
            scenarioPath = paths.filter(file -> file.getFileName().toString().endsWith("_scenario.lua"))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No scenario file in map folder"));
        }

        Lua.Value.Table luaScenarioInfo;
        try (InputStream inputStream = Files.newInputStream(scenarioPath)) {
            luaScenarioInfo = Lua.parse(inputStream)
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
        }

        if (!(luaScenarioInfo.get("name") instanceof Lua.Value.Str(String name))) {
            throw new IllegalArgumentException("ScenarioInfo.name is not a string");
        }
        map.setName(name);

        if (!(luaScenarioInfo.get("description") instanceof Lua.Value.Str(String description))) {
            throw new IllegalArgumentException("ScenarioInfo.description is not a string");
        }
        map.setDescription(description);

        if (!(luaScenarioInfo.get("norushradius") instanceof Lua.Value.Num(double noRushRadius))) {
            throw new IllegalArgumentException("ScenarioInfo.norushradius is not a number");
        }
        map.setNoRushRadius((float) noRushRadius);

        map.getSpawns().forEach(spawn -> {
            if (luaScenarioInfo.get("norushoffsetX_" + spawn.getId()) instanceof Lua.Value.Num(double xOffset) &&
                luaScenarioInfo.get("norushoffsetY_" + spawn.getId()) instanceof Lua.Value.Num(
                        double yOffset
                )) {
                spawn.setNoRushOffset(new Vector2((float) xOffset, (float) yOffset));
            }
        });

        if (luaScenarioInfo.get("Configurations") instanceof Lua.Value.Table configurations &&
            configurations.get("standard") instanceof Lua.Value.Table standardTable &&
            standardTable.get("teams") instanceof Lua.Value.Table teamsTable) {
            teamsTable.contents()
                      .values()
                      .stream()
                      .filter(Lua.Value.Table.class::isInstance)
                      .map(Lua.Value.Table.class::cast)
                      .filter(table -> table.get("name") instanceof Lua.Value.Str(String nameValue) &&
                                       "FFA".equals(nameValue))
                      .map(table -> table.get("armies"))
                      .filter(Lua.Value.Table.class::isInstance)
                      .map(Lua.Value.Table.class::cast)
                      .findFirst()
                      .ifPresent(armiesTable -> {
                          int numArmies = armiesTable.contents().size();
                          List<String> armyOrder = IntStream.range(0, numArmies)
                                                            .mapToObj(i -> switch (armiesTable.get(i + 1)) {
                                                                case null -> throw new IllegalStateException(
                                                                        "Count out of range");
                                                                case Lua.Value.Str(String value) -> value;
                                                                case Lua.Expression _ ->
                                                                        throw new IllegalStateException(
                                                                                "Army is not a string");
                                                            })
                                                            .toList();
                          map.setArmyOrder(armyOrder);
                      });
        }
    }

    private static boolean isScenarioInfoAssignment(Lua.Statement.Assignment assignment) {
        if (assignment.targets().size() != 1) {
            return false;
        }

        if (!(assignment.targets().getFirst() instanceof Lua.Variable.Named(
                String name, List<? extends Lua.MemberAccessor> memberAccessors
        )) || !name.equals("ScenarioInfo") || !memberAccessors.isEmpty()) {
            return false;
        }

        List<? extends Lua.Expression> values = assignment.values();
        if (values.size() != 1) {
            return false;
        }

        return values.getFirst() instanceof Lua.Value.Table;
    }
}
