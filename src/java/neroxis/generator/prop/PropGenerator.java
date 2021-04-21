package neroxis.generator.prop;

import lombok.Getter;
import neroxis.generator.ElementGenerator;
import neroxis.generator.placement.PropPlacer;
import neroxis.generator.placement.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.BinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

@Getter
public abstract strictfp class PropGenerator extends ElementGenerator {
    protected UnitPlacer unitPlacer;
    protected PropPlacer propPlacer;
    protected BinaryMask impassable;
    protected BinaryMask unbuildable;
    protected BinaryMask passableLand;

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
