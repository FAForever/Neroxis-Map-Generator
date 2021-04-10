package neroxis.map;

import lombok.Getter;
import neroxis.util.Pipeline;
import neroxis.util.Vector2f;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public strictfp class ConcurrentFloatMask extends ConcurrentMask<FloatMask> {

    public ConcurrentFloatMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        super(seed, name, size);
        this.mask = new FloatMask(size, seed, symmetrySettings);
        this.symmetrySettings = this.mask.getSymmetrySettings();

        Pipeline.add(this, Collections.emptyList(), res -> this.mask);
    }

    public ConcurrentFloatMask(ConcurrentFloatMask mask, Long seed, String name) {
        super(seed, name, mask.getPlannedSize());
        this.mask = new FloatMask(mask.getPlannedSize(), seed, mask.getSymmetrySettings());

        if (name.equals("mocked")) {
            this.mask = new FloatMask(mask.getFloatMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.mask.add(new FloatMask(((ConcurrentFloatMask) res.get(0)).getFloatMask(), this.mask.getRandom().nextLong())));
        }
        this.symmetrySettings = mask.getSymmetrySettings();
    }

    public ConcurrentFloatMask(ConcurrentBinaryMask mask, float low, float high, Long seed, String name) {
        super(seed, name, mask.getPlannedSize());
        this.mask = new FloatMask(mask.getPlannedSize(), seed, mask.getSymmetrySettings());

        if (name.equals("mocked")) {
            this.mask = new FloatMask(mask.getBinaryMask(), low, high, seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.mask.add(new FloatMask(((ConcurrentBinaryMask) res.get(0)).getBinaryMask(), low, high, this.mask.getRandom().nextLong())));
        }
        this.symmetrySettings = mask.getSymmetrySettings();
    }

    public ConcurrentFloatMask init(ConcurrentBinaryMask other, float low, float high) {
        plannedSize = other.getPlannedSize();
        return Pipeline.add(this, Arrays.asList(this, other), res -> this.mask.init(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), low, high)
        );
    }

    public ConcurrentFloatMask init(ConcurrentFloatMask other) {
        plannedSize = other.getPlannedSize();
        return Pipeline.add(this, Arrays.asList(this, other), res -> this.mask.init(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask copy() {
        return new ConcurrentFloatMask(this, this.mask.getRandom().nextLong(), name + "Copy");
    }

    public ConcurrentFloatMask resample(int size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.resample(size)
        );
    }

    public ConcurrentFloatMask add(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.add(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask add(ConcurrentBinaryMask other, float value) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.add(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask add(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.add(value)
        );
    }

    public ConcurrentFloatMask subtract(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.subtract(value)
        );
    }

    public ConcurrentFloatMask subtractAvg() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.subtractAvg()
        );
    }

    public ConcurrentFloatMask addGaussianNoise(float scale) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.addGaussianNoise(scale)
        );
    }

    public ConcurrentFloatMask addWhiteNoise(float scale) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.addWhiteNoise(scale)
        );
    }

    public ConcurrentFloatMask subtract(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.subtract(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask subtract(ConcurrentBinaryMask other, float value) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.subtract(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask multiply(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.multiply(value)
        );
    }

    public ConcurrentFloatMask setToValue(float value, ConcurrentBinaryMask area) {
        return Pipeline.add(this, Arrays.asList(this, area), res ->
                this.mask.setToValue(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask clampMax(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.clampMax(value)
        );
    }

    public ConcurrentFloatMask clampMax(float value, ConcurrentBinaryMask area) {
        return Pipeline.add(this, Arrays.asList(this, area), res ->
                this.mask.clampMax(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask clampMin(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.clampMin(value)
        );
    }

    public ConcurrentFloatMask clampMin(float value, ConcurrentBinaryMask area) {
        return Pipeline.add(this, Arrays.asList(this, area), res ->
                this.mask.clampMin(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), value)
        );
    }

    public ConcurrentFloatMask threshold(float value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.threshold(value)
        );
    }

    public ConcurrentFloatMask useBrush(Vector2f location, String brushName, float intensity, int size, boolean wrapEdges) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.useBrush(location, brushName, intensity, size, wrapEdges)
        );
    }

    public ConcurrentFloatMask useBrushWithinArea(ConcurrentBinaryMask area, String brushName, int size, int frequency, float intensity, boolean wrapEdges) {
        return Pipeline.add(this, Arrays.asList(this, area), res ->
                this.mask.useBrushWithinArea(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), brushName, size, frequency, intensity, wrapEdges)
        );
    }

    public ConcurrentFloatMask useBrushWithinAreaWithDensity(ConcurrentBinaryMask area, String brushName, int size, float density, float intensity, boolean wrapEdges) {
        return Pipeline.add(this, Arrays.asList(this, area), res ->
                this.mask.useBrushWithinAreaWithDensity(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), brushName, size, density, intensity, wrapEdges)
        );
    }

    public ConcurrentFloatMask addDistance(ConcurrentBinaryMask other, float scale) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.addDistance(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), scale)
        );
    }

    public ConcurrentFloatMask max(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.max(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask smooth(int radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.blur(radius)
        );
    }

    public ConcurrentFloatMask smooth(int radius, ConcurrentBinaryMask limiter) {
        return Pipeline.add(this, Arrays.asList(this, limiter), res ->
                this.mask.smooth(radius, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask gradient() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.gradient()
        );
    }

    public ConcurrentFloatMask supcomGradient() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.supcomGradient()
        );
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        return mask.toHash();
    }

    protected FloatMask getFloatMask() {
        return mask;
    }

    public FloatMask getFinalMask() {
        Pipeline.await(this);
        return mask.copy();
    }

    public ConcurrentFloatMask mockClone() {
        return new ConcurrentFloatMask(this, 0L, "mocked");
    }

    public void show() {
        this.mask.show();
    }
}
