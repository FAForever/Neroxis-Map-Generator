package neroxis.map;

import lombok.Data;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@Data
public strictfp class Prop {
    private final String path;
    private final float rotation;
    private Vector3f position;

    public Prop(String path, Vector2f position, float rotation) {
        this(path, new Vector3f(position), rotation);
    }

    public Prop(String path, Vector3f position, float rotation) {
        this.path = path;
        this.position = position;
        this.rotation = rotation;
    }

}
