package neroxis.map;

import lombok.Data;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@Data
public strictfp class Unit {
    private final String id;
    private final String type;
    private float rotation;
    private Vector3f position;

    public Unit(String id, String type, Vector2f position, float rotation) {
        this(id, type, new Vector3f(position), rotation);
    }

    public Unit(String id, String type, Vector3f position, float rotation) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.rotation = rotation;
    }

}
