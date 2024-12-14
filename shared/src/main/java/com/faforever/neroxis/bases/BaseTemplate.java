package com.faforever.neroxis.bases;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.LinkedHashSet;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

public record BaseTemplate(Vector2 center, SequencedMap<String, SequencedSet<Vector2>> units) {

    public void addUnits(Army army, Group group) {
        units().forEach((name, positions) -> positions.forEach(position -> group.addUnit(
                new Unit(String.format("%s %s Unit %d", army.getId(), group.getId(), group.getUnitCount()), name,
                         position.add(center), 0))));
    }

    public void flip(Symmetry symmetry) {
        units.entrySet()
             .forEach(entry -> entry.setValue(entry.getValue()
                                                   .stream()
                                                   .map(position -> position.flip(new Vector2(0, 0), symmetry))
                                                   .collect(Collectors.toCollection(LinkedHashSet::new))));
    }
}
