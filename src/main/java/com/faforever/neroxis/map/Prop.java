package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2f;
import com.faforever.neroxis.util.Vector3f;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Prop extends PositionedObject{
    private final String path;
    private final float rotation;

    public Prop(String path, Vector2f position, float rotation) {
        this(path, new Vector3f(position), rotation);
    }

    public Prop(String path, Vector3f position, float rotation) {
        super(position);
        this.path = path;
        this.rotation = rotation;
    }
}
