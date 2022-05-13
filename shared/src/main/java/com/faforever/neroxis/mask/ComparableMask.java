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
                && valueAtLessThanEqualTo(x - 1, y + 1, value) && valueAtLessThanEqualTo(x + 1, y + 1, value));
    }

    protected boolean isLocal1DMax(int x, int y) {
        T value = get(x, y);
        return (((x > 0 && valueAtLessThanEqualTo(x - 1, y, value)) && (x < getSize() - 1 && valueAtLessThanEqualTo(
                x + 1, y, value))) || ((y > 0 && valueAtLessThanEqualTo(x, y - 1, value)) && (y < getSize() - 1
                                                                                              && valueAtLessThanEqualTo(
                x, y + 1, value))));
    }

    protected boolean isLocalMin(int x, int y) {
        T value = get(x, y);
        return ((x > 0 && valueAtGreaterThanEqualTo(x - 1, y, value))
                && (x < getSize() - 1
                    && valueAtGreaterThanEqualTo(x + 1, y, value))
                && (y > 0 && valueAtGreaterThanEqualTo(x, y - 1, value))
                && (y < getSize() - 1 && valueAtGreaterThanEqualTo(x, y + 1, value))
                && valueAtGreaterThanEqualTo(x - 1, y - 1, value)
                && valueAtGreaterThanEqualTo(x + 1, y - 1, value)
                && valueAtGreaterThanEqualTo(x - 1, y + 1, value)
                && valueAtGreaterThanEqualTo(x + 1, y + 1, value));
    }

    protected boolean isLocal1DMin(int x, int y) {
        T value = get(x, y);
        return (((x > 0 && valueAtGreaterThanEqualTo(x - 1, y, value)) && (x < getSize() - 1
                                                                           && valueAtGreaterThanEqualTo(x + 1, y,
                                                                                                        value))) || ((y
                                                                                                                      > 0
                                                                                                                      && valueAtGreaterThanEqualTo(
                x, y - 1, value)) && (y < getSize() - 1 && valueAtGreaterThanEqualTo(x, y + 1, value))));
    }

    public abstract T getMin();

    public abstract T getMax();

    /**
     * Take the max value of this mask or {@code other} on a pixel basis.
     * Masks must be the same size and type
     *
     * @param other the mask to take the max with
     * @return the modified mask
     */
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

    /**
     * Where {@code other} is true ensure the value is not greater than {@code val}
     *
     * @param other the mask to determine where to clamp
     * @param val   value to use as the ceiling
     * @return the modified mask
     */
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

    /**
     * Ensure no value is greater than {@code val}
     *
     * @param val the value to clamp at
     * @return the modified mask
     */
    @GraphMethod
    public U clampMax(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) < 0 ? thisVal : val;
        });
    }

    /**
     * Take the min value of this mask or {@code other} on a pixel basis.
     * Masks must be the same size and type
     *
     * @param other the mask to take the min with
     * @return the modified mask
     */
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

    /**
     * Where {@code other} is true ensure the value is not less than {@code val}
     *
     * @param other the mask to determine where to clamp
     * @param val   value to use as the floor
     * @return the modified mask
     */
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

    /**
     * Ensure no value is less than {@code val}
     *
     * @param val the value to clamp at
     * @return the modified mask
     */
    @GraphMethod
    public U clampMin(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? thisVal : val;
        });
    }

    /**
     * Set all values below {@code val} to 0
     *
     * @param val the threshold
     * @return the modified mask
     */
    @GraphMethod
    public U threshold(T val) {
        return set((x, y) -> {
            T thisVal = get(x, y);
            return thisVal.compareTo(val) > 0 ? getZeroValue() : thisVal;
        });
    }

    /**
     * Set all values outside the range {@code [min, max]}  to 0
     *
     * @param min the minimum
     * @param max the maximum
     * @return the modified mask
     */
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

    /**
     * Set all values inside the range {@code [min, max]}  to 0
     *
     * @param min the minimum
     * @param max the maximum
     * @return the modified mask
     */
    @GraphMethod
    public U zeroInRange(T min, T max) {
        return set(
                (x, y) -> valueAtGreaterThanEqualTo(x, y, min) && valueAtLessThan(x, y, max) ? getZeroValue() : get(x,
                                                                                                                    y));
    }

    protected boolean valueAtGreaterThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) >= 0;
    }

    /**
     * Create a boolean mask where all values greater than {@code minValue} are true
     * and all values less than are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsBooleanMask(T minValue) {
        return copyAsBooleanMask(minValue, getName() + "toBoolean");
    }

    public BooleanMask copyAsBooleanMask(T minValue, String name) {
        return new BooleanMask(this, minValue, name);
    }

    /**
     * Create a boolean mask where all 2D maxima in range {@code [minValue, maxValue]} are true
     * and all others are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocalMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "Maximas",
                                                  isParallel());
        return localMaxima.initMaxima(this, minValue, maxValue);
    }

    /**
     * Create a boolean mask where all 1D maxima in range {@code [minValue, maxValue]} are true
     * and all others are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocal1DMaximums(T minValue, T maxValue) {
        return copyAsLocal1DMaximums(minValue, maxValue, getName() + "1DMaximas");
    }

    public BooleanMask copyAsLocal1DMaximums(T minValue, T maxValue, String name) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, name, isParallel());
        return localMaxima.init1DMaxima(this, minValue, maxValue);
    }

    /**
     * Create a boolean mask where all 2D maxima in range {@code [minValue, maxValue]} are true
     * and all others are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocalMinimums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "Minimas",
                                                  isParallel());
        return localMaxima.initMaxima(this, minValue, maxValue);
    }

    /**
     * Create a boolean mask where all 1D maxima in range {@code [minValue, maxValue]} are true
     * and all others are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsLocal1DMinimums(T minValue, T maxValue) {
        return copyAsLocal1DMaximums(minValue, maxValue, getName() + "1DMinimas");
    }

    public BooleanMask copyAsLocal1DMinimums(T minValue, T maxValue, String name) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, name, isParallel());
        return localMaxima.init1DMaxima(this, minValue, maxValue);
    }

    /**
     * Create a float mask where values inside the range {@code [minValue, maxValue]} are set to 0
     * and values outside that range are equal to the their distance to a value inside the range
     *
     * @param minValue the minimum to set a pixel to true
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public FloatMask copyAsDistanceFieldForRange(T minValue, T maxValue) {
        return copyAsDistanceFieldForRange(minValue, maxValue, getName() + "DistanceField");
    }

    public FloatMask copyAsDistanceFieldForRange(T minValue, T maxValue, String name) {
        return copyAsBooleanMask(minValue, maxValue).copyAsDistanceField();
    }

    /**
     * Create a boolean mask where all values in range {@code [minValue, maxValue]} are true
     * and all values outside are false
     *
     * @return the modified mask
     */
    @GraphMethod(returnsSelf = false)
    public BooleanMask copyAsBooleanMask(T minValue, T maxValue) {
        return copyAsBooleanMask(minValue, maxValue, getName() + "toBoolean");
    }

    public BooleanMask copyAsBooleanMask(T minValue, T maxValue, String name) {
        return new BooleanMask(this, minValue, maxValue, name);
    }
}
