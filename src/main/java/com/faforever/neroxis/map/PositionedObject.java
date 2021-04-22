package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2f;
import com.faforever.neroxis.util.Vector3f;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp abstract class PositionedObject {
    protected Vector3f position;

    protected PositionedObject(Vector2f position) {
        this.position = new Vector3f(position);
    }
}