package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public strictfp class WaveGenerator extends PositionedObject {
    private final String textureName;
    private final String rampName;
    private final float rotation;
    private final Vector3 velocity;
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

    public WaveGenerator(String textureName, String rampName, Vector3 position, float rotation, Vector3 velocity) {
        super(position);
        this.textureName = textureName;
        this.rampName = rampName;
        this.rotation = rotation;
        this.velocity = velocity;
    }
}
