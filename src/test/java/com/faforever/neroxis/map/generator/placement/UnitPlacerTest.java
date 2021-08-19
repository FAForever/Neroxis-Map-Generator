package com.faforever.neroxis.map.generator.placement;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class UnitPlacerTest {

    @Test
    public void maxUnitsPlacedTest() {
        Group group = new Group("TestGroup", new ArrayList<>());
        Army army = new Army("Test", Collections.singletonList(group));
        UnitPlacer unitPlacer = new UnitPlacer(0L);
        unitPlacer.placeUnits(new BooleanMask(256, 0L, new SymmetrySettings(Symmetry.POINT2)).invert(), new String[]{"test"}, army, group, 0f);
        assertTrue(army.getNumUnits() <= UnitPlacer.MAX_UNIT_COUNT);
    }
}


