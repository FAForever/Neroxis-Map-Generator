package map;

import lombok.Getter;
import util.Pipeline;
import util.Util;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public strictfp class ConcurrentFloatMask extends ConcurrentMask {

    private final String name;
    private FloatMask floatMask;

    public ConcurrentFloatMask(int size, Long seed, SymmetryHierarchy symmetryHierarchy, String name) {
        this.floatMask = new FloatMask(size, seed, symmetryHierarchy);
        this.name = name;
        this.symmetryHierarchy = this.floatMask.getSymmetryHierarchy();

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentFloatMask(ConcurrentFloatMask mask, Long seed, String name) {
        this.name = name;
        this.floatMask = new FloatMask(mask.getSize(), seed, mask.getSymmetryHierarchy());

        if (name.equals("mocked")) {
            this.floatMask = new FloatMask(mask.getFloatMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.floatMask.add(new FloatMask(((ConcurrentFloatMask) res.get(0)).getFloatMask(), this.floatMask.getRandom().nextLong())));
        }
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentFloatMask init(ConcurrentBinaryMask other, float low, float high) {
        return Pipeline.add(this, Arrays.asList(this, other), res -> this.floatMask.init(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), low, high)
        );
    }

    public ConcurrentFloatMask copy() {
        return new ConcurrentFloatMask(this, this.floatMask.getRandom().nextLong(), name + "Copy");
    }

    public ConcurrentFloatMask add(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.add(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask add(ConcurrentBinaryMask other, float value) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.add(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask subtract(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.subtract(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask subtract(ConcurrentBinaryMask other, float value) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.subtract(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask multiply(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.multiply(value)
        );
    }

    public ConcurrentFloatMask clampMax(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.clampMax(value)
        );
    }

    public ConcurrentFloatMask clampMin(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.clampMin(value)
        );
    }

    public ConcurrentFloatMask maskToHills(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.maskToHills(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask maskToMountains(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.maskToMountains(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask maskToHeightmap(float underWaterSlope, int maxRepeat, ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.maskToHeightmap(underWaterSlope, maxRepeat, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask max(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.max(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask smooth(int radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.smooth(radius)
        );
    }

    public ConcurrentFloatMask smooth(int radius, ConcurrentBinaryMask limiter) {
        return Pipeline.add(this, Arrays.asList(this, limiter), res ->
                this.floatMask.smooth(radius, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask gradient() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.gradient()
        );
    }

    @Override
    public void writeToFile(Path path) {
        floatMask.writeToFile(path);
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        return floatMask.toHash();
    }

    protected FloatMask getFloatMask() {
        return floatMask;
    }

    public FloatMask getFinalMask() {
        Pipeline.await(this);
        return floatMask.copy();
    }

    @Override
    public ConcurrentFloatMask mockClone() {
        return new ConcurrentFloatMask(this, 0L, "mocked");
    }

    @Override
    int getSize() {
        return floatMask.getSize();
    }

    public void show() {
        this.floatMask.show();
    }

    public void startVisualDebugger(String maskName) {
        this.floatMask.startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }
}
