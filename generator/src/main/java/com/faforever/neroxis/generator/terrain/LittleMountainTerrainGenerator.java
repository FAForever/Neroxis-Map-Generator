package com.faforever.neroxis.generator.terrain;

public class LittleMountainTerrainGenerator extends PathedPlateauTerrainGenerator {

    @Override
    protected void afterInitialize() {
        super.afterInitialize();
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        mountains.setSize(mapSize / 4);

        mountains.randomWalk(
                (int) (mountainDensity * 250 / symmetrySettings.terrainSymmetry().getNumSymPoints() + 100),
                mapSize / 128);

        mountains.setSize(mapSize + 1);
    }
}
