package neroxis.map;

import lombok.*;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp class Marker extends PositionedObject {
    private String id;

    public Marker(String id, Vector3f position) {
        super(position);
        this.id = id;
    }

    public Marker(String id, Vector2f position) {
        super(position);
        this.id = id;
    }
}
