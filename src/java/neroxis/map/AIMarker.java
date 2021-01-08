package neroxis.map;

import lombok.Data;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.util.Collection;
import java.util.LinkedHashSet;

@Data
public strictfp class AIMarker {
    private String id;
    private Vector3f position;
    private LinkedHashSet<String> neighbors;

    public AIMarker(String id, Vector2f position, LinkedHashSet<String> neighbors) {
        this(id, new Vector3f(position), neighbors);
    }

    public AIMarker(String id, Vector3f position, LinkedHashSet<String> neighbors) {
        this.id = id;
        this.position = position;
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

