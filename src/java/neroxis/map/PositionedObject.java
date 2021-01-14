package neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp abstract class PositionedObject {
    protected Vector3f position;

    protected PositionedObject(Vector2f position) {
        this.position = new Vector3f(position);
    }
}