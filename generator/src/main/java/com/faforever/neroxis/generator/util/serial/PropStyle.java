package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.BoulderFieldPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.ForrestSomethingPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum PropStyle implements HasParameterConstraints {
    BASIC(BasicPropGenerator::new, ParameterConstraints.ANY),
    BOULDER_FIELD(BoulderFieldPropGenerator::new, ParameterConstraints.ANY),
    ENEMY_CIV(EnemyCivPropGenerator::new, ParameterConstraints.ANY),
    HIGH_RECLAIM(HighReclaimPropGenerator::new, ParameterConstraints.ANY),
    LARGE_BATTLE(LargeBattlePropGenerator::new, ParameterConstraints.ANY),
    NAVY_WRECKS(NavyWrecksPropGenerator::new, ParameterConstraints.ANY),
    NEUTRAL_CIV(NeutralCivPropGenerator::new, ParameterConstraints.ANY),
    ROCK_FIELD(RockFieldPropGenerator::new, ParameterConstraints.ANY),
    SMALL_BATTLE(SmallBattlePropGenerator::new, ParameterConstraints.ANY),
    FORREST_SOMETHING(ForrestSomethingPropGenerator::new, ParameterConstraints.ANY);

    private final Supplier<com.faforever.neroxis.generator.prop.PropGenerator> generatorSupplier;
    private final ParameterConstraints parameterConstraints;

    @JsonIgnore
    @Override
    public ParameterConstraints parameterConstraints() {
        return parameterConstraints;
    }
}
