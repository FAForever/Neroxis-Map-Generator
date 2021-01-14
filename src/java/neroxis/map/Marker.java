package neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp class Marker {
    private String id;
    private Vector3f position;

    public Marker(String id, Vector2f position) {
        this(id, new Vector3f(position));
    }
}
