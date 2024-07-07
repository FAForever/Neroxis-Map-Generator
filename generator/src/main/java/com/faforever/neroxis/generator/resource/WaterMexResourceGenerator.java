package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.map.SymmetryType;

public class WaterMexResourceGenerator extends BasicResourceGenerator {
    @Override
    public void setupPipeline() {
        resourceMask.setSize(passableLand.getSize()).invert();

        resourceMask.subtract(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false, SymmetryType.TEAM);
        waterResourceMask.init(resourceMask);
    }
}


