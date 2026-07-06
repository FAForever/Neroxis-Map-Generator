package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public sealed class Marker extends PositionedObject permits AIMarker, Spawn, Unit {
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
