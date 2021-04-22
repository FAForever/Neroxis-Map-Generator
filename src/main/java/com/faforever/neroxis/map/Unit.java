package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2f;
import com.faforever.neroxis.util.Vector3f;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Unit extends Marker {
    private final String type;
    private float rotation;

    public Unit(String id, String type, Vector2f position, float rotation) {
        this(id, type, new Vector3f(position), rotation);
    }

    public Unit(String id, String type, Vector3f position, float rotation) {
        super(id, position);
        this.type = type;
        this.rotation = rotation;
    }

}
