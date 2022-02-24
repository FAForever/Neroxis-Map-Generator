package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.placement.PropPlacer;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Getter;

@Getter
public abstract strictfp class PropGenerator extends ElementGenerator {
    protected UnitPlacer unitPlacer;
    protected PropPlacer propPlacer;
    protected BooleanMask impassable;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.impassable = terrainGenerator.getImpassable();
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        unitPlacer = new UnitPlacer(random.nextLong());
        propPlacer = new PropPlacer(map, random.nextLong());
    }

    public abstract void placeProps();

    public abstract void placeUnits();
}
