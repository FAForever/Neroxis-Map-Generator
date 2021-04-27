package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector2;

import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public strictfp abstract class OperationsMask<T, U extends OperationsMask<T, U>> extends Mask<T, U> {

    protected OperationsMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    protected OperationsMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public OperationsMask(U other, Long seed) {
        super(other, seed);
    }

    public OperationsMask(U other, Long seed, String name) {
        super(other, seed, name);
    }

    public abstract T getAvg();

    public abstract void addValueAt(int x, int y, T value);

    public abstract void subtractValueAt(int x, int y, T value);

    public abstract void multiplyValueAt(int x, int y, T value);

    public abstract void divideValueAt(int x, int y, T value);

    public abstract T getSum();

    public U add(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            add(source::get);
        }, other);
        return (U) this;
    }

    public U add(BooleanMask other, T value) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            add((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
        return (U) this;
    }

    public U add(T val) {
        enqueue(() -> {
            add((x, y) -> val);
        });
        return (U) this;
    }

    public U addWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return addWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U addWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::addValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
        return (U) this;
    }

    public U subtractAvg() {
        enqueue(() -> subtract(getAvg()));
        return (U) this;
    }

    public U subtract(T val) {
        enqueue(() -> subtract((x, y) -> val));
        return (U) this;
    }

    public U subtract(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            subtract(source::get);
        }, other);
        return (U) this;
    }

    public U subtract(BooleanMask other, T value) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            subtract((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
        return (U) this;
    }

    public U subtractWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return subtractWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U subtractWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::subtractValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
        return (U) this;
    }

    public U multiply(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            multiply(source::get);
        }, other);
        return (U) this;
    }

    public U multiply(T val) {
        enqueue(() -> {
            multiply((x, y) -> val);
        });
        return (U) this;
    }

    public U multiply(BooleanMask other, T value) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            multiply((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
        return (U) this;
    }

    public U multiplyWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return multiplyWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U multiplyWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::multiplyValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
        return (U) this;
    }

    public U divide(U other) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            assertCompatibleMask(source);
            divide(source::get);
        }, other);
        return (U) this;
    }

    public U divide(T val) {
        enqueue(() -> {
            divide((x, y) -> val);
        });
        return (U) this;
    }

    public U divide(BooleanMask other, T value) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            divide((x, y) -> source.get(x, y) ? value : getZeroValue());
        }, other);
        return (U) this;
    }

    public U divideWithOffset(U other, Vector2 loc, boolean centered, boolean wrapEdges) {
        return divideWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public U divideWithOffset(U other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        enqueue(dependencies -> {
            U source = (U) dependencies.get(0);
            applyWithOffset(source, this::divideValueAt, xCoordinate, yCoordinate, center, wrapEdges);
        }, other);
        return (U) this;
    }

    public void add(BiFunction<Integer, Integer, T> valueFunction) {
        apply((x, y) -> addValueAt(x, y, valueFunction.apply(x, y)));
    }

    public void addWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addValueAt(sx, sy, value));
        });
    }

    public void subtract(BiFunction<Integer, Integer, T> valueFunction) {
        apply((x, y) -> subtractValueAt(x, y, valueFunction.apply(x, y)));
    }

    public void subtractWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractValueAt(sx, sy, value));
        });
    }

    public void multiply(BiFunction<Integer, Integer, T> valueFunction) {
        apply((x, y) -> multiplyValueAt(x, y, valueFunction.apply(x, y)));
    }

    public void multiplyWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyValueAt(sx, sy, value));
        });
    }

    public void divide(BiFunction<Integer, Integer, T> valueFunction) {
        apply((x, y) -> divideValueAt(x, y, valueFunction.apply(x, y)));
    }

    public void divideWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideValueAt(sx, sy, value));
        });
    }
}
