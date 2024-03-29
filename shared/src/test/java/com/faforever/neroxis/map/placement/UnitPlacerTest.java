package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
public class UnitPlacerTest {
    @Test
    public void maxUnitsPlacedTest() {
        Group group = new Group("TestGroup");
        Army army = new Army("Test");
        army.addGroup(group);
        UnitPlacer unitPlacer = new UnitPlacer(0L);
        unitPlacer.placeUnits(new BooleanMask(256, 0L, new SymmetrySettings(Symmetry.POINT2)).invert(),
                              new String[]{"test"}, army, group, 0f);
        assertTrue(army.getNumUnits() <= UnitPlacer.MAX_UNIT_COUNT);
    }
}


