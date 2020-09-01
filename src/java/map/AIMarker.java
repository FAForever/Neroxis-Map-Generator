package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;

@Data
public strictfp class AIMarker {
    private Vector3f position;
    private ArrayList<Integer> neighbors;

    public AIMarker(Vector2f position, ArrayList<Integer> neighbors) {
        this(new Vector3f(position), neighbors);
    }

    public AIMarker(Vector3f position, ArrayList<Integer> neighbors) {
        this.position = position;
        this.neighbors = neighbors;
    }

}

