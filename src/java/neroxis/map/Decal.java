package neroxis.map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class Decal extends PositionedObject {
    private final String path;
    private final Vector3f rotation;
    private Vector3f scale;
    private final DecalType type;
    private float cutOffLOD;

    public Decal(String path, Vector2f position, Vector3f rotation, float scale, float cutOffLOD) {
        this(path, new Vector3f(position), rotation, scale, cutOffLOD);
    }

    public Decal(String path, Vector3f position, Vector3f rotation, float scale, float cutOffLOD) {
        this(path, position, rotation, new Vector3f(scale, scale, scale), cutOffLOD);
    }

    public Decal(String path, Vector3f position, Vector3f rotation, Vector3f scale, float cutOffLOD) {
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

    public Decal(String path, Vector3f position, Vector3f rotation, Vector3f scale, float cutOffLOD, DecalType type) {
        super(position);
        this.path = path;
        this.type = type;
        this.rotation = rotation;
        this.scale = scale;
        this.cutOffLOD = cutOffLOD;
    }

}

