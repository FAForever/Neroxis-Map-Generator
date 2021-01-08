package neroxis.map;

import lombok.Data;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@Data
public strictfp class Mex {
    private final String id;
    private Vector3f position;

    public Mex(String id, Vector2f position) {
        this(id, new Vector3f(position));
    }

    public Mex(String id, Vector3f position) {
        this.id = id;
        this.position = position;
    }
}
