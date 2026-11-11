package com.faforever.neroxis.mask;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask.OddSymmetryOuterCircleTerrain;
import com.faforever.neroxis.util.Pipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FloatMaskTest {
    private static FloatMask createMask(int size) {
        return new FloatMask(size, null, new SymmetrySettings(Symmetry.NONE));
    }

    private static float height(int x, int y) {
        return x * y + 2f * x + 3f * y;
    }

    private static double sampleHeight(int size, double radius, double angle) {
        double center = (size - 1) / 2d;
        double x = StrictMath.clamp(center + radius * StrictMath.cos(angle), 0, size - 1);
        double y = StrictMath.clamp(center + radius * StrictMath.sin(angle), 0, size - 1);
        return x * y + 2 * x + 3 * y;
    }

    private static void fillWaterAndBlurShoreline(FloatMask mask, float waterHeight) {
        int size = mask.getSize();
        float radius = (size - 1) / 2f;
        BooleanMask outerCircle = new BooleanMask(size, null, mask.getSymmetrySettings());
        outerCircle.fillCircle(radius, radius, radius, true).invert();
        mask.setToValue(outerCircle, waterHeight - 8);
        mask.blur(5, outerCircle.outline().inflate(5));
    }

    @ParameterizedTest
    @CsvSource({"17, 3", "17, 5", "17, 7", "17, 9", "17, 15", "16, 3", "16, 5", "65, 15", "1, 3"})
    public void rotatesWithBilinearInterpolationThenFillsWaterAndBlursShoreline(int size, int numTeams) {
        FloatMask mask = createMask(size).set(FloatMaskTest::height);
        FloatMask expected = createMask(size);
        float waterHeight = 17f;

        double center = (size - 1) / 2d;
        double sliceAngle = 2 * StrictMath.PI / numTeams;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = x - center;
                double dy = y - center;
                double angle = StrictMath.atan2(center - y, center - x);
                if (angle < 0) {
                    angle += 2 * StrictMath.PI;
                }
                if (dx * dx + dy * dy > center * center || (dx == 0 && dy == 0)) {
                    expected.setPrimitive(x, y, height(x, y));
                } else {
                    double radius = StrictMath.hypot(x - center, y - center);
                    double localAngle = angle % sliceAngle;
                    double overlapAngle = StrictMath.min(sliceAngle / 2, 4 / radius);
                    double distance = StrictMath.min(localAngle, sliceAngle - localAngle);
                    double sourceAngle = localAngle + StrictMath.PI;
                    // A bilinear surface is reproduced exactly by bilinear interpolation.
                    double value = sampleHeight(size, radius, sourceAngle);
                    if (distance < overlapAngle) {
                        double adjacentAngle = sourceAngle + (localAngle < sliceAngle / 2 ? sliceAngle : -sliceAngle);
                        double weight = (1 - distance / overlapAngle) / 2;
                        double blend = 6 * StrictMath.pow(weight, 5) - 15 * StrictMath.pow(weight, 4)
                                       + 10 * StrictMath.pow(weight, 3);
                        value = value * (1 - blend) + sampleHeight(size, radius, adjacentAngle) * blend;
                    }
                    expected.setPrimitive(x, y, (float) value);
                }
            }
        }
        fillWaterAndBlurShoreline(expected, waterHeight);

        assertSame(mask, mask.forceSmoothTerrainRotation(numTeams, waterHeight, OddSymmetryOuterCircleTerrain.OCEAN));

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                assertEquals(expected.getPrimitive(x, y), mask.getPrimitive(x, y), 0.001, "Pixel " + x + ", " + y);
            }
        }
    }

    @Test
    public void blendsWraparoundSeamAndReducesHeightJumps() {
        FloatMask mask = createMask(129).set((x, y) -> (float) y);

        mask.forceSmoothTerrainRotation(3, 17f, OddSymmetryOuterCircleTerrain.OCEAN);

        double otherEdgeHeight = 64 - 24 * StrictMath.sqrt(3);
        assertEquals((64 + otherEdgeHeight) / 2, mask.getPrimitive(16, 64), 0.00001);
        assertTrue(StrictMath.abs(mask.getPrimitive(16, 63) - mask.getPrimitive(16, 64)) < 10);
        assertTrue(StrictMath.abs(mask.getPrimitive(16, 65) - mask.getPrimitive(16, 64)) < 10);
        assertTrue(mask.getPrimitive(16, 61) < 61f);
        assertTrue(mask.getPrimitive(16, 67) > otherEdgeHeight - 1.5);
        assertEquals(59f, mask.getPrimitive(16, 59));
        assertEquals(otherEdgeHeight - 2.5, mask.getPrimitive(16, 69), 0.00001);
    }

    @ParameterizedTest
    @CsvSource({"16, 63", "16, 65", "87, 22", "89, 22", "87, 106", "89, 106",
                "16, 61", "16, 67", "85, 22", "91, 22", "85, 106", "91, 106"})
    public void blendsBothSidesOfEverySliceBoundary(int x, int y) {
        FloatMask mask = createMask(129).set(FloatMaskTest::height);
        double radius = StrictMath.hypot(x - 64, y - 64);
        double sliceAngle = 2 * StrictMath.PI / 3;
        double angle = StrictMath.atan2(64 - y, 64 - x);
        if (angle < 0) {
            angle += 2 * StrictMath.PI;
        }
        double localAngle = angle % sliceAngle;
        double distance = radius * StrictMath.min(localAngle, sliceAngle - localAngle);
        assertTrue(distance < 4);
        double sourceAngle = localAngle + StrictMath.PI;
        double adjacentAngle = sourceAngle + (localAngle < sliceAngle / 2 ? sliceAngle : -sliceAngle);
        double sourceX = 64 + radius * StrictMath.cos(sourceAngle);
        double sourceY = 64 + radius * StrictMath.sin(sourceAngle);
        double adjacentX = 64 + radius * StrictMath.cos(adjacentAngle);
        double adjacentY = 64 + radius * StrictMath.sin(adjacentAngle);
        double sourceHeight = sourceX * sourceY + 2 * sourceX + 3 * sourceY;
        double adjacentHeight = adjacentX * adjacentY + 2 * adjacentX + 3 * adjacentY;
        double weight = (1 - distance / 4) / 2;
        double blend = 6 * StrictMath.pow(weight, 5) - 15 * StrictMath.pow(weight, 4) + 10 * StrictMath.pow(weight, 3);
        double expected = sourceHeight * (1 - blend) + adjacentHeight * blend;

        mask.forceSmoothTerrainRotation(3, 17f, OddSymmetryOuterCircleTerrain.OCEAN);

        assertEquals(expected, mask.getPrimitive(x, y), 0.001);
    }

    @Test
    public void interpolationUsesAllFourNeighbors() {
        FloatMask mask = createMask(65);
        mask.setPrimitive(29, 19, 10);
        mask.setPrimitive(30, 19, 20);
        mask.setPrimitive(29, 20, 30);
        mask.setPrimitive(30, 20, 50);

        mask.forceSmoothTerrainRotation(3, 17f, OddSymmetryOuterCircleTerrain.OCEAN);

        double xFraction = 2 * StrictMath.sqrt(3) - 3;
        double yFraction = 11 - 6 * StrictMath.sqrt(3);
        double low = 10 * (1 - xFraction) + 20 * xFraction;
        double high = 30 * (1 - xFraction) + 50 * xFraction;
        assertEquals(low * (1 - yFraction) + high * yFraction, mask.getPrimitive(44, 36), 0.00001);
    }

    @ParameterizedTest
    @CsvSource({"65, 3", "65, 5", "64, 3", "64, 5", "65, 15"})
    public void fillsOutsideInscribedCircleWithWaterBeyondShoreline(int size, int numTeams) {
        FloatMask mask = createMask(size).set(FloatMaskTest::height);

        mask.forceSmoothTerrainRotation(numTeams, 17f, OddSymmetryOuterCircleTerrain.OCEAN);

        double center = (size - 1) / 2d;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = x - center;
                double dy = y - center;
                if (StrictMath.hypot(dx, dy) > center + 14) {
                    assertEquals(9f, mask.getPrimitive(x, y));
                }
            }
        }
    }

    @Test
    public void blursShorelineOnCircleBoundary() {
        FloatMask mask = createMask(129).add(100f);
        FloatMask expected = createMask(129).add(100f);
        fillWaterAndBlurShoreline(expected, 17f);

        mask.forceSmoothTerrainRotation(3, 17f, OddSymmetryOuterCircleTerrain.OCEAN);

        assertEquals(expected.getPrimitive(109, 109), mask.getPrimitive(109, 109));
        assertTrue(mask.getPrimitive(109, 109) > 17f);
        assertTrue(mask.getPrimitive(109, 109) < 100f);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 15})
    public void constantHeightHasNoHoles(int numTeams) {
        FloatMask mask = createMask(33).add(42f).forceSmoothTerrainRotation(numTeams, 50f, OddSymmetryOuterCircleTerrain.OCEAN);

        for (int x = 0; x < mask.getSize(); x++) {
            for (int y = 0; y < mask.getSize(); y++) {
                assertEquals(42f, mask.getPrimitive(x, y));
            }
        }
    }

    @Test
    public void singleTeamOnlyFillsWaterAndBlursShoreline() {
        FloatMask mask = createMask(17).set(FloatMaskTest::height)
                                       .forceSmoothTerrainRotation(1, 17f, OddSymmetryOuterCircleTerrain.OCEAN);
        FloatMask expected = createMask(17).set(FloatMaskTest::height);
        fillWaterAndBlurShoreline(expected, 17f);

        for (int x = 0; x < mask.getSize(); x++) {
            for (int y = 0; y < mask.getSize(); y++) {
                assertEquals(expected.getPrimitive(x, y), mask.getPrimitive(x, y));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"3, false", "3, true", "5, false", "5, true", "7, false", "7, true"})
    public void fillsOuterCircleWithWaterAndBlursShorelineAfterRotation(int numTeams, boolean pipelined) {
        int size = 129;
        float waterHeight = 17f;
        SymmetrySettings settings = new SymmetrySettings(Symmetry.valueOf("POINT" + numTeams));
        FloatMask expected = new FloatMask(size, null, settings).add(100f);
        fillWaterAndBlurShoreline(expected, waterHeight);

        FloatMask actual = new FloatMask(9, null, settings);
        FloatMask result = new FloatMask(size, null, settings);
        Runnable setup = () -> {
            actual.setSize(size).add(100f);
            assertSame(actual, actual.forceSmoothTerrainRotation(numTeams, waterHeight, OddSymmetryOuterCircleTerrain.OCEAN));
            result.init(actual);
        };
        if (pipelined) {
            Pipeline.run(setup);
        } else {
            setup.run();
        }

        assertEquals(waterHeight - 8, result.getPrimitive(0, 0));
        assertEquals(waterHeight - 8, result.getPrimitive(size - 1, size - 1));
        assertEquals(100f, result.getPrimitive(64, 64));
        assertTrue(result.getPrimitive(109, 109) > waterHeight);
        assertTrue(result.getPrimitive(109, 109) < actual.getPrimitive(64, 64));
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                assertEquals(expected.getPrimitive(x, y), result.getPrimitive(x, y));
            }
        }
    }

    @Test
    public void runsAfterQueuedResizeAndBeforeDependentOperations() {
        FloatMask mask = createMask(9);
        FloatMask expected = createMask(17).set(FloatMaskTest::height)
                                           .forceSmoothTerrainRotation(5, 17f, OddSymmetryOuterCircleTerrain.OCEAN).add(7f);
        FloatMask result = createMask(17);

        Pipeline.run(() -> {
            mask.setSize(17).set(FloatMaskTest::height).forceSmoothTerrainRotation(5, 17f, OddSymmetryOuterCircleTerrain.OCEAN);
            result.init(mask).add(7f);
        });

        for (int x = 0; x < result.getSize(); x++) {
            for (int y = 0; y < result.getSize(); y++) {
                assertEquals(expected.getPrimitive(x, y), result.getPrimitive(x, y));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"129, 1, false", "129, 1, true", "129, 3, false", "129, 3, true",
                "513, 1, false", "513, 1, true", "513, 3, false", "513, 3, true"})
    public void fillsOuterCircleWithMountainBrushes(int size, int numTeams, boolean pipelined) {
        float waterHeight = 17f;
        SymmetrySettings settings = new SymmetrySettings(numTeams == 1 ? Symmetry.NONE : Symmetry.POINT3);
        FloatMask expected = new FloatMask(size, new SplittableRandom(123), settings).add(100f);
        BooleanMask outerCircle = new BooleanMask(size, null, settings);
        float radius = (size - 1) / 2f;
        outerCircle.fillCircle(radius, radius, radius, true).invert();
        expected.setToValue(outerCircle, waterHeight + 0.5f);
        expected.blur(5, outerCircle.copy().outline().inflate(5));
        assert expected.random != null;
        String brush = Brushes.GENERATOR_BRUSHES.get(expected.random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        expected.useBrushWithinAreaWithDensity(outerCircle, brush, size - 1 < 512 ? 32 : 64,
                                              size - 1 < 512 ? .1f : .05f, 10f, false);

        FloatMask actual = new FloatMask(9, new SplittableRandom(123), settings);
        FloatMask result = new FloatMask(size, null, settings);
        Runnable setup = () -> {
            actual.setSize(size).add(100f);
            assertSame(actual, actual.forceSmoothTerrainRotation(numTeams, waterHeight, OddSymmetryOuterCircleTerrain.MOUNTAINS));
            result.init(actual);
        };
        if (pipelined) {
            Pipeline.run(setup);
        } else {
            setup.run();
        }

        assertTrue(result.getMax() > 100f);
        assertEquals(100f, result.getPrimitive(size / 2, size / 2));
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                assertEquals(expected.getPrimitive(x, y), result.getPrimitive(x, y));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    public void flatLandFillPreservesMatchingConstantHeight(int numTeams) {
        FloatMask mask = createMask(33).add(42f);

        assertSame(mask, mask.forceSmoothTerrainRotation(numTeams, 41.5f, OddSymmetryOuterCircleTerrain.FLAT_LAND));

        assertEquals(42f, mask.getMin());
        assertEquals(42f, mask.getMax());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    public void rejectsNonPositiveTeamCounts(int numTeams) {
        FloatMask mask = createMask(17);

        assertThrows(IllegalArgumentException.class,
                     () -> mask.forceSmoothTerrainRotation(numTeams, 17f, OddSymmetryOuterCircleTerrain.OCEAN));
    }
}
