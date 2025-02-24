package com.faforever.neroxis.generator.resource;

public class RiversAndOceansMexResourceGenerator extends BasicResourceGenerator {

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand).startVisualDebugger();
        resourceMask.add(passableWater);
        resourceMask.subtract(unbuildable);
        waterResourceMask.init(resourceMask);
    }
}
