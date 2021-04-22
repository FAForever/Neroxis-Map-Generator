package neroxis.map;

import neroxis.util.VisualDebugger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public strictfp class IntegerMask extends Mask<Integer, IntegerMask> {

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
        execute(() -> VisualDebugger.visualizeMask(this));
    }

    public IntegerMask(IntegerMask sourceMask, Long seed) {
        this(sourceMask, seed, null);
    }

    public IntegerMask(IntegerMask sourceMask, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify(sourceMask::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, sourceMask);
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
        execute(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> StrictMath.round(calculateAreaAverage(radius, x, y, innerCount)));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public IntegerMask blur(int radius, BooleanMask limiter) {
        execute(() -> {
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
