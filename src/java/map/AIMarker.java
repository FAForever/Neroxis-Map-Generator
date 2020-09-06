package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

import java.util.List;

@Data
public strictfp class AIMarker {
    private int id;
    private Vector3f position;
    private List<Integer> neighbors;

    public AIMarker(int id, Vector2f position, List<Integer> neighbors) {
        this(id, new Vector3f(position), neighbors);
    }

    public AIMarker(int id, Vector3f position, List<Integer> neighbors) {
        this.id = id;
        this.position = position;
        this.neighbors = neighbors;
    }

    public int getNeighborCount() {
        return neighbors.size();
    }

    public int getNeighbor(int i) {
        return neighbors.get(i);
    }

    public void addNeighbor(int i) {
        neighbors.add(i);
    }

}

