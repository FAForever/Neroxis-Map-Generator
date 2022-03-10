package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedHashSet;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class AIMarker extends Marker {
    private LinkedHashSet<String> neighbors;

    public AIMarker(String id, Vector2 position, LinkedHashSet<String> neighbors) {
        this(id, new Vector3(position), neighbors);
    }

    public AIMarker(String id, Vector3 position, LinkedHashSet<String> neighbors) {
        super(id, position);
        this.neighbors = neighbors;
    }

    public int getNeighborCount() {
        return neighbors.size();
    }

    public void addNeighbor(String id) {
        neighbors.add(id);
    }

    public void addNeighbors(Collection<? extends String> ids) {
        neighbors.addAll(ids);
    }

}

