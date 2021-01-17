package neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@EqualsAndHashCode(callSuper = true)
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
