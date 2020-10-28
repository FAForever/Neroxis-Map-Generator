package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

@Data
public strictfp class Hydro {
    private final String id;
    private Vector3f position;

    public Hydro(String id, Vector2f position) {
        this(id, new Vector3f(position));
    }

    public Hydro(String id, Vector3f position) {
        this.id = id;
        this.position = position;
    }
}
