package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.BoulderFieldPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum PropStyle {
    BASIC(BasicPropGenerator::new),
    BOULDER_FIELD(BoulderFieldPropGenerator::new),
    ENEMY_CIV(EnemyCivPropGenerator::new),
    HIGH_RECLAIM(HighReclaimPropGenerator::new),
    LARGE_BATTLE(LargeBattlePropGenerator::new),
    NAVY_WRECKS(NavyWrecksPropGenerator::new),
    NEUTRAL_CIV(NeutralCivPropGenerator::new),
    ROCK_FIELD(RockFieldPropGenerator::new),
    SMALL_BATTLE(SmallBattlePropGenerator::new);

    private final Supplier<PropGenerator> generatorSupplier;
}
