package com.faforever.neroxis.importer;

import com.faforever.neroxis.lua.Lua;
import com.faforever.neroxis.map.AIMarker;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SaveImporter {
    public static void importSave(Path folderPath, SCMap map) throws IOException {
        Path savePath;
        try (Stream<Path> paths = Files.list(folderPath)) {
            savePath = paths.filter(file -> file.getFileName().toString().endsWith("_save.lua"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No save file in map folder"));
        }

        Lua.Value.Table luaScenario;
        try (InputStream inputStream = Files.newInputStream(savePath)) {
            luaScenario = Lua.parse(inputStream)
                             .statements()
                             .stream()
                             .filter(Lua.Statement.Assignment.class::isInstance)
                             .map(Lua.Statement.Assignment.class::cast)
                             .filter(SaveImporter::isScenarioAssignment)
                             .map(Lua.Statement.Assignment::values)
                             .map(List::getFirst)
                             .map(Lua.Value.Table.class::cast)
                             .findFirst()
                             .orElseThrow();
        }


        importPlayableArea(map, luaScenario);
        importMarkers(map, luaScenario);
        importArmies(map, luaScenario);
    }

    private static void importPlayableArea(SCMap map, Lua.Value.Table luaScenario) {
        Lua.Expression areasExpression = luaScenario.get("Areas");
        if (areasExpression == null) {
            return;
        }

        //@formatter:off

        if (!(areasExpression instanceof Lua.Value.Table areasTable)
            || !(areasTable.get("AREA_1") instanceof Lua.Value.Table area1Table)
            || !(extractTableFromExpression(area1Table.get("rectangle")) instanceof Lua.Value.Table rectangleTable)) {
            throw new IllegalStateException("Invalid Areas expression %s".formatted(areasExpression));
        }

        if (!(rectangleTable.get(1) instanceof Lua.Value.Number(double x0))
            || !(rectangleTable.get(2) instanceof Lua.Value.Number(double y0))
            || !(rectangleTable.get(3) instanceof Lua.Value.Number(double x1))
            || !(rectangleTable.get(4) instanceof Lua.Value.Number(double y1))) {
            throw new IllegalStateException("Invalid rectangle value %s".formatted(rectangleTable));
        }

        //@formatter:on

        map.setPlayableArea(new Vector4((float) x0, (float) y0, (float) x1, (float) y1));
    }

    private static void importMarkers(SCMap map, Lua.Value.Table luaScenario) {
        Lua.Expression masterChainExpression = luaScenario.get("MasterChain");
        if (masterChainExpression == null) {
            return;
        }

        //@formatter:off

        if (!(masterChainExpression instanceof Lua.Value.Table masterChainTable)
            || !(masterChainTable.get("_MASTERCHAIN_") instanceof Lua.Value.Table masterChainSubTable)
            || !(masterChainSubTable.get("Markers") instanceof Lua.Value.Table(
                Map<? extends Lua.Expression, ? extends Lua.Expression> markerContents
        ))
        ) {
            throw new IllegalStateException("Invalid master chain expression %s".formatted(masterChainExpression));
        }

        //@formatter:on

        markerContents.forEach((key, value) -> {
            if (!(key instanceof Lua.Value.String(String id))) {
                throw new IllegalStateException("Invalid marker id %s".formatted(key));
            }

            if (!(value instanceof Lua.Value.Table markerTable)) {
                throw new IllegalStateException("Invalid marker value %s".formatted(value));
            }

            addMarker(markerTable, id, map);
        });
    }

    private static void addMarker(Lua.Value.Table markerTable, String id, SCMap map) {
        String type = extractStringFromExpression(markerTable.get("type"));

        //@formatter:off

        if (!(extractTableFromExpression(markerTable.get("position")) instanceof Lua.Value.Table positionTable)
            || !(positionTable.get(1) instanceof Lua.Value.Number(double x))
            || !(positionTable.get(2) instanceof Lua.Value.Number(double y))
            || !(positionTable.get(3) instanceof Lua.Value.Number(double z))) {
            throw new IllegalArgumentException("Invalid position table %s".formatted(markerTable.get("position")));
        }

        //@formatter:on

        Vector3 location = new Vector3((float) x, (float) y, (float) z);

        switch (type) {
            case "Mass" -> map.addMex(new Marker(id, location));
            case "Hydrocarbon" -> map.addHydro(new Marker(id, location));
            case "Blank Marker" -> {
                if (id.contains("ARMY")) {
                    map.addSpawn(new Spawn(id, location, new Vector2(0, 0), 0));
                } else {
                    map.addBlank(new Marker(id, location));
                }
            }
            case "Air Path Node" -> {
                String adjacentTo = extractStringFromExpression(markerTable.get("adjacentTo"));
                SequencedSet<String> neighbors = Arrays.stream(adjacentTo.split(" "))
                                                       .filter(s -> !s.isEmpty())
                                                       .collect(Collectors.toCollection(LinkedHashSet::new));
                map.addAirMarker(new AIMarker(id, location, neighbors));
            }
            case "Amphibious Path Node" -> {
                String adjacentTo = extractStringFromExpression(markerTable.get("adjacentTo"));
                SequencedSet<String> neighbors = Arrays.stream(adjacentTo.split(" "))
                                                       .filter(s -> !s.isEmpty())
                                                       .collect(Collectors.toCollection(LinkedHashSet::new));
                map.addAmphibiousMarker(new AIMarker(id, location, neighbors));
            }
            case "Water Path Node" -> {
                String adjacentTo = extractStringFromExpression(markerTable.get("adjacentTo"));
                SequencedSet<String> neighbors = Arrays.stream(adjacentTo.split(" "))
                                                       .filter(s -> !s.isEmpty())
                                                       .collect(Collectors.toCollection(LinkedHashSet::new));
                map.addNavyMarker(new AIMarker(id, location, neighbors));
            }
            case "Land Path Node" -> {
                String adjacentTo = extractStringFromExpression(markerTable.get("adjacentTo"));
                SequencedSet<String> neighbors = Arrays.stream(adjacentTo.split(" "))
                                                       .filter(s -> !s.isEmpty())
                                                       .collect(Collectors.toCollection(LinkedHashSet::new));
                map.addLandMarker(new AIMarker(id, location, neighbors));
            }
            case "Rally Point" -> map.addRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            case "Expansion Area" -> map.addExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            case "Large Expansion Area" ->
                    map.addLargeExpansionMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            case "Naval Area" -> map.addNavalAreaMarker(new AIMarker(id, location, new LinkedHashSet<>()));
            case "Naval Rally Point" -> map.addNavyRallyMarker(new AIMarker(id, location, new LinkedHashSet<>()));
        }
    }

    private static void importArmies(SCMap map, Lua.Value.Table luaScenario) {
        Lua.Expression armiesExpression = luaScenario.get("Armies");
        if (armiesExpression == null) {
            return;
        }

        if (!(armiesExpression instanceof Lua.Value.Table(
                Map<? extends Lua.Expression, ? extends Lua.Expression> armiesContents
        ))) {
            throw new IllegalStateException("Invalid armies expression %s".formatted(armiesExpression));
        }

        armiesContents.forEach((key, value) -> {
            if (!(key instanceof Lua.Value.String(String id))) {
                throw new IllegalStateException("Invalid army id %s".formatted(key));
            }

            if (!(value instanceof Lua.Value.Table armyTable)) {
                throw new IllegalStateException("Invalid army value %s".formatted(value));
            }

            addArmy(armyTable, id, map);
        });
    }

    private static void addArmy(Lua.Value.Table armyTable, String armyName, SCMap map) {
        Army army = new Army(armyName);
        map.addArmy(army);
        Lua.Expression unitsExpression = armyTable.get("Units");
        if (unitsExpression == null) {
            return;
        }

        //@formatter:off

        if (!(extractTableFromExpression(unitsExpression) instanceof Lua.Value.Table unitsTable)) {
            throw new IllegalStateException("Invalid Units value %s".formatted(unitsExpression));
        }

        //@formatter:on

        Lua.Expression groupsExpression = unitsTable.get("Units");
        if (groupsExpression == null) {
            return;
        }

        if (!(extractTableFromExpression(groupsExpression) instanceof Lua.Value.Table(
                Map<? extends Lua.Expression, ? extends Lua.Expression> groupsContents
        ))) {
            throw new IllegalStateException("Invalid Units Units value %s".formatted(unitsExpression));
        }

        groupsContents.forEach((key, value) -> {
            if (!(key instanceof Lua.Value.String(String id))) {
                throw new IllegalStateException("Invalid group id %s".formatted(key));
            }

            if (!(extractTableFromExpression(value) instanceof Lua.Value.Table groupTable)) {
                throw new IllegalStateException("Invalid group value %s".formatted(value));
            }

            addGroup(groupTable, id, army);
        });
    }

    private static void addGroup(Lua.Value.Table groupTable, String groupName, Army army) {
        Group group = new Group(groupName);
        army.addGroup(group);
        Lua.Expression unitsExpression = groupTable.get("Units");
        if (unitsExpression == null) {
            return;
        }

        if (!(extractTableFromExpression(unitsExpression) instanceof Lua.Value.Table(
                Map<? extends Lua.Expression, ? extends Lua.Expression> unitsContents
        ))) {
            throw new IllegalStateException("Invalid Units value %s".formatted(unitsExpression));
        }

        unitsContents.forEach((key, value) -> {
            if (!(key instanceof Lua.Value.String(String id))) {
                throw new IllegalStateException("Invalid unit id %s".formatted(key));
            }

            if (!(value instanceof Lua.Value.Table unitTable)) {
                throw new IllegalStateException("Invalid unit value %s".formatted(value));
            }

            if (!(unitTable.get("type") instanceof Lua.Value.String(String type))) {
                throw new IllegalStateException("Invalid unit type %s".formatted(unitTable.get("type")));
            }

            //@formatter:off

            if (!(extractTableFromExpression(unitTable.get("Position")) instanceof Lua.Value.Table positionTable)
                || !(positionTable.get(1) instanceof Lua.Value.Number(double x))
                || !(positionTable.get(2) instanceof Lua.Value.Number(double y))
                || !(positionTable.get(3) instanceof Lua.Value.Number(double z))) {
                throw new IllegalArgumentException("Invalid position table %s".formatted(unitTable.get("position")));
            }

            if (!(extractTableFromExpression(unitTable.get("Orientation")) instanceof Lua.Value.Table orientationTable)
                || !(orientationTable.get(2) instanceof Lua.Value.Number(double rotation))) {
                throw new IllegalArgumentException("Invalid orientation table %s".formatted(unitTable.get("position")));
            }

            //@formatter:on

            Vector3 location = new Vector3((float) x, (float) y, (float) z);
            group.addUnit(new Unit(id, type, location, (float) rotation));
        });
    }

    private static Lua.Value.Table extractTableFromExpression(Lua.Expression expression) {
        return switch (expression) {
            case Lua.FunctionCall.Direct(
                    Lua.Variable.Named(String name, List<? extends Lua.MemberAccessor> memberAccessors),
                    List<? extends Lua.Expression> arguments
            ) when memberAccessors.isEmpty() && name.equals("RECTANGLE") && arguments.size() == 4 ->
                    convertArgumentsToIndexedTable(arguments);
            case Lua.FunctionCall.Direct(
                    Lua.Variable.Named(String name, List<? extends Lua.MemberAccessor> memberAccessors),
                    List<? extends Lua.Expression> arguments
            ) when memberAccessors.isEmpty() && name.equals("VECTOR3") && arguments.size() == 3 ->
                    convertArgumentsToIndexedTable(arguments);
            case Lua.FunctionCall.Direct(
                    Lua.Variable.Named(String name, List<? extends Lua.MemberAccessor> memberAccessors),
                    List<? extends Lua.Expression> arguments
            ) when memberAccessors.isEmpty()
                   && name.equals("GROUP")
                   && arguments.size() == 1
                   && arguments.getFirst() instanceof Lua.Value.Table table -> table;
            case Lua.Value.Table table -> table;
            default -> throw new IllegalArgumentException(
                    "Could not extract table from expression %s".formatted(expression));
        };
    }

    private static String extractStringFromExpression(Lua.Expression expression) {
        return switch (expression) {
            case Lua.FunctionCall.Direct(
                    Lua.Variable.Named(String name, List<? extends Lua.MemberAccessor> memberAccessors),
                    List<? extends Lua.Expression> arguments
            ) when memberAccessors.isEmpty()
                   && name.equals("STRING")
                   && arguments.size() == 1
                   && arguments.getFirst() instanceof Lua.Value.String(String value) -> value;
            case Lua.Value.String(String value) -> value;
            default -> throw new IllegalArgumentException(
                    "Could not extract string from expression %s".formatted(expression));
        };
    }

    private static Lua.Value.Table convertArgumentsToIndexedTable(List<? extends Lua.Expression> arguments) {
        Map<Lua.Value.Number, Lua.Expression> contents = new HashMap<>();
        int index = 1;
        for (Lua.Expression argument : arguments) {
            contents.put(new Lua.Value.Number(index++), argument);
        }

        return new Lua.Value.Table(contents);
    }

    private static boolean isScenarioAssignment(Lua.Statement.Assignment assignment) {
        if (assignment.targets().size() != 1) {
            return false;
        }

        if (!(assignment.targets().getFirst() instanceof Lua.Variable.Named(
                String name, List<? extends Lua.MemberAccessor> memberAccessors
        )) || !name.equals("Scenario") || !memberAccessors.isEmpty()) {
            return false;
        }

        List<? extends Lua.Expression> values = assignment.values();
        if (values.size() != 1) {
            return false;
        }

        return values.getFirst() instanceof Lua.Value.Table;
    }
}
