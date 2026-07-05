package com.faforever.neroxis.bases;

import com.faforever.neroxis.lua.Lua;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.ResourceUtil;
import com.faforever.neroxis.util.serial.biome.SCUnitSet;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

public class BaseTemplateLoader {

    public static final Comparator<Vector2> VECTOR_COMPARATOR = Comparator.comparing(Vector2::x)
                                                                          .thenComparing(Vector2::y);
    private static final Comparator<Map.Entry<String, Vector2>> UNIT_ENTRY_COMPARATOR = Map.Entry.<String, Vector2>comparingByKey()
                                                                                                 .thenComparing(
                                                                                                         Map.Entry::getValue,
                                                                                                         VECTOR_COMPARATOR);

    public static SequencedMap<String, SequencedSet<Vector2>> loadUnits(String path) throws IOException {
        try (InputStream inputStream = Objects.requireNonNull(ResourceUtil.getResourceAsStream(path))) {
            if (path.endsWith(".lua")) {
                return loadUnitsFromLua(inputStream);
            } else if (path.endsWith(".scunits")) {
                return loadUnitsFromSCUnits(inputStream);
            }
            throw new IllegalArgumentException("File format not valid");
        }
    }

    public static SequencedMap<String, SequencedSet<Vector2>> loadUnits(InputStream inputStream,
                                                                        TemplateType type) throws IOException {
        return switch (type) {
            case LUA -> loadUnitsFromLua(inputStream);
            case SCUNITS -> loadUnitsFromSCUnits(inputStream);
        };
    }

    private static SequencedMap<String, SequencedSet<Vector2>> loadUnitsFromLua(InputStream inputStream) throws
            IOException {
        Lua.Value.Table luaUnits = Lua.parse(inputStream)
                                      .statements()
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

        if (!(assignment.targets().getFirst() instanceof Lua.Variable.Named(
                String name, List<? extends Lua.MemberAccessor> memberAccessors
        )) || !name.equals("Units") || !memberAccessors.isEmpty()) {
            return false;
        }

        List<? extends Lua.Expression> values = assignment.values();
        if (values.size() != 1) {
            return false;
        }

        return values.getFirst() instanceof Lua.Value.Table;
    }

    private static Map.Entry<String, Vector2> extractUnitPositionEntry(
            Map.Entry<? extends Lua.Expression, ? extends Lua.Expression> entry) {
        if (!(entry.getKey() instanceof Lua.Value.Str(
                String keyValue
        ))) {
            throw new IllegalArgumentException("Key must be a string, got: %s".formatted(entry.getKey()));
        }

        if (!(entry.getValue() instanceof Lua.Value.Table table)) {
            throw new IllegalArgumentException(
                    "Value must be a table for unit %s, got: %s".formatted(keyValue, entry.getValue()));
        }

        if (!(table.get("type") instanceof Lua.Value.Str(
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

    private static double extractNumber(Lua.@Nullable Expression expression) {
        return switch (expression) {
            case Lua.Value.Num(double value) -> value;
            case Lua.UnaryOperator.Negate(Lua.Value.Num(double value)) -> -value;
            case null, default ->
                    throw new IllegalArgumentException("Expression must be a number got %s".formatted(expression));
        };
    }

    private static SequencedMap<String, SequencedSet<Vector2>> loadUnitsFromSCUnits(InputStream inputStream) throws
            IOException {
        SCUnitSet scUnitSet = FileUtil.deserialize(inputStream, SCUnitSet.class);
        Vector3 center = scUnitSet.center();
        return scUnitSet.units()
                        .stream()
                        .map(unit -> Map.entry(unit.ID(),
                                               new Vector2(unit.pos().subtract(center).multiply(10f).round(2))))
                        .sorted(UNIT_ENTRY_COMPARATOR)
                        .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                                                       Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(
                                                               LinkedHashSet::new))));
    }

    public enum TemplateType {
        SCUNITS, LUA
    }
}
