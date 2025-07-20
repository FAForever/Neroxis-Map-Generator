package com.faforever.neroxis.generator.resource;

public class RiversAndOceansMexResourceGenerator extends BasicResourceGenerator {

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand);
        resourceMask.add(passableWater
                                 .copy()
                                 .acid(random.nextFloat(0.3f, 0.7f), 0)
                                 .erode(random.nextFloat(0.6f,0.9f)));
        resourceMask.subtract(unbuildable.copy().inflate(2));
        waterResourceMask.init(resourceMask);
    }
}
