package neroxis.map;

import lombok.Data;
import neroxis.util.Vector3f;

@Data
public strictfp abstract class PositionedObject {
    protected Vector3f position;

    protected PositionedObject() {}

    protected PositionedObject(Vector3f position) {
        this.position = position;
    }

    protected Vector3f getPosition(PositionedObject positionedObject) {
        return this.position;
    }

    public PositionedObject setPosition(Vector3f position) {
        this.position = position;
        return this;
    }
}