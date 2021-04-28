package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;

import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("unchecked")
public strictfp abstract class ComparableMask<T extends Comparable<T>, U extends OperationsMask<T, U>> extends OperationsMask<T, U> {

    protected ComparableMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public ComparableMask(U other, Long seed) {
        super(other, seed);
    }

    public ComparableMask(U other, Long seed, String name) {
        super(other, seed, name);
    }

    public boolean valueAtEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) == 0;
    }

    public boolean valueAtLessThan(int x, int y, T value) {
        return get(x, y).compareTo(value) < 0;
    }

    public boolean valueAtLessThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) <= 0;
    }

    public boolean valueAtGreaterThan(int x, int y, T value) {
        return get(x, y).compareTo(value) > 0;
    }

    public boolean valueAtGreaterThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) >= 0;
    }

    public boolean isLocalMax(int x, int y) {
        T value = get(x, y);
        return ((x > 0 && valueAtLessThanEqualTo(x - 1, y, value))
                && (x < getSize() - 1 && valueAtLessThanEqualTo(x + 1, y, value))
                && (y > 0 && valueAtLessThanEqualTo(x, y - 1, value))
                && (y < getSize() - 1 && valueAtLessThanEqualTo(x, y + 1, value))
                && valueAtLessThanEqualTo(x - 1, y - 1, value)
                && valueAtLessThanEqualTo(x + 1, y - 1, value)
                && valueAtLessThanEqualTo(x - 1, y + 1, value)
                && valueAtLessThanEqualTo(x + 1, y + 1, value));
    }

    public boolean isLocal1DMax(int x, int y) {
        T value = get(x, y);
        return (((x > 0 && valueAtLessThanEqualTo(x - 1, y, value))
                && (x < getSize() - 1 && valueAtLessThanEqualTo(x + 1, y, value)))
                || ((y > 0 && valueAtLessThanEqualTo(x, y - 1, value))
                && (y < getSize() - 1 && valueAtLessThanEqualTo(x, y + 1, value))));
    }

    public T getMin() {
        return Arrays.stream(mask).flatMap(Arrays::stream).min(Comparator.comparing(value -> value)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMax() {
        return Arrays.stream(mask).flatMap(Arrays::stream).max(Comparator.comparing(value -> value)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public U init(BooleanMask other, T low, T high) {
        plannedSize = other.getSize();
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            setSize(source.getSize());
            assertCompatibleMask(source);
            set((x, y) -> source.get(x, y) ? high : low);
        }, other);
        return (U) this;
    }

    public U max(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            set((x, y) -> {
                T thisVal = get(x, y);
                T otherVal = source.get(x, y);
                return thisVal.compareTo(otherVal) > 0 ? thisVal : otherVal;
            });
        }, other);
        return (U) this;
    }

    public U clampMax(BooleanMask other, T val) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.get(x, y) ? (thisVal.compareTo(val) < 0 ? val : thisVal) : thisVal;
            });
        }, other);
        return (U) this;
    }

    public U clampMax(T val) {
        enqueue(() -> {
            set((x, y) -> {
                T thisVal = get(x, y);
                return thisVal.compareTo(val) < 0 ? thisVal : val;
            });
        });
        return (U) this;
    }

    public U min(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            set((x, y) -> {
                T thisVal = get(x, y);
                T otherVal = source.get(x, y);
                return thisVal.compareTo(otherVal) < 0 ? thisVal : otherVal;
            });
        }, other);
        return (U) this;
    }

    public U clampMin(BooleanMask other, T val) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.get(x, y) ? (thisVal.compareTo(val) > 0 ? val : thisVal) : thisVal;
            });
        }, other);
        return (U) this;
    }

    public U clampMin(T val) {
        enqueue(() -> {
            set((x, y) -> {
                T thisVal = get(x, y);
                return thisVal.compareTo(val) > 0 ? thisVal : val;
            });
        });
        return (U) this;
    }

    public U threshold(T val) {
        enqueue(() -> {
            set((x, y) -> {
                T thisVal = get(x, y);
                return thisVal.compareTo(val) > 0 ? getZeroValue() : get(x, y);
            });
        });
        return (U) this;
    }

    public U zeroOutsideRange(T min, T max) {
        enqueue(() -> {
            set((x, y) -> valueAtLessThan(x, y, min) || valueAtGreaterThan(x, y, max) ? getZeroValue() : get(x, y));
        });
        return (U) this;
    }

    public U zeroInRange(T min, T max) {
        enqueue(() -> {
            set((x, y) -> valueAtGreaterThanEqualTo(x, y, min) && valueAtLessThan(x, y, max) ? getZeroValue() : get(x, y));
        });
        return (U) this;
    }

    public BooleanMask convertToBooleanMask(T minValue) {
        Long seed = random != null ? random.nextLong() : null;
        return new BooleanMask(this, minValue, seed, getName() + "toBoolean");
    }

    public BooleanMask convertToBooleanMask(T minValue, T maxValue) {
        Long seed = random != null ? random.nextLong() : null;
        return new BooleanMask(this, minValue, maxValue, seed, getName() + "toBoolean");
    }

    public BooleanMask getLocalMaximums(T minValue, T maxValue) {
        Long seed = random != null ? random.nextLong() : null;
        BooleanMask localMaxima = new BooleanMask(getSize(), seed, symmetrySettings, getName() + "Maximas", isParallel());
        enqueue(localMaxima, dependencies ->
                applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
                    T value = get(x, y);
                    if (value.compareTo(minValue) >= 0 && value.compareTo(maxValue) < 0 && isLocalMax(x, y)) {
                        localMaxima.applyAtSymmetryPoints(x, y, SymmetryType.SPAWN, (sx, sy) -> localMaxima.set(x, y, true));
                    }
                }));
        return localMaxima;
    }

    public BooleanMask getLocal1DMaximums(T minValue, T maxValue) {
        Long seed = random != null ? random.nextLong() : null;
        BooleanMask localMaxima = new BooleanMask(getSize(), seed, symmetrySettings, getName() + "1DMaximas", isParallel());
        enqueue(localMaxima, dependencies ->
                applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
                    T value = get(x, y);
                    if (value.compareTo(minValue) >= 0 && value.compareTo(maxValue) < 0 && isLocal1DMax(x, y)) {
                        localMaxima.applyAtSymmetryPoints(x, y, SymmetryType.SPAWN, (sx, sy) -> localMaxima.set(x, y, true));
                    }
                }), localMaxima);

        return localMaxima;
    }

    public FloatMask getDistanceFieldForRange(T minValue, T maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }
}
