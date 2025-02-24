package com.faforever.neroxis.generator.resource;

public class RiversResourceGenerator extends BasicResourceGenerator {

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand);
        resourceMask.subtract(unbuildable);
        waterResourceMask.init(resourceMask);
    }
}
