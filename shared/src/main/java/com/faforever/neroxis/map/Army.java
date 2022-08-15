package com.faforever.neroxis.map;

import java.util.List;
import lombok.Data;

@Data
public strictfp class Army {
    private final String id;
    private final List<Group> groups;

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
        return groups.stream().mapToInt(group -> group.getUnits().size()).sum();
    }
}
