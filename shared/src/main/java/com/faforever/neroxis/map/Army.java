package com.faforever.neroxis.map;

import lombok.Value;

import java.util.Collection;
import java.util.List;

@Value
public class Army {
    String id;
    List<Group> groups;

    public Group getGroup(String id) {
        return groups.stream().filter(group -> group.getId().equals(id)).findFirst().orElse(null);
    }

    public Group getGroup(int i) {
        return groups.get(i);
    }

    public void addGroup(Group group) {
        groups.add(group);
    }

    public int getGroupCount() {
        return groups.size();
    }

    public int getNumUnits() {
        return groups.stream().map(Group::getUnits).mapToInt(Collection::size).sum();
    }
}
