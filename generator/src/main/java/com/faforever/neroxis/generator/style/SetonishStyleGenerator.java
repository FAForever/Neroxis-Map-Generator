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

import java.util.List;

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

        List<Symmetry> VALID_SYMMETRIES = List.of(Symmetry.POINT2, Symmetry.DIAG, Symmetry.XZ, Symmetry.ZX, Symmetry.X, Symmetry.Z);
        SymmetrySettings currentSymmetrySettings = getSymmetrySettings();

        if (!VALID_SYMMETRIES.contains(currentSymmetrySettings.terrainSymmetry())
            || !VALID_SYMMETRIES.contains(currentSymmetrySettings.spawnSymmetry())
            || !VALID_SYMMETRIES.contains(currentSymmetrySettings.teamSymmetry())
        ) {
            symmetrySettings = new SymmetrySettings(Symmetry.POINT2, Symmetry.POINT2, Symmetry.POINT2);
        }
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
