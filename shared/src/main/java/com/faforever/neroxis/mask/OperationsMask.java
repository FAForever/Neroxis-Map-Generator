package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.functional.BiIntFunction;
import com.faforever.neroxis.util.vector.Vector2;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class OperationsMask<T, U extends OperationsMask<T, U>> extends Mask<T, U> {

    protected OperationsMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    protected OperationsMask(U other, String name) {
        super(other, name);
    }

    public abstract T getSum();

    /**
     * Add mask {@code other} to this mask on a pixel by pixel basis.
     * Masks must be the same size and concrete type
     *
     * @param other mask to add to this mask
     * @return the modified mask
     */
    @GraphMethod
    public U add(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            add(source::get);
        }, other);
    }

    protected U add(BiIntFunction<T> valueFunction) {
        return apply((x, y) -> addValueAt(x, y, valueFunction.apply(x, y)));
    }

    protected abstract void addValueAt(int x, int y, T value);

    /**
     * Add {@code value} wherever the {@link BooleanMask} {@code other} is true
     * Masks must be the same size and concrete type
     *
     * @param other the {@link BooleanMask} that determines where to add {@code value}
     * @param value the value to add to the mask
     * @return the modified mask
     */
    @GraphMethod
    public U add(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addValueAt(x, y, value);
                }
            });
        }, other);
    }

    /**
     * Add {@code values} on a pixel basis only where {@code other} is true.
     * Masks must be the same size and concrete type
     *
     * @param other  the {@link BooleanMask} that determines which pixels to add
     * @param values the mask containing the values to add
     * @return the modified mask
     */
    @GraphMethod
    public U add(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addValueAt(x, y, vals.get(x, y));
                }
            });
        }, other, values);
    }

    /**
     * Add {@code val} to every pixel
     *
     * @param val the value to add
     * @return the modified mask
     */
    @GraphMethod
    public U add(T val) {
        return add((x, y) -> val);
    }

    public U addWithOffset(U other, Vector2 offset, boolean centered, boolean wrapEdges) {
        return addWithOffset(other, (int) offset.getX(), (int) offset.getY(), centered, wrapEdges);
    }

    public U addWithOffset(U other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::addValueAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    /**
     * Subtract the average of the mask
     *
     * @return the modified mask
     */
    @GraphMethod
    public U subtractAvg() {
        return enqueue(() -> subtract(getAvg()));
    }

    /**
     * Subtract the given value from the mask
     *
     * @param val value to subtract
     * @return the modified mask
     */
    @GraphMethod
    public U subtract(T val) {
        return subtract((x, y) -> val);
    }

    protected U subtract(BiIntFunction<T> valueFunction) {
        return apply((x, y) -> subtractValueAt(x, y, valueFunction.apply(x, y)));
    }

    protected abstract void subtractValueAt(int x, int y, T value);

    public abstract T getAvg();

    /**
     * Subtract the given mask {@code other}.
     * Masks must be the same size and concrete type
     *
     * @param other mask to subtract
     * @return the modified mask
     */
    @GraphMethod
    public U subtract(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            subtract(source::get);
        }, other);
    }

    /**
     * Subtract the given {@code value} where the {@link BooleanMask} other
     * is true. Masks must be the same size and concrete type
     *
     * @param other Mask determining which pixels to subtract
     * @param value Value to subtract
     * @return the modified mask
     */
    @GraphMethod
    public U subtract(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, value);
                }
            });
        }, other);
    }

    /**
     *
     */
    @GraphMethod
    public U subtract(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, vals.get(x, y));
                }
            });
        }, other, values);
    }

    public U subtractWithOffset(U other, Vector2 offset, boolean centered, boolean wrapEdges) {
        return subtractWithOffset(other, (int) offset.getX(), (int) offset.getY(), centered, wrapEdges);
    }

    @GraphMethod
    public U subtractWithOffset(U other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::subtractValueAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @GraphMethod
    public U multiply(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            multiply(source::get);
        }, other);
    }

    protected U multiply(BiIntFunction<T> valueFunction) {
        return apply((x, y) -> multiplyValueAt(x, y, valueFunction.apply(x, y)));
    }

    protected abstract void multiplyValueAt(int x, int y, T value);

    @GraphMethod
    public U multiply(T val) {
        return multiply((x, y) -> val);
    }

    @GraphMethod
    public U multiply(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, value);
                }
            });
        }, other);
    }

    @GraphMethod
    public U multiply(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, vals.get(x, y));
                }
            });
        }, other, values);
    }

    public U multiplyWithOffset(U other, Vector2 offset, boolean centered, boolean wrapEdges) {
        return multiplyWithOffset(other, (int) offset.getX(), (int) offset.getY(), centered, wrapEdges);
    }

    @GraphMethod
    public U multiplyWithOffset(U other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::multiplyValueAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @GraphMethod
    public U divide(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            divide(source::get);
        }, other);
    }

    protected U divide(BiIntFunction<T> valueFunction) {
        return apply((x, y) -> divideValueAt(x, y, valueFunction.apply(x, y)));
    }

    protected abstract void divideValueAt(int x, int y, T value);

    @GraphMethod
    public U divide(T val) {
        return divide((x, y) -> val);
    }

    @GraphMethod
    public U divide(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, value);
                }
            });
        }, other);
    }

    @GraphMethod
    public U divide(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractValueAt(x, y, vals.get(x, y));
                }
            });
        }, other, values);
    }

    public U divideWithOffset(U other, Vector2 offset, boolean centered, boolean wrapEdges) {
        return divideWithOffset(other, (int) offset.getX(), (int) offset.getY(), centered, wrapEdges);
    }

    @GraphMethod
    public U divideWithOffset(U other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::divideValueAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    protected U addWithSymmetry(SymmetryType symmetryType, BiIntFunction<T> valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addValueAt(sx, sy, value));
        });
    }

    protected U subtractWithSymmetry(SymmetryType symmetryType, BiIntFunction<T> valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractValueAt(sx, sy, value));
        });
    }

    protected U multiplyWithSymmetry(SymmetryType symmetryType, BiIntFunction<T> valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyValueAt(sx, sy, value));
        });
    }

    protected U divideWithSymmetry(SymmetryType symmetryType, BiIntFunction<T> valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideValueAt(sx, sy, value));
        });
    }

    protected void calculateScalarInnerValue(int[][] innerCount, int x, int y, int val) {
        innerCount[x][y] = val;
        innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
        innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
        innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
    }
}
