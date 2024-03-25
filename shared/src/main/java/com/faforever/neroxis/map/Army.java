package com.faforever.neroxis.map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Army {
    @EqualsAndHashCode.Include
    private final String id;
    private final List<Group> groups = new ArrayList<>();

    public List<Group> getGroups() {
        return List.copyOf(groups);
    }

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
        return groups.stream().mapToInt(Group::getUnitCount).sum();
    }
}
