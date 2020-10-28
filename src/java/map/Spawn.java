package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

@Data
public strictfp class Spawn {
    private String id;
    private Vector3f position;
    private Vector2f noRushOffset;

    public Spawn(String id, Vector2f position, Vector2f noRushOffset) {
        this(id, new Vector3f(position), noRushOffset);
    }

    public Spawn(String id, Vector3f position, Vector2f noRushOffset) {
        this.id = id;
        this.position = position;
        this.noRushOffset = noRushOffset;
    }
}
