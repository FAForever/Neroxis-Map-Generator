package com.faforever.neroxis.map;

import com.faforever.neroxis.util.VisualDebugger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public strictfp class IntegerMask extends NumberMask<Integer, IntegerMask> {

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(seed, symmetrySettings, name, parallel);
        this.mask = getEmptyMask(size);
        this.plannedSize = size;
        enqueue(() -> VisualDebugger.visualizeMask(this));
    }

    public IntegerMask(IntegerMask sourceMask, Long seed) {
        this(sourceMask, seed, null);
    }

    public IntegerMask(IntegerMask other, Long seed, String name) {
        super(seed, other.getSymmetrySettings(), name, other.isParallel());
        this.mask = getEmptyMask(other.getSize());
        this.plannedSize = other.getSize();
        setProcessing(other.isProcessing());
        enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            modify(source::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, other);
    }

    @Override
    protected Integer[][] getEmptyMask(int size) {
        Integer[][] empty = new Integer[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                empty[x][y] = 0;
            }
        }
        return empty;
    }

    @Override
    public IntegerMask interpolate() {
        return blur(1);
    }

    public IntegerMask blur(int radius) {
        enqueue(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> StrictMath.round(calculateAreaAverage(radius, x, y, innerCount)));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public IntegerMask blur(int radius, BooleanMask limiter) {
        enqueue(() -> {
            assertCompatibleMask(limiter);
            int[][] innerCount = getInnerCount();
            modify((x, y) -> StrictMath.round(limiter.getValueAt(x, y) ? calculateAreaAverage(radius, x, y, innerCount) : getValueAt(x, y)));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    @Override
    public IntegerMask copy() {
        if (random != null) {
            return new IntegerMask(this, random.nextLong());
        } else {
            return new IntegerMask(this, null);
        }
    }

    @Override
    public Integer getDefaultValue() {
        return 0;
    }

    @Override
    public Integer add(Integer val1, Integer val2) {
        return val1 + val2;
    }

    @Override
    public Integer subtract(Integer val1, Integer val2) {
        return val1 - val2;
    }

    @Override
    public Integer multiply(Integer val1, Integer val2) {
        return val1 * val2;
    }

    @Override
    public Integer divide(Integer val1, Integer val2) {
        return val1 / val2;
    }

    @Override
    public Integer getAvg() {
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, getValueAt(x, y)));
        return innerCount;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.putInt(getValueAt(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
