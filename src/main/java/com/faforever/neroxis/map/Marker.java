package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp class Marker extends PositionedObject {
    private String id;

    public Marker(String id, Vector3 position) {
        super(position);
        this.id = id;
    }

    public Marker(String id, Vector2 position) {
        super(position);
        this.id = id;
    }
}
