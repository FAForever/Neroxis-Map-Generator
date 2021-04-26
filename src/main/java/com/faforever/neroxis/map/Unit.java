package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Unit extends Marker {
    private final String type;
    private float rotation;

    public Unit(String id, String type, Vector2 position, float rotation) {
        this(id, type, new Vector3(position), rotation);
    }

    public Unit(String id, String type, Vector3 position, float rotation) {
        super(id, position);
        this.type = type;
        this.rotation = rotation;
    }

}
