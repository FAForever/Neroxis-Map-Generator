package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import lombok.Data;

import java.awt.*;

@Data
public strictfp class SkyBox {
    private float horizonHeight;
    private float scale;
    private float subHeight;
    private int subDivAx;
    private int subDivHeight;
    private float zenithHeight;
    private Vector3 horizonColor = new Vector3(0f, 0f, 0f);
    private Vector3 zenithColor = new Vector3(0f, 0f, 0f);
    private Vector3 position = new Vector3(0f, 0f, 0f);
    private float decalGlowMultiplier;
    private String albedo = "";
    private String glow = "";
    private Planet[] Planets = new Planet[]{};
    private Color midRgbColor = new Color(0);
    private float cirrusMultiplier;
    private Vector3 cirrusColor = new Vector3(0f, 0f, 0f);
    private String cirrusTexture = "";
    private Cirrus[] cirrusLayers = new Cirrus[]{};
    private float clouds7;

    @Data
    public static strictfp class Planet {
        private Vector3 position = new Vector3(0f, 0f, 0f);
        private float rotation;
        private Vector2 scale = new Vector2(0f, 0f);
        private Vector4 uv = new Vector4(0f, 0f, 0f, 0f);
    }

    @Data
    public static strictfp class Cirrus {
        private Vector2 frequency = new Vector2(0f, 0f);
        private float speed;
        private Vector2 direction = new Vector2(0f, 0f);
    }
}