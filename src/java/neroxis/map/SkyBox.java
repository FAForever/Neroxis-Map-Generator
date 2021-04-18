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
    private Vector3f horizonColor = new Vector3f(0f, 0f, 0f);
    private Vector3f zenithColor = new Vector3f(0f, 0f, 0f);
    private Vector3f position = new Vector3f(0f, 0f, 0f);
    private float decalGlowMultiplier;
    private String albedo = "";
    private String glow = "";
    private Planet[] Planets = new Planet[]{};
    private Color midRgbColor = new Color(0);
    private float cirrusMultiplier;
    private Vector3f cirrusColor = new Vector3f(0f, 0f, 0f);
    private String cirrusTexture = "";
    private Cirrus[] cirrusLayers = new Cirrus[]{};
    private float clouds7;

    @Data
    public static strictfp class Planet {
        private Vector3f position = new Vector3f(0f, 0f, 0f);
        private float rotation;
        private Vector2f scale = new Vector2f(0f, 0f);
        private Vector4f uv = new Vector4f(0f, 0f, 0f, 0f);
    }

    @Data
    public static strictfp class Cirrus {
        private Vector2f frequency = new Vector2f(0f, 0f);
        private float speed;
        private Vector2f direction = new Vector2f(0f, 0f);
    }
}