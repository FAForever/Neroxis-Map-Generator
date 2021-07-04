package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Decal extends PositionedObject {
    private final String path;
    private final Vector3 rotation;
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
        if (path.toLowerCase().contains("normal")) {
            this.type = DecalType.NORMALS;
        } else {
            this.type = DecalType.ALBEDO;
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

