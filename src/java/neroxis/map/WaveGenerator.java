package neroxis.map;

import lombok.Data;
import neroxis.util.Vector3f;

@Data
public strictfp class WaveGenerator {
    private final String textureName;
    private final String rampName;
    private final Vector3f position;
    private final float rotation;
    private final Vector3f velocity;

    private float lifeTimeFirst;
    private float lifeTimeSecond;
    private float periodFirst;
    private float periodSecond;
    private float scaleFirst;
    private float scaleSecond;

    private float frameCount;
    private float frameRateFirst;
    private float frameRateSecond;
    private float stripCount;
}
