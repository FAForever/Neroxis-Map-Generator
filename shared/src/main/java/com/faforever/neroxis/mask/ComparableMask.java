package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.map.SymmetrySettings;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class ComparableMask<T extends Comparable<T>, U extends ComparableMask<T, U>> extends OperationsMask<T, U> {

    protected ComparableMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    protected ComparableMask(U other, String name) {
        super(other, name);
    }

    protected boolean valueAtEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) == 0;
    }

    protected boolean valueAtLessThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) <= 0;
    }

    protected boolean isLocalMax(int x, int y) {
        T value = get(x, y);
        return ((x > 0 && valueAtLessThanEqualTo(x - 1, y, value))
                && (x < getSize() - 1 && valueAtLessThanEqualTo(x + 1, y, value))
                && (y > 0 && valueAtLessThanEqualTo(x, y - 1, value))
                && (y < getSize() - 1
                    && valueAtLessThanEqualTo(x, y
                                                 + 1, value))
                && valueAtLessThanEqualTo(x - 1, y - 1, value)
                && valueAtLessThanEqualTo(x + 1, y - 1, value)
                && valueAtLessThanEqualTo(x - 1, y + 1, value)
                && valueAtLessThanEqualTo(x + 1, y + 1, value));
    }

    protected boolean isLocal1DMax(int x, int y) {
        T value = get(x, y);
        return (((x > 0 && valueAtLessThanEqualTo(x - 1, y, value)) && (x < getSize() - 1 && valueAtLessThanEqualTo(
                x + 1, y, value))) || ((y > 0 && valueAtLessThanEqualTo(x, y - 1, value)) && (y < getSize() - 1
                                                                                              && valueAtLessThanEqualTo(
                x, y + 1, value))));
    }

    public abstract T getMin();

    public abstract T getMax();

    @GraphMethod
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

    @GraphMethod
    public U clampMax(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.getPrimitive(x, y) ? (thisVal.compareTo(val) < 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMax(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) < 0 ? thisVal : val;
        });
    }

    @GraphMethod
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

    @GraphMethod
    public U clampMin(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set((x, y) -> {
                T thisVal = get(x, y);
                return source.getPrimitive(x, y) ? (thisVal.compareTo(val) > 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMin(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? thisVal : val;
        });
    }

    @GraphMethod
    public U threshold(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? getZeroValue() : thisVal;
        });
    }

    @GraphMethod
    public U zeroOutsideRange(T min, T max) {
        return set((x, y) -> valueAtLessThan(x, y, min) || valueAtGreaterThan(x, y, max) ? getZeroValue() : get(x, y));
    }

    protected boolean valueAtLessThan(int x, int y, T value) {
        return get(x, y).compareTo(value) < 0;
    }

    protected boolean valueAtGreaterThan(int x, int y, T value) {
        return get(x, y).compareTo(value) > 0;
    }

    @GraphMethod
    public U zeroInRange(T min, T max) {
        return set(
                (x, y) -> valueAtGreaterThanEqualTo(x, y, min) && valueAtLessThan(x, y, max) ? getZeroValue() : get(x,
                                                                                                                    y));
    }

    protected boolean valueAtGreaterThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) >= 0;
    }

    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsBooleanMask(T minValue) {
        return copyAsBooleanMask(minValue, getName() + "toBoolean");
    }

    public BooleanMask copyAsBooleanMask(T minValue, String name) {
        return new BooleanMask(this, minValue, name);
    }

    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocalMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "Maximas",
                                                  isParallel());
        return localMaxima.initMaxima(this, minValue, maxValue);
    }

    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocal1DMaximums(T minValue, T maxValue) {
        return copyAsLocal1DMaximums(minValue, maxValue, getName() + "1DMaximas");
    }

    public BooleanMask copyAsLocal1DMaximums(T minValue, T maxValue, String name) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, name, isParallel());
        return localMaxima.init1DMaxima(this, minValue, maxValue);
    }

    @GraphMethod(returnsSelf = false)
    public FloatMask copyAsDistanceFieldForRange(T minValue, T maxValue) {
        return copyAsDistanceFieldForRange(minValue, maxValue, getName() + "DistanceField");
    }

    public FloatMask copyAsDistanceFieldForRange(T minValue, T maxValue, String name) {
        return copyAsBooleanMask(minValue, maxValue).copyAsDistanceField();
    }

    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsBooleanMask(T minValue, T maxValue) {
        return copyAsBooleanMask(minValue, maxValue, getName() + "toBoolean");
    }

    public BooleanMask copyAsBooleanMask(T minValue, T maxValue, String name) {
        return new BooleanMask(this, minValue, maxValue, name);
    }
}
