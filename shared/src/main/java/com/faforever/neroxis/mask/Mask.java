package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetryUtil;
import com.faforever.neroxis.util.functional.BiIntConsumer;
import com.faforever.neroxis.util.functional.BiIntFunction;
import com.faforever.neroxis.util.functional.BiIntObjConsumer;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.visualization.VisualDebugger;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract sealed class Mask<T, U extends Mask<T, U>> permits OperationsMask {
    private static final String MOCK_NAME = "Mock";
    private static final String COPY_NAME = "Copy";
    private final AtomicInteger copyCount = new AtomicInteger();
    protected final Random random;
    @Getter
    private final String name;
    @Getter
    protected final SymmetrySettings symmetrySettings;
    @Getter
    private boolean immutable;
    private int plannedSize;
    @Getter
    @Setter
    private boolean visualDebug;
    private boolean visible;
    private boolean mock;
    @Setter
    private String visualName;

    protected Mask(U other, String name) {
        this(other.getSize(), (name != null && name.endsWith(MOCK_NAME)) ? null : other.getNextSeed(),
             other.getSymmetrySettings(), name);
        init(other);
    }

    protected Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this.symmetrySettings = symmetrySettings;
        this.name = name == null ? String.valueOf(hashCode()) : name;
        this.plannedSize = size;
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
        if (Pipeline.isAccepting()) {
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
        return get(StrictMath.round(location.x()), StrictMath.round(location.z()));
    }

    protected abstract T get(int x, int y);

    protected void set(Vector3 location, T value) {
        set(StrictMath.round(location.x()), StrictMath.round(location.z()), value);
    }

    protected abstract void set(int x, int y, T value);

    protected void set(Vector2 location, T value) {
        set(StrictMath.round(location.x()), StrictMath.round(location.y()), value);
    }

    public U immutableCopy() {
        assertNotPipelined();
        Mask<?, U> copy = copy(getName() + MOCK_NAME);
        copy.makeImmutable();
        return (U) copy;
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
        return enqueue(_ -> function.run());
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
        List<Mask<?, ?>> dependencies = List.of(usedMasks);
        if (Pipeline.isAccepting()) {
            Pipeline.add(this, dependencies, function);
        } else {
            boolean visibleState = visible;
            visible = false;
            function.accept(dependencies);
            visible = visibleState;
            if (isVisualDebug() && visible) {
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
            BooleanMask source = (BooleanMask) dependencies.getFirst();
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
        if (Pipeline.isAccepting()) {
            throw new IllegalStateException("Mask is pipelined and cannot return an immediate result");
        }
    }

    /**
     * Rotates a point (x, y) around the center of a square of size {@code getSize()}
     * using a three-shear decomposition of the rotation matrix:
     *
     * <pre>
     * R(θ) = ShearX(-tan(θ/2)) * ShearY(sin(θ)) * ShearX(-tan(θ/2))
     * </pre>
     * <p>
     * Coordinates are first translated so the square’s center is the origin,
     * rotated via shear steps with {@link StrictMath#round(double)} applied at each stage
     * (for grid alignment), and then translated back.
     * <a href="https://en.wikipedia.org/wiki/Shear_matrix#Rotation">Wikipedia – Shear matrix: Rotation</a></li>
     * * </ul>
     *
     * @param x     point x-coordinate
     * @param y     point y-coordinate
     * @param angle rotation angle in radians
     * @return rotated point as {@code Vector2}
     */
    private Vector2 getRotatedPoint(float x, float y, float angle) {
        float halfSize = getSize() / 2f;

        // Translate so that center is at origin
        double xt = x - halfSize;
        double yt = y - halfSize;

        double tanHalf = StrictMath.tan(angle / 2.0);
        double sin = StrictMath.sin(angle);

        // Step 1: shear along x-axis
        double x1 = StrictMath.round(xt - yt * tanHalf);
        double y1 = StrictMath.round(yt);

        // Step 2: shear along y-axis
        double x2 = StrictMath.round(x1);
        double y2 = StrictMath.round(y1 + x1 * sin);

        // Step 3: shear along x-axis again
        double xr = StrictMath.round(x2 - y2 * tanHalf);
        double yr = StrictMath.round(y2);

        // Translate back
        return new Vector2((float) (xr + halfSize), (float) (yr + halfSize));
    }

    protected static boolean inBounds(int x, int y, int size) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public boolean inTeam(int x, int y, boolean reverse) {
        return (x >= 0 &&
                x < getMaxXBound(SymmetryType.TEAM) &&
                y >= getMinYBoundFunction(SymmetryType.TEAM).applyAsInt(x) &&
                y < getMaxYBoundFunction(SymmetryType.TEAM).applyAsInt(x)) ^ reverse && inBounds(x, y);
    }

    protected int getMaxXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return SymmetryUtil.getMaxXBound(symmetry, getSize());
    }

    protected IntUnaryOperator getMinYBoundFunction(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return SymmetryUtil.getMinYBoundFunction(symmetry, getSize());
    }

    protected IntUnaryOperator getMaxYBoundFunction(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return SymmetryUtil.getMaxYBoundFunction(symmetry, getSize());
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
            BooleanMask source = (BooleanMask) dependencies.getFirst();
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
        return inBounds(x, y, getSize());
    }

    public boolean onBoundary(Vector2 location) {
        return onBoundary((int) location.x(), (int) location.y());
    }

    public boolean onBoundary(int x, int y) {
        int size = getSize();
        return x == 0 || x == size - 1 || y == 0 || y == size - 1;
    }

    public List<Vector2> getSymmetryPoints(Vector3 point, SymmetryType symmetryType) {
        return getSymmetryPoints(new Vector2(point), symmetryType);
    }

    public List<Vector2> getSymmetryPoints(Vector2 point, SymmetryType symmetryType) {
        return getSymmetryPoints(point.x(), point.y(), symmetryType);
    }

    public List<Vector2> getSymmetryPoints(float x, float y, SymmetryType symmetryType) {
        List<Vector2> symmetryPoints = new ArrayList<>(getSymmetryPointsWithOutOfBounds(x, y, symmetryType));
        symmetryPoints.removeIf(point -> !inBounds(point));
        return List.copyOf(symmetryPoints);
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(Vector3 point, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(new Vector2(point), symmetryType);
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(Vector2 point, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(point.x(), point.y(), symmetryType);
    }

    public List<Vector2> getSymmetryPointsWithOutOfBounds(float x, float y, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        Symmetry secondarySymmetry = symmetrySettings.getSymmetry(SymmetryType.TEAM);
        return SymmetryUtil.getSymmetryPoints(x, y, getSize(), symmetry, secondarySymmetry);
    }

    public List<Float> getSymmetryRotations(float rot) {
        return getSymmetryRotations(rot, SymmetryType.SPAWN);
    }

    public List<Float> getSymmetryRotations(float rot, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        Symmetry teamSymmetry = symmetrySettings.teamSymmetry();
        return SymmetryUtil.getSymmetryRotations(rot, symmetry, teamSymmetry);
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
        return (x >= 0 &&
                x < getMaxXBound(SymmetryType.TEAM) &&
                y >= getMinYBoundFunction(SymmetryType.TEAM).applyAsInt(x) &&
                y < getMaxYBoundFunction(SymmetryType.TEAM).applyAsInt(x)) ^ reverse;
    }

    public boolean inTeam(Vector3 pos, boolean reverse) {
        return inTeam(new Vector2(pos), reverse);
    }

    public boolean inTeam(Vector2 pos, boolean reverse) {
        return inTeam((int) pos.x(), (int) pos.y(), reverse);
    }

    public boolean inHalfNoBounds(int x, int y, float angle) {
        return inHalfNoBounds(new Vector2(x, y), angle);
    }

    public boolean inTeamNoBounds(Vector3 pos, boolean reverse) {
        return inTeam(new Vector2(pos), reverse);
    }

    public boolean inTeamNoBounds(Vector2 pos, boolean reverse) {
        return inTeam((int) pos.x(), (int) pos.y(), reverse);
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
        if (!getSymmetrySettings().terrainSymmetry().isPerfectSymmetry()) {
            return enqueue(() -> {});
        } else {
            if (!reverse) {
                return applyWithSymmetry(symmetryType, (x, y) -> {
                    T value = get(x, y);
                    applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> set(sx, sy, value));
                });
            } else {
                if (symmetrySettings.getSymmetry(symmetryType).getNumSymPoints() != 2) {
                    throw new IllegalArgumentException("Symmetry has more than two symmetry points");
                }
                return applyWithSymmetry(symmetryType, (x, y) -> {
                    List<Vector2> symPoints = getSymmetryPoints(x, y, symmetryType);
                    symPoints.forEach(symPoint -> set(x, y, get((int) symPoint.x(), (int) symPoint.y())));
                });
            }
        }
    }

    public T get(Vector2 location) {
        return get(StrictMath.round(location.x()), StrictMath.round(location.y()));
    }

    public boolean inHalfNoBounds(Vector3 pos, float angle) {
        return inHalfNoBounds(new Vector2(pos), angle);
    }

    protected U applyWithSymmetry(SymmetryType symmetryType, BiIntConsumer maskAction) {
        return enqueue(() -> {
            loopInSymmetryRegion(symmetryType, maskAction);
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
        return inBounds(StrictMath.round(location.x()), StrictMath.round(location.y()));
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
        return applyAtSymmetryPointsWithOutOfBounds((int) location.x(), (int) location.y(), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(Vector2 location, SymmetryType symmetryType, BiIntConsumer action) {
        return applyAtSymmetryPoints((int) location.x(), (int) location.y(), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(int x, int y, SymmetryType symmetryType, BiIntConsumer action) {
        return enqueue(() -> {
            int size = getSize();
            BiIntConsumer protectedAction = (px, py) -> {
                if (!inBounds(px, py, size)) {
                    return;
                }

                action.accept(px, py);
            };
            Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);


            protectedAction.accept(x, y);
            switch (symmetry) {
                case POINT2 -> protectedAction.accept(size - x - 1, size - y - 1);
                case POINT4 -> {
                    protectedAction.accept(size - x - 1, size - y - 1);
                    protectedAction.accept(y, size - x - 1);
                    protectedAction.accept(size - y - 1, x);
                }
                case POINT6, POINT8, POINT10, POINT12, POINT14, POINT16 -> {
                    protectedAction.accept(size - x - 1, size - y - 1);
                    int numSymPoints = symmetry.getNumSymPoints();
                    for (int i = 1; i < numSymPoints / 2; i++) {
                        float angle = (float) (2 * StrictMath.PI * i / numSymPoints);
                        Vector2 rotated = getRotatedPoint(x, y, angle);
                        protectedAction.accept((int) rotated.x(), (int) rotated.y());
                        Vector2 antiRotated = getRotatedPoint(x, y, (float) (angle + StrictMath.PI));
                        protectedAction.accept((int) antiRotated.x(), (int) antiRotated.y());
                    }
                }
                case POINT3, POINT5, POINT7, POINT9, POINT11, POINT13, POINT15 -> {
                    int numSymPoints = symmetry.getNumSymPoints();
                    for (int i = 1; i < numSymPoints; i++) {
                        Vector2 rotated = getRotatedPoint(x, y, (float) (2 * StrictMath.PI * i / numSymPoints));
                        protectedAction.accept((int) rotated.x(), (int) rotated.y());
                    }
                }
                case X -> protectedAction.accept(size - x - 1, y);
                case Z -> protectedAction.accept(x, size - y - 1);
                case XZ -> protectedAction.accept(y, x);
                case ZX -> protectedAction.accept(size - y - 1, size - x - 1);
                case QUAD -> {
                    if (symmetrySettings.teamSymmetry() == Symmetry.Z) {
                        protectedAction.accept(x, size - y - 1);
                        protectedAction.accept(size - x - 1, y);
                        protectedAction.accept(size - x - 1, size - y - 1);
                    } else {
                        protectedAction.accept(size - x - 1, y);
                        protectedAction.accept(x, size - y - 1);
                        protectedAction.accept(size - x - 1, size - y - 1);
                    }
                }
                case DIAG -> {
                    if (symmetrySettings.teamSymmetry() == Symmetry.ZX) {
                        protectedAction.accept(size - y - 1, size - x - 1);
                        protectedAction.accept(y, x);
                        protectedAction.accept(size - x - 1, size - y - 1);
                    } else {
                        protectedAction.accept(y, x);
                        protectedAction.accept(size - y - 1, size - x - 1);
                        protectedAction.accept(size - x - 1, size - y - 1);
                    }
                }
            }
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
                        if (inBounds(shiftX, shiftY, size)) {
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
                            if (inBounds(shiftX, shiftY, size)) {
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
                    if (inBounds(shiftX, shiftY, otherSize)) {
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
            symPoints.forEach(point -> action.accept((int) point.x(), (int) point.y()));
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

    protected void loopInSymmetryRegion(SymmetryType symmetryType, BiIntConsumer maskAction) {
        assertNotPipelined();
        int maxX = getMaxXBound(symmetryType);
        IntUnaryOperator minYBoundFunction = getMinYBoundFunction(symmetryType);
        IntUnaryOperator maxYBoundFunction = getMaxYBoundFunction(symmetryType);
        for (int x = 0; x < maxX; x++) {
            int minY = minYBoundFunction.applyAsInt(x);
            int maxY = maxYBoundFunction.applyAsInt(x);
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
        return copy(getName() + COPY_NAME + copyCount.getAndIncrement());
    }

    public U getFinalMask() {
        return (U) this;
    }

    private U copy(String maskName) {
        Class<?> clazz = getClass();
        try {
            U copy = (U) clazz.getDeclaredConstructor(clazz, String.class).newInstance(this, maskName);
            copy.setVisualDebug(isVisualDebug());
            return copy;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public U startVisualDebugger() {
        return startVisualDebugger(name == null ? toString() : name);
    }

    public U startVisualDebugger(String maskName) {
        visualName = maskName;
        visualDebug = true;
        visible = true;
        show();
        return (U) this;
    }

    public U show() {
        if (!Pipeline.isAccepting() && (isVisualDebug() && visible)) {
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
                case Z ->
                        fillRect(0, 0, extent / 2, size, value).fillRect(size - extent / 2, 0, size - extent / 2, size,
                                                                         value);
                case X ->
                        fillRect(0, 0, size, extent / 2, value).fillRect(0, size - extent / 2, size, extent / 2, value);
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
                case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13,
                     POINT14, POINT15, POINT16 ->
                        fillCircle((float) size / 2, (float) size / 2, extent * 3 / 4f, value);
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
        return fillCircle(center.x(), center.z(), radius, value);
    }

    public U fillCircle(Vector2 center, float radius, T value) {
        return fillCircle(center.x(), center.y(), radius, value);
    }

    public U fillCircle(float x, float y, float radius, T value) {
        return enqueue(() -> fillArc(x, y, 0, 360, radius, value));
    }

    public U fillArc(float x, float y, float startAngle, float endAngle, float radius, T value) {
        return enqueue(() -> {
            int size = getSize();
            float dx;
            float dy;
            float radius2 = (radius + .5f) * (radius + .5f);
            float radiansToDegreeFactor = (float) (180 / StrictMath.PI);
            for (int cx = StrictMath.round(x - radius); cx < StrictMath.round(x + radius + 1); cx++) {
                for (int cy = StrictMath.round(y - radius); cy < StrictMath.round(y + radius + 1); cy++) {
                    dx = x - cx;
                    dy = y - cy;
                    float angle = (float) (StrictMath.atan2(dy, dx) / radiansToDegreeFactor + 360) % 360;
                    if (inBounds(cx, cy, size)
                        && dx * dx + dy * dy <= radius2
                        && angle >= startAngle
                        && angle <= endAngle) {
                        set(cx, cy, value);
                    }
                }
            }
        });
    }

    public U fillSquare(Vector2 topLeft, int extent, T value) {
        return fillSquare((int) topLeft.x(), (int) topLeft.y(), extent, value);
    }

    public U fillSquare(int x, int y, int extent, T value) {
        return enqueue(() -> fillRect(x, y, extent, extent, value));
    }

    public U fillRect(Vector2 topLeft, int width, int height, T value) {
        return fillRect((int) topLeft.x(), (int) topLeft.y(), width, height, value);
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
        return fillParallelogram((int) topLeft.x(), (int) topLeft.y(), width, height, xSlope, ySlope, value);
    }

    public U fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, T value) {
        return enqueue(() -> {
            int size = getSize();
            for (int px = 0; px < width; px++) {
                for (int py = 0; py < height; py++) {
                    int calcX = x + px + py * xSlope;
                    int calcY = y + py + px * ySlope;
                    if (inBounds(calcX, calcY, size)) {
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
                location -> applyAtSymmetryPoints((int) location.x(), (int) location.y(), SymmetryType.SPAWN,
                                                  (x, y) -> set(x, y, value)));
        return (U) this;
    }
}
