package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

import java.util.Collection;
import java.util.LinkedHashSet;

@Data
public strictfp class AIMarker {
    private int id;
    private Vector3f position;
    private LinkedHashSet<Integer> neighbors;

    public AIMarker(int id, Vector2f position, LinkedHashSet<Integer> neighbors) {
        this(id, new Vector3f(position), neighbors);
    }

    public AIMarker(int id, Vector3f position, LinkedHashSet<Integer> neighbors) {
        this.id = id;
        this.position = position;
        this.neighbors = neighbors;
    }

    public int getNeighborCount() {
        return neighbors.size();
    }

    public void addNeighbor(int i) {
        neighbors.add(i);
    }

    public void addNeighbors(Collection<? extends Integer> ids) {
        neighbors.addAll(ids);
    }

}

