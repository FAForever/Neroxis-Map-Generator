package com.faforever.neroxis.bases;

import com.faforever.neroxis.lua.Lua;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.serial.biome.SCUnitSet;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

public class BaseTemplateLoader {

    private static final Comparator<Map.Entry<String, Vector2>> UNIT_ENTRY_COMPARATOR = Map.Entry.<String, Vector2>comparingByKey()
                                                                                                 .thenComparing(
                                                                                                         Map.Entry::getValue,
                                                                                                         Comparator.comparing(
                                                                                                                           Vector2::getX)
                                                                                                                   .thenComparing(
                                                                                                                           Vector2::getY));

    public static SequencedMap<String, SequencedSet<Vector2>> loadUnits(String file) throws IOException {
        if (file.endsWith(".lua")) {
            return loadUnitsFromLua(file);
        } else if (file.endsWith(".scunits")) {
            return loadUnitsFromSCUnits(file);
        }
        throw new IllegalArgumentException("File format not valid");
    }

    private static SequencedMap<String, SequencedSet<Vector2>> loadUnitsFromLua(String luaFile) throws IOException {
        Lua.Block lua = Lua.parse(BaseTemplate.class.getResourceAsStream(luaFile));
        Lua.Value.Table luaUnits = lua.statements()
                                      .stream()
                                      .filter(Lua.Statement.Assignment.class::isInstance)
                                      .map(Lua.Statement.Assignment.class::cast)
                                      .filter(BaseTemplateLoader::isUnitsAssignment)
                                      .map(Lua.Statement.Assignment::values)
                                      .map(List::getFirst)
                                      .map(Lua.Value.Table.class::cast)
                                      .findFirst()
                                      .orElseThrow();

        return luaUnits.contents()
                       .entrySet()
                       .stream()
                       .map(BaseTemplateLoader::extractUnitPositionEntry)
                       .sorted(UNIT_ENTRY_COMPARATOR)
                       .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                                                      Collectors.mapping(Map.Entry::getValue,
                                                                         Collectors.toCollection(LinkedHashSet::new))));
    }

    private static boolean isUnitsAssignment(Lua.Statement.Assignment assignment) {
        if (assignment.targets().size() != 1) {
            return false;
        }

        if (!(assignment.targets().getFirst() instanceof Lua.Receiver.Named(
                String name, List<Lua.MemberAccessor> memberAccessors
        )) || !name.equals("Units") || !memberAccessors.isEmpty()) {
            return false;
        }

        List<Lua.Expression> values = assignment.values();
        if (values.size() != 1) {
            return false;
        }

        return values.getFirst() instanceof Lua.Value.Table;
    }

    private static Map.Entry<String, Vector2> extractUnitPositionEntry(
            Map.Entry<Lua.Expression, Lua.Expression> entry) {
        if (!(entry.getKey() instanceof Lua.Value.String(
                String keyValue
        ))) {
            throw new IllegalArgumentException("Key must be a string, got: %s".formatted(entry.getKey()));
        }

        if (!(entry.getValue() instanceof Lua.Value.Table table)) {
            throw new IllegalArgumentException(
                    "Value must be a table for unit %s, got: %s".formatted(keyValue, entry.getValue()));
        }

        if (!(table.get("type") instanceof Lua.Value.String(
                String type
        ))) {
            throw new IllegalArgumentException(
                    "type must be a String for unit %s, got: %s".formatted(keyValue, table.get("type")));
        }

        Vector2 position = extractPosition(keyValue, table);
        return Map.entry(type, position);
    }

    private static Vector2 extractPosition(String keyValue, Lua.Value.Table table) {
        if (!(table.get("Position") instanceof Lua.Value.Table positionTable)) {
            throw new IllegalArgumentException(
                    "Position must be a table for unit %s, got: %s".formatted(keyValue, table.get("Position")));
        }

        double x = extractNumber(positionTable.get(1));
        double y = extractNumber(positionTable.get(3));

        return new Vector2((float) x, (float) y);
    }

    private static double extractNumber(Lua.Expression expression) {
        return switch (expression) {
            case Lua.Value.Number(double value) -> value;
            case Lua.UnaryOperator.Negate(Lua.Value.Number(double value)) -> -value;
            default -> throw new IllegalArgumentException(
                    "Expression must be a number got %s".formatted(expression)
            );
        };
    }

    private static SequencedMap<String, SequencedSet<Vector2>> loadUnitsFromSCUnits(String scUnitsFile) throws
            IOException {
        SCUnitSet scUnitSet = FileUtil.deserialize(BaseTemplate.class.getResourceAsStream(scUnitsFile),
                                                   SCUnitSet.class);
        scUnitSet.units()
                 .forEach(unit -> unit.pos().subtract(scUnitSet.center()).multiply(10f).round(2));

        return scUnitSet.units()
                        .stream()
                        .map(unit -> Map.entry(unit.ID(), new Vector2(unit.pos())))
                        .sorted(UNIT_ENTRY_COMPARATOR)
                        .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                                                       Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(
                                                               LinkedHashSet::new))));
    }
}
