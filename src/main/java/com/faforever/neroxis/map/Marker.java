package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2f;
import com.faforever.neroxis.util.Vector3f;
import lombok.*;

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
