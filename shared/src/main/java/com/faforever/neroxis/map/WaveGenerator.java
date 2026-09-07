package com.faforever.neroxis.map;

import com.faforever.neroxis.util.vector.Vector3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public final class WaveGenerator extends PositionedObject {
    private String textureName;
    private String rampName;
    private float rotation;
    private Vector3 velocity;
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
