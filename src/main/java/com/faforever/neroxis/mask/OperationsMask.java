package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.*;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class OperationsMask<T, U extends OperationsMask<T, U>> extends Mask<T, U> {

    protected OperationsMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public OperationsMask(U other, String name) {
        super(other, name);
    }

    public abstract T getAvg();

    protected void addValueAt(Point point, T value) {
        addValueAt(point.x, point.y, value);
    }

    protected abstract void addValueAt(int x, int y, T value);

    protected void subtractValueAt(Point point, T value) {
        subtractValueAt(point.x, point.y, value);
    }

    protected abstract void subtractValueAt(int x, int y, T value);

    protected void multiplyValueAt(Point point, T value) {
        multiplyValueAt(point.x, point.y, value);
    }

    protected abstract void multiplyValueAt(int x, int y, T value);

    protected void divideValueAt(Point point, T value) {
        divideValueAt(point.x, point.y, value);
    }

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
            add(point -> source.get(point) ? value : getZeroValue());
        }, other);
    }

    public U add(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            add(point -> source.get(point) ? vals.get(point) : getZeroValue());
        }, other, values);
    }

    public U add(T val) {
        return add(point -> val);
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
        return enqueue(() -> subtract(point -> val));
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
            subtract(point -> source.get(point) ? value : getZeroValue());
        }, other);
    }

    public U subtract(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            subtract(point -> source.get(point) ? vals.get(point) : getZeroValue());
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
        return enqueue(() -> multiply(point -> val));
    }

    public U multiply(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            multiply(point -> source.get(point) ? value : getZeroValue());
        }, other);
    }

    public U multiply(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            multiply(point -> source.get(point) ? vals.get(point) : getZeroValue());
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
        return enqueue(() -> divide(point -> val));
    }

    public U divide(BooleanMask other, T value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            divide(point -> source.get(point) ? value : getZeroValue());
        }, other);
    }

    public U divide(BooleanMask other, U values) {
        assertCompatibleMask(other);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            U vals = (U) dependencies.get(1);
            divide(point -> source.get(point) ? vals.get(point) : getZeroValue());
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

    public U add(Function<Point, T> valueFunction) {
        return enqueue(() -> apply(point -> addValueAt(point, valueFunction.apply(point))));
    }

    public U addWithSymmetry(SymmetryType symmetryType, Function<Point, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, point -> {
            T value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, spoint -> addValueAt(spoint, value));
        }));
    }

    public U subtract(Function<Point, T> valueFunction) {
        return enqueue(() -> apply(point -> subtractValueAt(point, valueFunction.apply(point))));
    }

    public U subtractWithSymmetry(SymmetryType symmetryType, Function<Point, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, point -> {
            T value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, spoint -> subtractValueAt(spoint, value));
        }));
    }

    public U multiply(Function<Point, T> valueFunction) {
        return enqueue(() -> apply(point -> multiplyValueAt(point, valueFunction.apply(point))));
    }

    public U multiplyWithSymmetry(SymmetryType symmetryType, Function<Point, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, point -> {
            T value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, spoint -> multiplyValueAt(spoint, value));
        }));
    }

    public U divide(Function<Point, T> valueFunction) {
        return enqueue(() -> apply(point -> divideValueAt(point, valueFunction.apply(point))));
    }

    public U divideWithSymmetry(SymmetryType symmetryType, Function<Point, T> valueFunction) {
        return enqueue(() -> applyWithSymmetry(symmetryType, point -> {
            T value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, spoint -> divideValueAt(spoint, value));
        }));
    }

    protected void calculateScalarInnerValue(int[][] innerCount, int x, int y, int val) {
        innerCount[x][y] = val;
        innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
        innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
        innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
    }
}
