package neroxis.map;

import lombok.Data;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;
import neroxis.util.Vector4f;

import java.awt.*;

@Data
public strictfp class SkyBox {
    private float horizonHeight;
    private float scale;
    private float subHeight;
    private int subDivAx;
    private int subDivHeight;
    private float zenithHeight;
    private Vector3f horizonColor;
    private Vector3f zenithColor;
    private Vector3f position;
    private float decalGlowMultiplier;
    private String albedo;
    private String glow;
    private Planet[] Planets;
    private Color midRgbColor;
    private float cirrusMultiplier;
    private Vector3f cirrusColor;
    private String cirrusTexture;
    private Cirrus[] cirrusLayers;
    private float clouds7;

    @Data
    public static strictfp class Planet {
        private Vector3f position;
        private float rotation;
        private Vector2f scale;
        private Vector4f uv;
    }

    @Data
    public static strictfp class Cirrus {
        private Vector2f frequency;
        private float speed;
        private Vector2f direction;
    }
}