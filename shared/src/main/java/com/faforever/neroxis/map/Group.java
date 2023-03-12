package com.faforever.neroxis.map;

import lombok.Value;

import java.util.List;

@Value
public class Group {
    String id;
    List<Unit> units;

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
