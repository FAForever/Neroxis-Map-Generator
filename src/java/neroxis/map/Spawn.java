package neroxis.map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@EqualsAndHashCode(callSuper = true)
@Data
public strictfp class Spawn extends Marker {
    private Vector2f noRushOffset;

    public Spawn(String id, Vector2f position, Vector2f noRushOffset) {
        this(id, new Vector3f(position), noRushOffset);
    }

    public Spawn(String id, Vector3f position, Vector2f noRushOffset) {
        super(id, position);
        this.noRushOffset = noRushOffset;
    }
}
