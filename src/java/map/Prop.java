package map;

import lombok.Getter;
import util.Vector3f;

@Getter
public strictfp class Prop {
    private final String path;
    private final Vector3f position;
    private final float rotation;

    public Prop(String path, Vector3f position, float rotation) {
        this.path = path;
        this.position = position;
        this.rotation = rotation;
    }

}
