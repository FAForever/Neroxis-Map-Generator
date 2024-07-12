package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.functional.BiIntConsumer;
import com.faforever.neroxis.util.functional.BiIntFunction;
import com.faforever.neroxis.util.functional.BiIntObjConsumer;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.visualization.VisualDebugger;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.awt.image.BufferedImage;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract sealed class Mask<T, U extends Mask<T, U>> permits OperationsMask {
    private static final String MOCK_NAME = "Mock";
    private static final String COPY_NAME = "Copy";
    protected final Random random;
    @Getter
    private final String name;
    @Getter
    protected final SymmetrySettings symmetrySettings;
    private boolean immutable;
    private int plannedSize;
    @Getter
    @Setter
    private boolean parallel;
    @Getter
    @Setter
    private boolean visualDebug;
    private boolean visible;
    private boolean mock;
    @Setter
    private String visualName;

    protected Mask(U other, String name) {
        this(other.getSize(), (name != null && name.endsWith(MOCK_NAME)) ? null : other.getNextSeed(),
             other.getSymmetrySettings(), name, other.isParallel());
        init(other);
    }

    protected Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        this.symmetrySettings = symmetrySettings;
        this.name = name == null ? String.valueOf(hashCode()) : name;
        this.plannedSize = size;
        this.parallel = parallel;
        random = seed != null ? new Random(seed) : null;
        visible = true;
        initializeMask(size);
    }

    protected static int getShiftedValue(int val, int offset, int size, boolean wrapEdges) {
        return wrapEdges ? (val + offset + size) % size : val + offset;
    }

    protected static Map<Integer, Integer> getSymmetricScalingCoordinateMap(int currentSize, int scaledSize) {
        float scale = (float) currentSize / scaledSize;
        float halfScaledSize = scaledSize / 2f;
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < StrictMath.ceil(halfScaledSize); ++i) {
            int scaledI = (int) StrictMath.floor(i * scale);
            map.put(i, scaledI);
            map.put(scaledSize - 1 - i, currentSize - 1 - scaledI);
        }
        return map;
    }

    public U init(U other) {
        plannedSize = other.getSize();
        return copyFrom(other);
    }

    /**
     * Blurs the mask in place by using a square filter of twice
     * the given radius centered on a pixel
     *
     * @param radius half size of the square filter
     * @return the blurred mask
     */
    public abstract U blur(int radius);

    /**
     * Blurs the mask in place by using a square filter of twice
     * the given radius centered on a pixel. Only applies the filter
     * where {@code other} is true
     *
     * @param radius half size of the square filter
     * @param other  boolean mask indicating where to apply the filter
     * @return the blurred mask
     */
    public abstract U blur(int radius, BooleanMask other);

    protected abstract U copyFrom(U other);

    public boolean isMock() {
        return (name != null && name.endsWith(MOCK_NAME)) || mock;
    }

    public int getSize() {
        if (parallel && !Pipeline.isRunning()) {
            return plannedSize;
        } else {
            return getImmediateSize();
        }
    }

    /**
     * Scales the mask to tne given size.
     * Uses unfiltered sampling to scale the contents
     *
     * @param newSize size to scale the mask to
     * @return the scaled mask
     */
    public U setSize(int newSize) {
        int size = getSize();
        if (newSize != size) {
            plannedSize = newSize;
            return enqueue(() -> setSizeInternal(newSize));
        } else {
            return (U) this;
        }
    }

    protected abstract void initializeMask(int size);

    protected Long getNextSeed() {
        return random != null ? random.nextLong() : null;
    }

    protected abstract int getImmediateSize();

    public abstract BufferedImage writeToImage(BufferedImage image);

    public abstract BufferedImage toImage();

    public abstract String toHash() throws NoSuchAlgorithmException;

    public String getVisualName() {
        return visualName != null ? visualName : (name != null ? name : toString());
    }

    @Override
    public String toString() {
        if (name != null) {
            return String.format("Mask(name=%s,size=%d)", name, getSize());
        } else {
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        }
    }

    public T get(Vector3 location) {
        return get(StrictMath.round(location.getX()), StrictMath.round(location.getZ()));
    }

    protected abstract T get(int x, int y);

    protected void set(Vector3 location, T value) {
        set(StrictMath.round(location.getX()), StrictMath.round(location.getZ()), value);
    }

    protected abstract void set(int x, int y, T value);

    protected void set(Vector2 location, T value) {
        set(StrictMath.round(location.getX()), StrictMath.round(location.getY()), value);
    }

    @SneakyThrows
    public U immutableCopy() {
        Mask<?, U> copy = copy(getName() + MOCK_NAME);
        return copy.enqueue(copy::makeImmutable);
    }

    protected abstract U fill(T value);

    protected abstract T getZeroValue();

    public U set(BiIntFunction<T> valueFunction) {
        return apply((x, y) -> set(x, y, valueFunction.apply(x, y)));
    }

    public U apply(BiIntConsumer maskAction) {
        return enqueue(() -> loop(maskAction));
    }

    protected U enqueue(Runnable function) {
        return enqueue(ignored -> function.run());
    }

    private void makeImmutable() {
        immutable = true;
        mock = true;
    }

    /**
     * Set the mask to all zeros
     *
     * @return the cleared mask
     */
    public U clear() {
        return fill(getZeroValue());
    }

    protected U enqueue(Consumer<List<Mask<?, ?>>> function, Mask<?, ?>... usedMasks) {
        assertMutable();
        List<Mask<?, ?>> dependencies = Arrays.asList(usedMasks);
        if (parallel && !Pipeline.isRunning()) {
            if (dependencies.stream().anyMatch(dep -> !dep.parallel)) {
                throw new IllegalArgumentException("Non parallel masks used as dependents");
            }
            Pipeline.add(this, dependencies, function);
        } else {
            boolean visibleState = visible;
            visible = false;
            function.accept(dependencies);
            visible = visibleState;
            if (((DebugUtil.DEBUG && isVisualDebug()) || (DebugUtil.VISUALIZE && !isMock() && !isParallel())) &&
                visible) {
                String callingMethod = DebugUtil.getLastStackTraceMethodInPackage("com.faforever.neroxis.mask");
                String callingLine = DebugUtil.getLastStackTraceLineAfterPackage("com.faforever.neroxis.mask");
                VisualDebugger.visualizeMask(this, callingMethod, callingLine);
            }
        }
        return (U) this;
    }

    protected void assertMutable() {
        if (immutable) {
            throw new IllegalStateException("Mask is a mock and cannot be modified");
        }
    }

    protected abstract U setSizeInternal(int newSize);

    public U init(BooleanMask other, T falseValue, T trueValue) {
        plannedSize = other.getSize();
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            initializeMask(source.getSize());
            set((x, y) -> source.getPrimitive(x, y) ? trueValue : falseValue);
        }, other);
    }

    protected void loop(BiIntConsumer maskAction) {
        assertNotPipelined();
        int size = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                maskAction.accept(x, y);
            }
        }
    }

    protected void assertNotPipelined() {
        if (parallel && !Pipeline.isRunning()) {
            throw new IllegalStateException("Mask is pipelined and cannot return an immediate result");
        }
    }

    private Vector2 getRotatedPoint(float x, float y, float angle) {
        float halfSize = getSize() / 2f;
        float xOffset = x - halfSize;
        float yOffset = y - halfSize;
        double cosAngle = StrictMath.cos(angle);
        double sinAngle = StrictMath.sin(angle);
        float newX = (float) (xOffset * cosAngle - yOffset * sinAngle + halfSize);
        float newY = (float) (xOffset * sinAngle + yOffset * cosAngle + halfSize);
        return new Vector2(newX, newY);
    }

    public boolean inTeam(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM)
                && x < getMaxXBound(SymmetryType.TEAM)
                && y >= getMinYBound(x, SymmetryType.TEAM)
                && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse && inBounds(x, y);
    }

    protected int getMinYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 ->
                    getMinYFromXOnArc(
                            x, 360f / symmetry.getNumSymPoints());
            case DIAG, XZ -> x;
            default -> 0;
        };
    }

    /**
     * Set the mask to the given value where the {@code area} is true
     *
     * @param area  boolean mask indicating where to set the value to true
     * @param value value to set where area is true
     * @return the modified mask
     */
    public U setToValue(BooleanMask area, T value) {
        assertCompatibleMask(area);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    set(x, y, value);
                }
            });
        }, area);
    }

    /**
     * Copy the mask pixels to where {@code area} is true
     *
     * @param area  boolean mask indicating where to set the value to true
     * @param value mask representing the values to set where area is true
     * @return the modified mask
     */
    public U setToValue(BooleanMask area, U value) {
        assertCompatibleMask(area);
        assertCompatibleMask(value);
        return enqueue(dependencies -> {
            BooleanMask placement = (BooleanMask) dependencies.get(0);
            U source = (U) dependencies.get(1);
            apply((x, y) -> {
                if (placement.getPrimitive(x, y)) {
                    set(x, y, source.get(x, y));
                }
            });
        }, area, value);
    }

    public boolean inBounds(int x, int y) {
        int size = getSize();
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public boolean onBoundary(Vector2 location) {
        return onBoundary((int) location.getX(), (int) location.getY());
    }

    public boolean onBoundary(int x, int y) {
        int size = getSize();
        return x == 0 || x == size - 1 || y == 0 || y == size - 1;
    }

    public List<Vector2> getSymmetryPoints(Vector3 point, SymmetryType symmetryType) {
        return getSymmetryPoints(new Vector2(point), symmetryType);
    }

    public List<Vector2> getSymmetryPoints(Vector2 point, SymmetryType symmetryType) {
        return getSymmetryPoints(point.getX(), point.getY(), symmetryType);
    }

    public List<Vector2> getSymmetryPoints(float x, float y, SymmetryType symmetryType) {
        List<Vector2> symmetryPoints = getSymmetryPointsWithOutOfBounds(x, y, symmetryType);
        symmetryPoints.removeIf(point -> !inBounds(point));
        return symmetryPoints;
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(Vector3 point, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(new Vector2(point), symmetryType);
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(Vector2 point, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(point.getX(), point.getY(), symmetryType);
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(float x, float y, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int numSymPoints = symmetry.getNumSymPoints();
        List<Vector2> symmetryPoints = new ArrayList<>(numSymPoints - 1);
        int size = getSize();
        switch (symmetry) {
            case POINT2 -> symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
            case POINT4 -> {
                symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                symmetryPoints.add(new Vector2(y, size - x - 1));
                symmetryPoints.add(new Vector2(size - y - 1, x));
            }
            case POINT6, POINT8, POINT10, POINT12, POINT14, POINT16 -> {
                symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                for (int i = 1; i < numSymPoints / 2; i++) {
                    float angle = (float) (2 * StrictMath.PI * i / numSymPoints);
                    Vector2 rotated = getRotatedPoint(x, y, angle);
                    symmetryPoints.add(rotated);
                    Vector2 antiRotated = getRotatedPoint(x, y, (float) (angle + StrictMath.PI));
                    symmetryPoints.add(antiRotated);
                }
            }
            case POINT3, POINT5, POINT7, POINT9, POINT11, POINT13, POINT15 -> {
                for (int i = 1; i < numSymPoints; i++) {
                    Vector2 rotated = getRotatedPoint(x, y, (float) (2 * StrictMath.PI * i / numSymPoints));
                    symmetryPoints.add(rotated);
                }
            }
            case X -> symmetryPoints.add(new Vector2(size - x - 1, y));
            case Z -> symmetryPoints.add(new Vector2(x, size - y - 1));
            case XZ -> symmetryPoints.add(new Vector2(y, x));
            case ZX -> symmetryPoints.add(new Vector2(size - y - 1, size - x - 1));
            case QUAD -> {
                if (symmetrySettings.teamSymmetry() == Symmetry.Z) {
                    symmetryPoints.add(new Vector2(x, size - y - 1));
                    symmetryPoints.add(new Vector2(size - x - 1, y));
                    symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                } else {
                    symmetryPoints.add(new Vector2(size - x - 1, y));
                    symmetryPoints.add(new Vector2(x, size - y - 1));
                    symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                }
            }
            case DIAG -> {
                if (symmetrySettings.teamSymmetry() == Symmetry.ZX) {
                    symmetryPoints.add(new Vector2(size - y - 1, size - x - 1));
                    symmetryPoints.add(new Vector2(y, x));
                    symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                } else {
                    symmetryPoints.add(new Vector2(y, x));
                    symmetryPoints.add(new Vector2(size - y - 1, size - x - 1));
                    symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                }
            }
        }
        return symmetryPoints;
    }

    protected int getMaxYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        return switch (symmetry) {
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 ->
                    getMaxYFromXOnArc(
                            x, 360f / symmetry.getNumSymPoints());
            case ZX, DIAG -> size - x;
            case Z, POINT2, POINT4, QUAD -> size / 2 + size % 2;
            default -> size;
        };
    }

    public ArrayList<Float> getSymmetryRotation(float rot) {
        return getSymmetryRotation(rot, SymmetryType.SPAWN);
    }

    public ArrayList<Float> getSymmetryRotation(float rot, SymmetryType symmetryType) {
        ArrayList<Float> symmetryRotation = new ArrayList<>();
        final float xRotation = (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
        final float zRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
        final float diagRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        Symmetry teamSymmetry = symmetrySettings.teamSymmetry();
        switch (symmetry) {
            case POINT2, X, Z -> symmetryRotation.add(rot + (float) StrictMath.PI);
            case POINT4 -> {
                symmetryRotation.add(rot + (float) StrictMath.PI);
                symmetryRotation.add(rot + (float) StrictMath.PI / 2);
                symmetryRotation.add(rot - (float) StrictMath.PI / 2);
            }
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> {
                int numSymPoints = symmetry.getNumSymPoints();
                for (int i = 1; i < numSymPoints; i++) {
                    symmetryRotation.add(rot + (float) (2 * StrictMath.PI * i / numSymPoints));
                }
            }
            case XZ, ZX -> symmetryRotation.add(diagRotation);
            case QUAD -> {
                if (teamSymmetry == Symmetry.Z) {
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
            case DIAG -> {
                if (teamSymmetry == Symmetry.ZX) {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
        }
        return symmetryRotation;
    }

    protected int getMinXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return 0;
    }

    protected int getMaxXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        return switch (symmetry) {
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 ->
                    StrictMath.max(
                            getMaxXFromAngle(360f / symmetry.getNumSymPoints()), size / 2 + 1);
            case POINT4, X, QUAD, DIAG -> size / 2;
            default -> size;
        };
    }

    /**
     * Scales the mask to tne given size.
     * Filters the mask before/after scaling the content
     *
     * @param newSize size to scale the mask to
     * @return the scaled mask
     */
    public U resample(int newSize) {
        int size = getSize();
        if (newSize != size) {
            plannedSize = newSize;
            return enqueue(() -> {
                if (size < newSize) {
                    setSize(newSize);
                    blur(StrictMath.round((float) newSize / size / 2 - 1));
                } else {
                    blur(StrictMath.round((float) size / newSize / 2 - 1));
                    setSize(newSize);
                }
            });
        } else {
            return (U) this;
        }
    }

    public boolean inTeamNoBounds(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM)
                && x < getMaxXBound(SymmetryType.TEAM)
                && y >= getMinYBound(x, SymmetryType.TEAM)
                && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse;
    }

    private int getMaxXFromAngle(float angle) {
        int size = getSize();
        int x = (int) StrictMath.round(StrictMath.cos(((angle + 180) / 180) % 2 * StrictMath.PI) * size + size / 2f);
        return StrictMath.max(StrictMath.min(x, size), 0);
    }

    private int getMinYFromXOnArc(int x, float angle) {
        int size = getSize();
        float dx = x - size / 2f;
        int y;
        if (x > getMaxXFromAngle(angle)) {
            y = (int) (size / 2 + StrictMath.tan(((angle + 180) / 180) % 2 * StrictMath.PI) * dx);
        } else {
            y = (int) StrictMath.round(size / 2f - StrictMath.sqrt(size * size - dx * dx));
        }
        return StrictMath.max(StrictMath.min(y, size), 0);
    }

    private int getMaxYFromXOnArc(int x, float angle) {
        int size = getSize();
        float dx = x - size / 2f;
        int y;
        if (x > size / 2) {
            y = (int) (size / 2f + StrictMath.tan(((angle + 180) / 180) % 2 * StrictMath.PI) * dx);
        } else {
            y = size / 2 + 1;
        }
        return StrictMath.max(StrictMath.min(y, getSize()), 0);
    }

    public boolean inTeam(Vector3 pos, boolean reverse) {
        return inTeam(new Vector2(pos), reverse);
    }

    public boolean inTeam(Vector2 pos, boolean reverse) {
        return inTeam((int) pos.getX(), (int) pos.getY(), reverse);
    }

    public boolean inHalfNoBounds(int x, int y, float angle) {
        return inHalfNoBounds(new Vector2(x, y), angle);
    }

    public boolean inTeamNoBounds(Vector3 pos, boolean reverse) {
        return inTeam(new Vector2(pos), reverse);
    }

    public boolean inTeamNoBounds(Vector2 pos, boolean reverse) {
        return inTeam((int) pos.getX(), (int) pos.getY(), reverse);
    }

    public boolean inHalfNoBounds(Vector2 pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2(halfSize, halfSize).angleTo(pos) * 180f / StrictMath.PI) + 90f + 360f)
                            % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle);
        }
    }

    public boolean inHalf(Vector3 pos, float angle) {
        return inHalf(new Vector2(pos), angle);
    }

    public U forceSymmetry(SymmetryType symmetryType, boolean reverse) {
        if (!reverse) {
            boolean isPerfectSym = symmetrySettings.getSymmetry(SymmetryType.SPAWN).isPerfectSymmetry();
            if (!isPerfectSym) {
                // When we don't have a perfect symmetry, we can skip this.
                return enqueue(() -> {});
            } else {
                return applyWithSymmetry(symmetryType, (x, y) -> {
                    T value = get(x, y);
                    applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> set(sx, sy, value));
                });
            }
        } else {
            if (symmetrySettings.getSymmetry(symmetryType).getNumSymPoints() != 2) {
                throw new IllegalArgumentException("Symmetry has more than two symmetry points");
            }
            return applyWithSymmetry(symmetryType, (x, y) -> {
                List<Vector2> symPoints = getSymmetryPoints(x, y, symmetryType);
                symPoints.forEach(symPoint -> set(x, y, get((int) symPoint.getX(), (int) symPoint.getY())));
            });
        }
    }

    public T get(Vector2 location) {
        return get(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    public boolean inHalfNoBounds(Vector3 pos, float angle) {
        return inHalfNoBounds(new Vector2(pos), angle);
    }

    protected U applyWithSymmetry(SymmetryType symmetryType, BiIntConsumer maskAction) {
        return enqueue(() -> {
            loopWithSymmetry(symmetryType, maskAction);
            if (!symmetrySettings.getSymmetry(symmetryType).isPerfectSymmetry() && symmetrySettings.spawnSymmetry()
                                                                                                   .isPerfectSymmetry()) {
                forceSymmetry(SymmetryType.SPAWN);
            }
        });
    }

    public U forceSymmetry(float angle) {
        if (symmetrySettings.getSymmetry(SymmetryType.SPAWN) != Symmetry.POINT2) {
            throw new IllegalArgumentException("Spawn Symmetry must equal POINT2");
        }
        return apply((x, y) -> {
            if (inHalf(x, y, angle)) {
                T value = get(x, y);
                applyAtSymmetryPoints(x, y, SymmetryType.SPAWN, (sx, sy) -> set(sx, sy, value));
            }
        });
    }

    public boolean inHalf(int x, int y, float angle) {
        return inHalf(new Vector2(x, y), angle);
    }

    public boolean inHalf(Vector2 pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2(halfSize, halfSize).angleTo(pos) * 180f / StrictMath.PI) + 90f + 360f)
                            % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle) && inBounds(pos);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle) && inBounds(pos);
        }
    }

    public boolean inBounds(Vector2 location) {
        return inBounds(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    void copyPrimitiveFromReverseLookup(int x, int y) {
        int numSpawns = symmetrySettings.spawnSymmetry().getNumSymPoints();
        double radiansPerSlice = StrictMath.PI * 2 / numSpawns;
        int size = getSize();
        int dx = x - (size / 2);
        int dy = y - (size / 2);

        // Find the angle of this point relative to the center of the map
        double angle = StrictMath.atan2(dy, dx);
        if (y < 0) {
            angle = StrictMath.PI - angle;
        } else {
            angle = StrictMath.PI + angle;
        }

        // Find out what slice of the pie this pixel sits in
        int slice = (int) (angle / radiansPerSlice);
        if (slice > 0) {
            // Find the angle we need to rotate, in order to lookup this pixels value on the original slice.
            double antiRotateAngle = -slice * radiansPerSlice;

            // Find the X and Y coords of this pixel in the original slice
            float halfSize = size / 2f;
            float xOffset = x - halfSize;
            float yOffset = y - halfSize;
            double cosAngle = StrictMath.cos(antiRotateAngle);
            double sinAngle = StrictMath.sin(antiRotateAngle);
            float antiRotatedX = (float) (xOffset * cosAngle - yOffset * sinAngle + halfSize);
            float antiRotatedY = (float) (xOffset * sinAngle + yOffset * cosAngle + halfSize);

            // Copy the value from the original slice
            if (inBounds((int) antiRotatedX, (int) antiRotatedY)) {
                set(x, y, get((int) antiRotatedX, (int) antiRotatedY));
            }
        }
    }

    public U forceSymmetry(SymmetryType symmetryType) {
        return forceSymmetry(symmetryType, false);
    }

    /**
     * Force spawn symmetry on the map
     *
     * @return the symmetric mask
     */
    public U forceSymmetry() {
        return forceSymmetry(SymmetryType.SPAWN);
    }

    protected U setWithSymmetry(SymmetryType symmetryType, BiIntFunction<T> valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            T value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> set(sx, sy, value));
        });
    }

    protected U applyAtSymmetryPointsWithOutOfBounds(Vector2 location, SymmetryType symmetryType,
                                                     BiIntConsumer action) {
        return applyAtSymmetryPointsWithOutOfBounds((int) location.getX(), (int) location.getY(), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(Vector2 location, SymmetryType symmetryType, BiIntConsumer action) {
        return applyAtSymmetryPoints((int) location.getX(), (int) location.getY(), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(int x, int y, SymmetryType symmetryType, BiIntConsumer action) {
        return enqueue(() -> {
            action.accept(x, y);
            List<Vector2> symPoints = getSymmetryPoints(x, y, symmetryType);
            symPoints.forEach(symPoint -> action.accept((int) symPoint.getX(), (int) symPoint.getY()));
        });
    }

    protected U applyWithOffset(U other, BiIntObjConsumer<T> action, int xOffset, int yOffset, boolean center,
                                boolean wrapEdges) {
        return enqueue(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int biggerSize = StrictMath.max(size, otherSize);
            if (smallerSize == otherSize) {
                if (symmetrySettings.spawnSymmetry().isPerfectSymmetry()) {
                    Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    other.apply((x, y) -> {
                        int shiftX = coordinateXMap.get(x);
                        int shiftY = coordinateYMap.get(y);
                        if (inBounds(shiftX, shiftY)) {
                            T value = other.get(x, y);
                            applyAtSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN,
                                                  (sx, sy) -> action.accept(sx, sy, value));
                        }
                    });
                } else {
                    applyAtSymmetryPointsWithOutOfBounds(xOffset, yOffset, SymmetryType.SPAWN, (sx, sy) -> {
                        Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(sx, center, wrapEdges, otherSize,
                                                                                       size);
                        Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(sy, center, wrapEdges, otherSize,
                                                                                       size);
                        other.apply((x, y) -> {
                            int shiftX = coordinateXMap.get(x);
                            int shiftY = coordinateYMap.get(y);
                            if (inBounds(shiftX, shiftY)) {
                                action.accept(shiftX, shiftY, other.get(x, y));
                            }
                        });
                    });
                }
            } else {
                Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges, size,
                                                                               otherSize);
                Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges, size,
                                                                               otherSize);
                apply((x, y) -> {
                    int shiftX = coordinateXMap.get(x);
                    int shiftY = coordinateYMap.get(y);
                    if (other.inBounds(shiftX, shiftY)) {
                        T value = other.get(shiftX, shiftY);
                        action.accept(x, y, value);
                    }
                });
            }
        });
    }

    protected U applyAtSymmetryPointsWithOutOfBounds(int x, int y, SymmetryType symmetryType, BiIntConsumer action) {
        return enqueue(() -> {
            action.accept(x, y);
            List<Vector2> symPoints = getSymmetryPointsWithOutOfBounds(x, y, symmetryType);
            symPoints.forEach(point -> action.accept((int) point.getX(), (int) point.getY()));
        });
    }

    protected void populateCoordinateMaps(int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges,
                                          int fromSize, int toSize, Map<Integer, Integer> coordinateXMap,
                                          Map<Integer, Integer> coordinateYMap) {
        int offsetX;
        int offsetY;
        if (center) {
            offsetX = xCoordinate - fromSize / 2;
            offsetY = yCoordinate - fromSize / 2;
        } else {
            offsetX = xCoordinate;
            offsetY = yCoordinate;
        }
        for (int i = 0; i < fromSize; ++i) {
            coordinateXMap.put(i, getShiftedValue(i, offsetX, toSize, wrapEdges));
            coordinateYMap.put(i, getShiftedValue(i, offsetY, toSize, wrapEdges));
        }
    }

    protected Map<Integer, Integer> getShiftedCoordinateMap(int offset, boolean center, boolean wrapEdges, int fromSize,
                                                            int toSize) {
        int trueOffset;
        if (center) {
            trueOffset = offset - fromSize / 2;
        } else {
            trueOffset = offset;
        }

        return IntStream.range(0, fromSize)
                        .boxed()
                        .collect(Collectors.toMap(i -> i, i -> getShiftedValue(i, trueOffset, toSize, wrapEdges)));
    }

    protected void loopWithSymmetry(SymmetryType symmetryType, BiIntConsumer maskAction) {
        assertNotPipelined();
        int minX = getMinXBound(symmetryType);
        int maxX = getMaxXBound(symmetryType);
        for (int x = minX; x < maxX; x++) {
            int minY = getMinYBound(x, symmetryType);
            int maxY = getMaxYBound(x, symmetryType);
            for (int y = minY; y < maxY; y++) {
                maskAction.accept(x, y);
            }
        }
    }

    protected void assertCompatibleMask(Mask<?, ?> other) {
        int otherSize = other.getSize();
        int size = getSize();
        String name = getName();
        String otherName = other.getName();
        if (otherSize != size) {
            throw new IllegalArgumentException(
                    String.format("Masks not the same size: %s is %d and %s is %d", name, size, otherName, otherSize));
        }
        SymmetrySettings symmetrySettings = getSymmetrySettings();
        SymmetrySettings otherSymmetrySettings = other.getSymmetrySettings();
        if (symmetrySettings.spawnSymmetry() != Symmetry.NONE && !symmetrySettings.equals(otherSymmetrySettings)) {
            throw new IllegalArgumentException(
                    String.format("Masks not the same symmetry: %s is %s and %s is %s", name, symmetrySettings,
                                  otherName, otherSymmetrySettings));
        }
        if (isParallel() && !Pipeline.isRunning() && !other.isParallel()) {
            throw new IllegalArgumentException(
                    String.format("Masks not the same processing chain: %s and %s", name, otherName));
        }
    }

    protected void assertSmallerSize(int size) {
        int actualSize = getSize();
        if (size > actualSize) {
            throw new IllegalArgumentException(
                    "Intended mask size is larger than base mask size: Mask is " + actualSize + " and size is " + size);
        }
    }

    protected void assertSize(int size) {
        int actualSize = getSize();
        if (size != actualSize) {
            throw new IllegalArgumentException(
                    "Mask size is incorrect: Mask is " + actualSize + " and size is " + size);
        }
    }

    /**
     * Copy the mask
     *
     * @return a copy of the mask
     */
    public U copy() {
        return copy(getName() + COPY_NAME);
    }

    public U getFinalMask() {
        Pipeline.await(this);
        U finalMask = copy();
        finalMask.setParallel(false);
        return finalMask;
    }

    @SneakyThrows
    public U copy(String maskName) {
        Class<?> clazz = getClass();
        return (U) clazz.getDeclaredConstructor(clazz, String.class).newInstance(this, maskName);
    }

    public U startVisualDebugger() {
        return startVisualDebugger(name == null ? toString() : name);
    }

    public U startVisualDebugger(String maskName) {
        visualName = maskName;
        visualDebug = DebugUtil.DEBUG;
        visible = true;
        show();
        return (U) this;
    }

    public U show() {
        if (!parallel && (((DebugUtil.DEBUG && isVisualDebug())) && visible)) {
            VisualDebugger.visualizeMask(this, "show");
        }
        return (U) this;
    }

    /**
     * Fill the sides of the mask where the sides are defined by the team {@link Symmetry}
     * in the mask {@link SymmetrySettings}
     *
     * @param extent how far to fill the sides in pixels
     * @param value  value to fill the pixels with
     * @return the modified mask
     */
    public U fillSides(int extent, T value) {
        return fillSides(extent, value, SymmetryType.SPAWN);
    }

    public U fillSides(int extent, T value, SymmetryType symmetryType) {
        return enqueue(() -> {
            int size = getSize();
            switch (symmetrySettings.getSymmetry(symmetryType)) {
                case Z -> fillRect(0, 0, extent / 2, size, value).fillRect(size - extent / 2, 0, size - extent / 2,
                                                                           size, value);
                case X -> fillRect(0, 0, size, extent / 2, value).fillRect(0, size - extent / 2, size, extent / 2,
                                                                           value);
                case XZ -> fillParallelogram(0, 0, size, extent * 3 / 4, 0, -1, value).fillParallelogram(
                        size - extent * 3 / 4, size, size, extent * 3 / 4, 0, -1, value);
                case ZX -> fillParallelogram(size - extent * 3 / 4, 0, extent * 3 / 4, extent * 3 / 4, 1, 0,
                                             value).fillParallelogram(-extent * 3 / 4, size - extent * 3 / 4,
                                                                      extent * 3 / 4, extent * 3 / 4, 1, 0, value);
            }
            forceSymmetry(symmetryType);
        });
    }

    /**
     * Fill the center of the mask using the team {@link Symmetry} of the {@link SymmetrySettings}
     *
     * @param radius how many pixels to fill in the center
     * @param value  value to fill in the center with
     * @return the modified mask
     */
    public U fillCenter(int radius, T value) {
        return fillCenter(radius, value, SymmetryType.TEAM);
    }

    public U fillCenter(int extent, T value, SymmetryType symmetryType) {
        return enqueue(() -> {
            int size = getSize();
            switch (symmetrySettings.getSymmetry(symmetryType)) {
                case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 ->
                        fillCircle(
                                (float) size / 2, (float) size / 2, extent * 3 / 4f, value);
                case Z -> fillRect(0, size / 2 - extent / 2, size, extent, value);
                case X -> fillRect(size / 2 - extent / 2, 0, extent, size, value);
                case XZ -> fillDiagonal(extent * 3 / 4, false, value);
                case ZX -> fillDiagonal(extent * 3 / 4, true, value);
                case DIAG -> {
                    if (symmetrySettings.teamSymmetry() == Symmetry.DIAG) {
                        fillDiagonal(extent * 3 / 8, false, value);
                        fillDiagonal(extent * 3 / 8, true, value);
                    } else {
                        fillDiagonal(extent * 3 / 16, false, value);
                        fillDiagonal(extent * 3 / 16, true, value);
                        fillCenter(extent, value, SymmetryType.TEAM);
                    }
                }
                case QUAD -> {
                    if (symmetrySettings.teamSymmetry() == Symmetry.QUAD) {
                        fillRect(size / 2 - extent / 4, 0, extent / 2, size, value);
                        fillRect(0, size / 2 - extent / 4, size, extent / 2, value);
                    } else {
                        fillRect(size / 2 - extent / 8, 0, extent / 4, size, value);
                        fillRect(0, size / 2 - extent / 8, size, extent / 4, value);
                        fillCenter(extent, value, SymmetryType.TEAM);
                    }
                }
            }
            forceSymmetry(SymmetryType.SPAWN);
        });
    }

    public U fillCircle(Vector3 center, float radius, T value) {
        return fillCircle(center.getX(), center.getZ(), radius, value);
    }

    public U fillCircle(Vector2 center, float radius, T value) {
        return fillCircle(center.getX(), center.getY(), radius, value);
    }

    public U fillCircle(float x, float y, float radius, T value) {
        return enqueue(() -> fillArc(x, y, 0, 360, radius, value));
    }

    public U fillArc(float x, float y, float startAngle, float endAngle, float radius, T value) {
        return enqueue(() -> {
            float dx;
            float dy;
            float radius2 = (radius + .5f) * (radius + .5f);
            float radiansToDegreeFactor = (float) (180 / StrictMath.PI);
            for (int cx = StrictMath.round(x - radius); cx < StrictMath.round(x + radius + 1); cx++) {
                for (int cy = StrictMath.round(y - radius); cy < StrictMath.round(y + radius + 1); cy++) {
                    dx = x - cx;
                    dy = y - cy;
                    float angle = (float) (StrictMath.atan2(dy, dx) / radiansToDegreeFactor + 360) % 360;
                    if (inBounds(cx, cy) && dx * dx + dy * dy <= radius2 && angle >= startAngle && angle <= endAngle) {
                        set(cx, cy, value);
                    }
                }
            }
        });
    }

    public U fillSquare(Vector2 topLeft, int extent, T value) {
        return fillSquare((int) topLeft.getX(), (int) topLeft.getY(), extent, value);
    }

    public U fillSquare(int x, int y, int extent, T value) {
        return enqueue(() -> fillRect(x, y, extent, extent, value));
    }

    public U fillRect(Vector2 topLeft, int width, int height, T value) {
        return fillRect((int) topLeft.getX(), (int) topLeft.getY(), width, height, value);
    }

    public U fillRect(int x, int y, int width, int height, T value) {
        return enqueue(() -> fillParallelogram(x, y, width, height, 0, 0, value));
    }

    public U fillRectFromPoints(int x1, int x2, int z1, int z2, T value) {
        int smallX = StrictMath.min(x1, x2);
        int bigX = StrictMath.max(x1, x2);
        int smallZ = StrictMath.min(z1, z2);
        int bigZ = StrictMath.max(z1, z2);
        return fillRect(smallX, smallZ, bigX - smallX, bigZ - smallZ, value);
    }

    public U fillParallelogram(Vector2 topLeft, int width, int height, int xSlope, int ySlope, T value) {
        return fillParallelogram((int) topLeft.getX(), (int) topLeft.getY(), width, height, xSlope, ySlope, value);
    }

    public U fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, T value) {
        return enqueue(() -> {
            for (int px = 0; px < width; px++) {
                for (int py = 0; py < height; py++) {
                    int calcX = x + px + py * xSlope;
                    int calcY = y + py + px * ySlope;
                    if (inBounds(calcX, calcY)) {
                        set(calcX, calcY, value);
                    }
                }
            }
        });
    }

    public U fillDiagonal(int extent, boolean inverted, T value) {
        return enqueue(() -> {
            int size = getSize();
            for (int cx = -extent; cx < extent; cx++) {
                for (int y = 0; y < size; y++) {
                    int x;
                    if (inverted) {
                        x = size - (cx + y);
                    } else {
                        x = cx + y;
                    }
                    if (x >= 0 && x < size) {
                        set(x, y, value);
                    }
                }
            }
        });
    }

    public U fillEdge(int rimWidth, T value) {
        return enqueue(() -> {
            int size = getSize();
            for (int a = 0; a < rimWidth; a++) {
                for (int b = 0; b < size - rimWidth; b++) {
                    set(a, b, value);
                    set(size - 1 - a, size - 1 - b, value);
                    set(b, size - 1 - a, value);
                    set(size - 1 - b, a, value);
                }
            }
        });
    }

    protected U fillCoordinates(Collection<Vector2> coordinates, T value) {
        coordinates.forEach(
                location -> applyAtSymmetryPoints((int) location.getX(), (int) location.getY(), SymmetryType.SPAWN,
                                                  (x, y) -> set(x, y, value)));
        return (U) this;
    }
}
