package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.BoulderFieldPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
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
    protected void initialize(GeneratorParameters generatorParameters) {
        super.initialize(generatorParameters);

        SymmetrySettings currentSymmetrySettings = getSymmetrySettings();
        switch (currentSymmetrySettings.terrainSymmetry()) {
            case Symmetry.ZX -> {
                this.setSymmetrySettings(new SymmetrySettings(Symmetry.ZX, Symmetry.ZX, Symmetry.ZX));
            }
            case Symmetry.X -> {
                this.setSymmetrySettings(new SymmetrySettings(Symmetry.X, Symmetry.X, Symmetry.X));
            }
            case Symmetry.Z -> {
                this.setSymmetrySettings(new SymmetrySettings(Symmetry.Z, Symmetry.Z, Symmetry.Z));
            }
            default -> {
                this.setSymmetrySettings(new SymmetrySettings(Symmetry.POINT2, Symmetry.XZ, Symmetry.POINT2));
            }
        }
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicResourceGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new SetonishLastTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator(),
                                              new WeightedOption<>(new BasicPropGenerator(), 1f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), .5f),
                                              new WeightedOption<>(new BoulderFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 2f));
    }
}
