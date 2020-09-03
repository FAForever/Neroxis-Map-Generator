package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

@Data
public strictfp class AIMarker {
    private int id;
    private Vector3f position;
    private int[] neighbors;

    public AIMarker(int id, Vector2f position, int[] neighbors) {
        this(id, new Vector3f(position), neighbors);
    }

    public AIMarker(int id, Vector3f position, int[] neighbors) {
        this.id = id;
        this.position = position;
        this.neighbors = neighbors;
    }

}

