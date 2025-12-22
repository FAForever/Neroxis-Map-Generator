package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.SetonishLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;

public class SetonishStyleGenerator extends StyleGenerator {
    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(512, 1024)
                                   .numTeams(2, 2)
                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);

        WeightedOptionsWithFallback<Symmetry> limitedTerrainSymmetries = WeightedOptionsWithFallback.of(
                Symmetry.POINT2,
                new WeightedOption<>(Symmetry.POINT2, 1f),
                new WeightedOption<>(Symmetry.DIAG, 1f),
                new WeightedOption<>(Symmetry.XZ, 1f),
                new WeightedOption<>(Symmetry.ZX, 1f),
                new WeightedOption<>(Symmetry.X, 1f),
                new WeightedOption<>(Symmetry.Z, 1f)
        );
        WeightedOptionsWithFallback<Symmetry> limitedSpawnSymmetries = WeightedOptionsWithFallback.of(
                Symmetry.POINT2,
                new WeightedOption<>(Symmetry.POINT2, 1f),
                new WeightedOption<>(Symmetry.XZ, 1f),
                new WeightedOption<>(Symmetry.ZX, 1f),
                new WeightedOption<>(Symmetry.X, 1f),
                new WeightedOption<>(Symmetry.Z, 1f)
        );

        Symmetry terrainSymmetry = limitedTerrainSymmetries.select(random);
        Symmetry spawnAndTeamSymmetry = limitedSpawnSymmetries.select(random);

        symmetrySettings =  new SymmetrySettings(terrainSymmetry, spawnAndTeamSymmetry, spawnAndTeamSymmetry);
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new SetonishLastTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator(),
                                              new WeightedOption<>(new BasicPropGenerator(), 1f),
                                              new WeightedOption<>(new HighReclaimPropGenerator(), .25f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), .5f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 1f));
    }
}
