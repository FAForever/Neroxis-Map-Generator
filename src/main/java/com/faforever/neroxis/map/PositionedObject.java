package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public strictfp abstract class PositionedObject {
    protected Vector3 position;

    protected PositionedObject(Vector2 position) {
        this.position = new Vector3(position);
    }
}