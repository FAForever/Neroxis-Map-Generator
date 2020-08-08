package map;

import lombok.Data;
import util.Vector2f;
import util.Vector3f;

@Data
public strictfp class Decal {
    private final String path;
    private final float rotation;
    private final Vector3f scale;
    private final int type;
    private final float cutOffLOD;
    private Vector3f position;

    public Decal(String path, Vector2f position, float rotation, float scale, float cutOffLOD) {
        this(path, new Vector3f(position), rotation, scale, cutOffLOD);
    }

    public Decal(String path, Vector3f position, float rotation, float scale, float cutOffLOD) {
        this(path, position, rotation, new Vector3f(scale, scale, scale), cutOffLOD);
    }

    public Decal(String path, Vector3f position, float rotation, Vector3f scale, float cutOffLOD) {
        this.path = path;
        if (path.contains("normal")) {
            this.type = 2;
        } else {
            this.type = 1;
        }
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        this.cutOffLOD = cutOffLOD;
    }

}

