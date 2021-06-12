package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;

import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class ComparableMask<T extends Comparable<T>, U extends ComparableMask<T, U>> extends OperationsMask<T, U> {

    protected ComparableMask(Class<T> objectClass, int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(objectClass, size, seed, symmetrySettings, name, parallel);
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

    public U max(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                T otherVal = source.get(x, y);
                return thisVal.compareTo(otherVal) > 0 ? thisVal : otherVal;
            });
        }, other);
    }

    public U clampMax(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.get(x, y) ? (thisVal.compareTo(val) < 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    public U clampMax(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) < 0 ? thisVal : val;
        });
    }

    public U min(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                T otherVal = source.get(x, y);
                return thisVal.compareTo(otherVal) < 0 ? thisVal : otherVal;
            });
        }, other);
    }

    public U clampMin(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.get(x, y) ? (thisVal.compareTo(val) > 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    public U clampMin(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? thisVal : val;
        });
    }

    public U threshold(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? getZeroValue() : get(x, y);
        });
    }

    public U zeroOutsideRange(T min, T max) {
        return set((x, y) -> valueAtLessThan(x, y, min) || valueAtGreaterThan(x, y, max) ? getZeroValue() : get(x, y));
    }

    public U zeroInRange(T min, T max) {
        return set((x, y) -> valueAtGreaterThanEqualTo(x, y, min) && valueAtLessThan(x, y, max) ? getZeroValue() : get(x, y));
    }

    public BooleanMask convertToBooleanMask(T minValue) {
        return new BooleanMask(this, minValue, getNextSeed(), getName() + "toBoolean");
    }

    public BooleanMask convertToBooleanMask(T minValue, T maxValue) {
        return new BooleanMask(this, minValue, maxValue, getNextSeed(), getName() + "toBoolean");
    }

    public BooleanMask getLocalMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "Maximas", isParallel());
        localMaxima.initMaxima(this, minValue, maxValue);
        return localMaxima;
    }

    public BooleanMask getLocal1DMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "1DMaximas", isParallel());
        localMaxima.init1DMaxima(this, minValue, maxValue);
        return localMaxima;
    }

    public FloatMask getDistanceFieldForRange(T minValue, T maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }
}
