package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import lombok.Data;

import java.awt.*;

@Data
public strictfp class SkyBox {
    private float horizonHeight = -42.5f;
    private float scale = 1171.5814f;
    private float subHeight = 1.256637f;
    private int subDivAx = 16;
    private int subDivHeight = 6;
    private float zenithHeight = 293.50708f;
    private Vector3 horizonColor = new Vector3(0.6485937f, 0.8204687f, 0.84f);
    private Vector3 zenithColor = new Vector3(0.21999997f, 0.40999997f, 0.71999997f);
    private Vector3 position = new Vector3(256f, 0f, 256f);
    private float decalGlowMultiplier = .1f;
    private String albedo = "/textures/environment/Decal_test_Albedo001.dds";
    private String glow = "/textures/environment/Decal_test_Glow001.dds";
    private Planet[] Planets = new Planet[]{new Planet()};
    private Color midRgbColor = new Color(0);
    private float cirrusMultiplier = 1.8f;
    private Vector3 cirrusColor = new Vector3(1.16f, 1.16f, 1.23f);
    private String cirrusTexture = "/textures/environment/cirrus000.dds";
    private Cirrus[] cirrusLayers = new Cirrus[]{new Cirrus()};
    private float clouds7 = 0;

    @Data
    public static strictfp class Planet {
        private Vector3 position = new Vector3(2190.5806f, 570.7427f, -1020.0027f);
        private float rotation = -1.5847589f;
        private Vector2 scale = new Vector2(183.0f, 183.0f);
        private Vector4 uv = new Vector4(0.0f, 0.5f, 0.5f, 0.5f);
    }

    @Data
    public static strictfp class Cirrus {
        private Vector2 frequency = new Vector2(.0001f, .0001f);
        private float speed = 7.7999997f;
        private Vector2 direction = new Vector2(0.4320857f, -0.9018325f);
    }
}