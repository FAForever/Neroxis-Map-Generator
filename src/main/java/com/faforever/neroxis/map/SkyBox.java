package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import lombok.Data;

import java.awt.*;

@Data
public strictfp class SkyBox {
    private float horizonHeight = -35;
    private float scale = 1000;
    private float subHeight = 1;
    private int subDivAx = 16;
    private int subDivHeight = 6;
    private float zenithHeight = 150;
    private Vector3 horizonColor = new Vector3(.5f, .5f, .5f);
    private Vector3 zenithColor = new Vector3(.2f, .2f, .2f);
    private Vector3 position = new Vector3(256f, 0f, 256f);
    private float decalGlowMultiplier;
    private String albedo = "/textures/environment/Decal_test_Albedo001.dds";
    private String glow = "/textures/environment/Decal_test_Glow001.dds";
    private Planet[] Planets = new Planet[]{};
    private Color midRgbColor = new Color(0);
    private float cirrusMultiplier;
    private Vector3 cirrusColor = new Vector3(1f, 1f, 1f);
    private String cirrusTexture = "/textures/environment/cirrus000.dds";
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