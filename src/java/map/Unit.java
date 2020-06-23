package map;

import lombok.Getter;
import util.Vector3f;

@Getter
public strictfp class Unit {
    private final String type;
    private final Vector3f position;
    private final float rotation;

    public Unit(String type, Vector3f position, float rotation) {
        this.type = type;
        this.position = position;
        this.rotation = rotation;
    }

}
