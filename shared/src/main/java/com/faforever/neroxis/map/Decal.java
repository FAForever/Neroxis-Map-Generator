package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public final class Decal extends PositionedObject {
    private String path;
    private Vector3 rotation;
    private Vector3 scale;
    private DecalType type;
    private float cutOffLOD;

    public Decal(String path, Vector2 position, Vector3 rotation, float scale, float cutOffLOD) {
        this(path, new Vector3(position), rotation, scale, cutOffLOD);
    }

    public Decal(String path, Vector3 position, Vector3 rotation, float scale, float cutOffLOD) {
        this(path, position, rotation, new Vector3(scale, scale, scale), cutOffLOD);
    }

    public Decal(String path, Vector3 position, Vector3 rotation, Vector3 scale, float cutOffLOD) {
        super(position);
        this.path = path;
        if (path.toLowerCase(Locale.ROOT).contains("normal")) {
            this.type = DecalType.Known.NORMALS;
        } else {
            this.type = DecalType.Known.ALBEDO;
        }
        this.rotation = rotation;
        this.scale = scale;
        this.cutOffLOD = cutOffLOD;
    }

    public Decal(String path, Vector3 position, Vector3 rotation, Vector3 scale, float cutOffLOD, DecalType type) {
        super(position);
        this.path = path;
        this.type = type;
        this.rotation = rotation;
        this.scale = scale;
        this.cutOffLOD = cutOffLOD;
    }
}

