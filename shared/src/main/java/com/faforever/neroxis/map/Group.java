package com.faforever.neroxis.map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Group {
    @EqualsAndHashCode.Include
    private final String id;
    private final List<Unit> units = new ArrayList<>();

    public Unit getUnit(String id) {
        return units.stream().filter(unit -> unit.getId().equals(id)).findFirst().orElse(null);
    }

    public Unit getUnit(int i) {
        return units.get(i);
    }

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public int getUnitCount() {
        return units.size();
    }
}
