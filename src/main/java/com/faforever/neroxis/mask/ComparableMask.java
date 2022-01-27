package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.ui.GraphMethod;

import java.awt.*;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class ComparableMask<T extends Comparable<T>, U extends ComparableMask<T, U>> extends OperationsMask<T, U> {

    protected ComparableMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    protected ComparableMask(U other, String name) {
        super(other, name);
    }

    public boolean valueAtEqualTo(Point point, T value) {
        return valueAtEqualTo(point.x, point.y, value);
    }

    public boolean valueAtEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) == 0;
    }

    public boolean valueAtLessThan(Point point, T value) {
        return valueAtLessThan(point.x, point.y, value);
    }

    public boolean valueAtLessThan(int x, int y, T value) {
        return get(x, y).compareTo(value) < 0;
    }

    public boolean valueAtLessThanEqualTo(Point point, T value) {
        return valueAtLessThanEqualTo(point.x, point.y, value);
    }

    public boolean valueAtLessThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) <= 0;
    }

    public boolean valueAtGreaterThan(Point point, T value) {
        return valueAtGreaterThan(point.x, point.y, value);
    }

    public boolean valueAtGreaterThan(int x, int y, T value) {
        return get(x, y).compareTo(value) > 0;
    }

    public boolean valueAtGreaterThanEqualTo(Point point, T value) {
        return valueAtGreaterThanEqualTo(point.x, point.y, value);
    }

    public boolean valueAtGreaterThanEqualTo(int x, int y, T value) {
        return get(x, y).compareTo(value) >= 0;
    }

    public boolean isLocalMax(Point point) {
        return isLocalMax(point.x, point.y);
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

    public boolean isLocal1DMax(Point point) {
        return isLocal1DMax(point.x, point.y);
    }

    public boolean isLocal1DMax(int x, int y) {
        T value = get(x, y);
        return (((x > 0 && valueAtLessThanEqualTo(x - 1, y, value))
                && (x < getSize() - 1 && valueAtLessThanEqualTo(x + 1, y, value)))
                || ((y > 0 && valueAtLessThanEqualTo(x, y - 1, value))
                && (y < getSize() - 1 && valueAtLessThanEqualTo(x, y + 1, value))));
    }

    public abstract T getMin();

    public abstract T getMax();

    @GraphMethod
    public U max(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            set(point -> {
                T thisVal = get(point);
                T otherVal = source.get(point);
                return thisVal.compareTo(otherVal) > 0 ? thisVal : otherVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMax(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set(point -> {
                T thisVal = get(point);
                return source.get(point) ? (thisVal.compareTo(val) < 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMax(T val) {
        return enqueue(() -> set(point -> {
            T thisVal = get(point);
            return thisVal.compareTo(val) < 0 ? thisVal : val;
        }));
    }

    @GraphMethod
    public U min(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            set(point -> {
                T thisVal = get(point);
                T otherVal = source.get(point);
                return thisVal.compareTo(otherVal) < 0 ? thisVal : otherVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMin(BooleanMask other, T val) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set(point -> {
                T thisVal = get(point);
                return source.get(point) ? (thisVal.compareTo(val) > 0 ? val : thisVal) : thisVal;
            });
        }, other);
    }

    @GraphMethod
    public U clampMin(T val) {
        return enqueue(() -> set(point -> {
            T thisVal = get(point);
            return thisVal.compareTo(val) > 0 ? thisVal : val;
        }));
    }

    @GraphMethod
    public U threshold(T val) {
        return enqueue(() -> set(point -> {
            T thisVal = get(point);
            return thisVal.compareTo(val) > 0 ? getZeroValue() : get(point);
        }));
    }

    @GraphMethod
    public U zeroOutsideRange(T min, T max) {
        return enqueue(() -> set(point -> valueAtLessThan(point, min) || valueAtGreaterThan(point, max) ? getZeroValue() : get(point)));
    }

    @GraphMethod
    public U zeroInRange(T min, T max) {
        return enqueue(() -> set(point -> valueAtGreaterThanEqualTo(point, min) && valueAtLessThan(point, max) ? getZeroValue() : get(point)));
    }

    @GraphMethod
    public BooleanMask convertToBooleanMask(T minValue) {
        return new BooleanMask(this, minValue, getName() + "toBoolean");
    }

    @GraphMethod
    public BooleanMask convertToBooleanMask(T minValue, T maxValue) {
        return new BooleanMask(this, minValue, maxValue, getName() + "toBoolean");
    }

    @GraphMethod
    public BooleanMask getLocalMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "Maximas", isParallel());
        return localMaxima.initMaxima(this, minValue, maxValue);
    }

    @GraphMethod
    public BooleanMask getLocal1DMaximums(T minValue, T maxValue) {
        BooleanMask localMaxima = new BooleanMask(getSize(), getNextSeed(), symmetrySettings, getName() + "1DMaximas", isParallel());
        return localMaxima.init1DMaxima(this, minValue, maxValue);
    }

    @GraphMethod
    public FloatMask getDistanceFieldForRange(T minValue, T maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }
}
