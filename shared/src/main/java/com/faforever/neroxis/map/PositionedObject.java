package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public sealed abstract class PositionedObject permits Decal, Marker, Prop, WaveGenerator {
    protected Vector3 position;

    protected PositionedObject(Vector2 position) {
        this.position = new Vector3(position);
    }
}