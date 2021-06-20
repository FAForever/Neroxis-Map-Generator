package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector2;

import java.util.function.BiFunction;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class OperationsMask<T, U extends OperationsMask<T, U>> extends Mask<T, U> {

    protected OperationsMask(Class<T> objectClass, int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(objectClass, size, seed, symmetrySettings, name, parallel);
    }

    public OperationsMask(U other, Long seed, String name) {
        super(other, seed, name);
    }

    public abstract T getAvg();

    protected abstract void addValueAt(int x, int y, T value);

    protected abstract void subtractValueAt(int x, int y, T value);

    protected abstract void multiplyValueAt(int x, int y, T value);

    protected abstract void divideValueAt(int x, int y, T value);

    public abstract T getSum();

    public U add(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            add(source::get);
        }, other);
    }

    public U add(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            add((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
    }

    public U add(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            add((x, y) -> source.get(x, y) ? vals.get(x, y) : getZeroValue());
        }, other, values);
    }

    public U add(T val) {
        return add((x, y) -> val);
    }

    public U addWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return addWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U addWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::addValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
    }

    public U subtractAvg() {
        return enqueue(() -> subtract(getAvg()));
    }

    public U subtract(T val) {
        return enqueue(() -> subtract((x, y) -> val));
    }

    public U subtract(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            subtract(source::get);
        }, other);
    }

    public U subtract(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            subtract((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
    }

    public U subtract(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            subtract((x, y) -> source.get(x, y) ? vals.get(x, y) : getZeroValue());
        }, other, values);
    }

    public U subtractWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return subtractWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U subtractWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::subtractValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
    }

    public U multiply(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            multiply(source::get);
        }, other);
    }

    public U multiply(T val) {
        return enqueue(() -> multiply((x, y) -> val));
    }

    public U multiply(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            multiply((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
    }

    public U multiply(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            multiply((x, y) -> source.get(x, y) ? vals.get(x, y) : getZeroValue());
        }, other, values);
    }

    public U multiplyWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return multiplyWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U multiplyWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::multiplyValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
    }

    public U divide(U other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            divide(source::get);
        }, other);
    }

    public U divide(T val) {
        return enqueue(() -> divide((x, y) -> val));
    }

    public U divide(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            divide((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
    }

    public U divide(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            divide((x, y) -> source.get(x, y) ? vals.get(x, y) : getZeroValue());
        }, other, values);
    }

    public U divideWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return divideWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U divideWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::divideValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
    }

    public U add(BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> apply((x, y) -> addValueAt(x, y, valueFunction.apply(x, y))));
    }

    public U addWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addValueAt(sx, sy, value));
        }));
    }

    public U subtract(BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> apply((x, y) -> subtractValueAt(x, y, valueFunction.apply(x, y))));
    }

    public U subtractWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractValueAt(sx, sy, value));
        }));
    }

    public U multiply(BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> apply((x, y) -> multiplyValueAt(x, y, valueFunction.apply(x, y))));
    }

    public U multiplyWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyValueAt(sx, sy, value));
        }));
    }

    public U divide(BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> apply((x, y) -> divideValueAt(x, y, valueFunction.apply(x, y))));
    }

    public U divideWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideValueAt(sx, sy, value));
        }));
    }

    protected void calculateScalarInnerValue(int[][] innerCount, int x, int y, int val) {
        innerCount[x][y] = val;
        innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
        innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
        innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
    }
}
