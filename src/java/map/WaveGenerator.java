package map;

import lombok.Data;
import util.Vector3f;

@Data
public strictfp class WaveGenerator {
    private final String textureName;
    private final String rampName;
    private final Vector3f position;
    private final float rotation;
    private final Vector3f velocity;
}
