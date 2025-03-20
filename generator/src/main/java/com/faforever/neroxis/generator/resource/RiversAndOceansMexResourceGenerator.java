package com.faforever.neroxis.generator.resource;

public class RiversAndOceansMexResourceGenerator extends BasicResourceGenerator {

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand).startVisualDebugger();
        resourceMask.add(passableWater
                                 .copy()
                                 .acid(random.nextFloat(0.3f, 0.7f), 0)
                                 .erode(random.nextFloat(0.5f,0.7f)));
        resourceMask.subtract(unbuildable);
        waterResourceMask.init(resourceMask);
    }
}
