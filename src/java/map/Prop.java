package map;

import lombok.Getter;
import lombok.Setter;
import util.Vector2f;
import util.Vector3f;

@Getter
@Setter
public strictfp class Prop {
    private final String path;
    private final float rotation;
    private Vector3f position;

    public Prop(String path, Vector2f position, float rotation) {
        this.path = path;
        this.position = new Vector3f(position.x, 0, position.y);
        this.rotation = rotation;
    }

    public Prop(String path, Vector3f position, float rotation) {
        this.path = path;
        this.position = position;
        this.rotation = rotation;
    }

}
