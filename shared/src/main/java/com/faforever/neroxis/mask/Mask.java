package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.debugger.VisualDebugger;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class Mask<T, U extends Mask<T, U>> {
    protected static final String MOCK_NAME = "Mock";
    protected static final String COPY_NAME = "Copy";

    @Getter
    private final String name;
    protected final Random random;
    @Getter
    protected SymmetrySettings symmetrySettings;
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

    protected Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        this.symmetrySettings = symmetrySettings;
        this.name = name == null ? String.valueOf(hashCode()) : name;
        this.plannedSize = size;
        this.parallel = parallel;
        random = seed != null ? new Random(seed) : null;
        visible = true;
        enqueue(() -> initializeMask(size));
    }

    protected Mask(U other, String name) {
        this(other.getSize(), (name != null && name.endsWith(MOCK_NAME)) ? null : other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        init(other);
    }

    protected abstract void initializeMask(int size);

    public abstract BufferedImage writeToImage(BufferedImage image);

    public abstract BufferedImage toImage();

    public abstract String toHash() throws NoSuchAlgorithmException;

    protected abstract T getZeroValue();

    protected abstract U fill(T value);

    protected abstract U blur(int radius);

    protected abstract U blur(int radius, BooleanMask other);

    protected static int getShiftedValue(int val, int offset, int size, boolean wrapEdges) {
        return wrapEdges ? (val + offset + size) % size : val + offset;
    }

    public String getVisualName() {
        return visualName != null ? visualName : (name != null ? name : toString());
    }

    public boolean isMock() {
        return (name != null && name.endsWith(MOCK_NAME)) || mock;
    }

    public T get(Vector3 location) {
        return get(StrictMath.round(location.getX()), StrictMath.round(location.getZ()));
    }

    public T get(Vector2 location) {
        return get(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    protected T get(Point point) {
        return get(point.x, point.y);
    }

    protected abstract T get(int x, int y);

    protected void set(Vector3 location, T value) {
        set(StrictMath.round(location.getX()), StrictMath.round(location.getZ()), value);
    }

    protected void set(Vector2 location, T value) {
        set(StrictMath.round(location.getX()), StrictMath.round(location.getY()), value);
    }

    protected void set(Point point, T value) {
        set(point.x, point.y, value);
    }

    protected abstract void set(int x, int y, T value);

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

    public int getSize() {
        if (parallel && !Pipeline.isRunning()) {
            return plannedSize;
        } else {
            return getImmediateSize();
        }
    }

    @GraphMethod
    public U setSize(int newSize) {
        int size = getSize();
        if (newSize != size) {
            plannedSize = newSize;
            return enqueue(() -> setSizeInternal(newSize));
        } else {
            return (U) this;
        }
    }

    protected Long getNextSeed() {
        return random != null ? random.nextLong() : null;
    }

    @SneakyThrows
    @GraphMethod(returnsSelf = false)
    public U copy() {
        return copy(getName() + COPY_NAME);
    }

    @SneakyThrows
    public U copy(String maskName) {
        Class<?> clazz = getClass();
        return (U) clazz.getDeclaredConstructor(clazz, String.class).newInstance(this, maskName);
    }

    @SneakyThrows
    public U mock() {
        Class<?> clazz = getClass();
        Mask<?, U> mock = copy(getName() + MOCK_NAME);
        return mock.enqueue(mock::makeImmutable);
    }

    private void makeImmutable() {
        immutable = true;
        mock = true;
    }

    protected abstract int getImmediateSize();

    @GraphMethod
    public U clear() {
        return fill(getZeroValue());
    }

    protected abstract U setSizeInternal(int newSize);

    @GraphMethod
    public U init(U other) {
        plannedSize = other.getSize();
        return copyFrom(other);
    }

    protected abstract U copyFrom(U other);

    @GraphMethod
    public U init(BooleanMask other, T falseValue, T trueValue) {
        plannedSize = other.getSize();
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            initializeMask(source.getSize());
            set(point -> source.get(point) ? trueValue : falseValue);
        }, other);
    }

    @GraphMethod
    public U setToValue(BooleanMask area, T value) {
        assertCompatibleMask(area);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set(point -> source.get(point) ? value : get(point));
        }, area);
    }

    @GraphMethod
    public U setToValues(BooleanMask area, U values) {
        assertCompatibleMask(area);
        assertCompatibleMask(values);
        return enqueue(dependencies -> {
            BooleanMask placement = (BooleanMask) dependencies.get(0);
            U source = (U) dependencies.get(1);
            set(point -> placement.get(point) ? source.get(point) : get(point));
        }, area, values);
    }

    public boolean inBounds(Vector2 location) {
        return inBounds(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    public boolean inBounds(Point point) {
        return inBounds(point.x, point.y);
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

    public List<Vector2> getSymmetryPointsWithOutOfBounds(Vector3 v, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(new Vector2(v), symmetryType);
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
                if (symmetrySettings.getTeamSymmetry() == Symmetry.Z) {
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
                if (symmetrySettings.getTeamSymmetry() == Symmetry.ZX) {
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

    public ArrayList<Float> getSymmetryRotation(float rot) {
        return getSymmetryRotation(rot, SymmetryType.SPAWN);
    }

    public ArrayList<Float> getSymmetryRotation(float rot, SymmetryType symmetryType) {
        ArrayList<Float> symmetryRotation = new ArrayList<>();
        final float xRotation = (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
        final float zRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
        final float diagRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        Symmetry teamSymmetry = symmetrySettings.getTeamSymmetry();
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

    protected int getMinXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return 0;
    }

    protected int getMinYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> getMinYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case DIAG, XZ -> x;
            default -> 0;
        };
    }

    @GraphMethod
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

    protected int getMaxYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        return switch (symmetry) {
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> getMaxYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case ZX, DIAG -> size - x;
            case Z, POINT2, POINT4, QUAD -> size / 2 + size % 2;
            default -> size;
        };
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

    public boolean inTeam(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM) && x < getMaxXBound(SymmetryType.TEAM) && y >= getMinYBound(x, SymmetryType.TEAM) && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse && inBounds(x, y);
    }

    public boolean inTeamNoBounds(Vector3 pos, boolean reverse) {
        return inTeam(new Vector2(pos), reverse);
    }

    public boolean inTeamNoBounds(Vector2 pos, boolean reverse) {
        return inTeam((int) pos.getX(), (int) pos.getY(), reverse);
    }

    public boolean inTeamNoBounds(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM) && x < getMaxXBound(SymmetryType.TEAM) && y >= getMinYBound(x, SymmetryType.TEAM) && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse;
    }

    public boolean inHalf(Vector3 pos, float angle) {
        return inHalf(new Vector2(pos), angle);
    }

    public boolean inHalf(int x, int y, float angle) {
        return inHalf(new Vector2(x, y), angle);
    }

    public boolean inHalf(Vector2 pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2(halfSize, halfSize).angleTo(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle) && inBounds(pos);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle) && inBounds(pos);
        }
    }

    public boolean inHalfNoBounds(Vector3 pos, float angle) {
        return inHalfNoBounds(new Vector2(pos), angle);
    }

    public boolean inHalfNoBounds(int x, int y, float angle) {
        return inHalfNoBounds(new Vector2(x, y), angle);
    }

    public boolean inHalfNoBounds(Vector2 pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2(halfSize, halfSize).angleTo(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle);
        }
    }

    protected int getMaxXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        return switch (symmetry) {
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> StrictMath.max(getMaxXFromAngle(360f / symmetry.getNumSymPoints()), size / 2 + 1);
            case POINT4, X, QUAD, DIAG -> size / 2;
            default -> size;
        };
    }

    public U forceSymmetry(SymmetryType symmetryType) {
        return forceSymmetry(symmetryType, false);
    }

    public U forceSymmetry(SymmetryType symmetryType, boolean reverse) {
        if (!reverse) {
            return applyWithSymmetry(symmetryType, point -> {
                T value = get(point);
                applyAtSymmetryPoints(point, symmetryType, spoint -> set(spoint, value));
            });
        } else {
            if (symmetrySettings.getSymmetry(symmetryType).getNumSymPoints() != 2) {
                throw new IllegalArgumentException("Symmetry has more than two symmetry points");
            }
            return applyWithSymmetry(symmetryType, point -> {
                List<Vector2> symPoints = getSymmetryPoints(point.x, point.y, symmetryType);
                symPoints.forEach(symmetryPoint -> set(point, get(symmetryPoint)));
            });
        }
    }

    public U forceSymmetry(float angle) {
        if (symmetrySettings.getSymmetry(SymmetryType.SPAWN) != Symmetry.POINT2) {
            throw new IllegalArgumentException("Spawn Symmetry must equal POINT2");
        }
        return apply(point -> {
            if (inHalf(point.x, point.y, angle)) {
                T value = get(point);
                applyAtSymmetryPoints(point, SymmetryType.SPAWN, spoint -> set(spoint, value));
            }
        });
    }

    @GraphMethod
    public U forceSymmetry() {
        return forceSymmetry(SymmetryType.SPAWN);
    }

    protected U set(Function<Point, T> valueFunction) {
        return apply(point -> set(point, valueFunction.apply(point)));
    }

    protected U setWithSymmetry(SymmetryType symmetryType, Function<Point, T> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            T value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, spoint -> set(spoint, value));
        });
    }

    protected U apply(Consumer<Point> maskAction) {
        return enqueue(() -> loop(maskAction));
    }

    protected U applyWithSymmetry(SymmetryType symmetryType, Consumer<Point> maskAction) {
        return enqueue(() -> {
            loopWithSymmetry(symmetryType, maskAction);
            if (!symmetrySettings.getSymmetry(symmetryType).isPerfectSymmetry() && symmetrySettings.getSpawnSymmetry().isPerfectSymmetry()) {
                forceSymmetry(SymmetryType.SPAWN);
            }
        });
    }

    protected U applyAtSymmetryPoints(Vector2 location, SymmetryType symmetryType, Consumer<Point> action) {
        return applyAtSymmetryPoints((int) location.getX(), (int) location.getY(), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(int x, int y, SymmetryType symmetryType, Consumer<Point> action) {
        return applyAtSymmetryPoints(new Point(x, y), symmetryType, action);
    }

    protected U applyAtSymmetryPoints(Point point, SymmetryType symmetryType, Consumer<Point> action) {
        return enqueue(() -> {
            action.accept(point);
            List<Vector2> symPoints = getSymmetryPoints(point.x, point.y, symmetryType);
            symPoints.forEach(symPoint -> {
                point.setLocation(symPoint.getX(), symPoint.getY());
                action.accept(point);
            });
        });
    }

    protected U applyAtSymmetryPointsWithOutOfBounds(Vector2 location, SymmetryType symmetryType, Consumer<Point> action) {
        return applyAtSymmetryPointsWithOutOfBounds((int) location.getX(), (int) location.getY(), symmetryType, action);
    }

    protected U applyAtSymmetryPointsWithOutOfBounds(int x, int y, SymmetryType symmetryType, Consumer<Point> action) {
        return applyAtSymmetryPointsWithOutOfBounds(new Point(x, y), symmetryType, action);
    }

    protected U applyAtSymmetryPointsWithOutOfBounds(Point point, SymmetryType symmetryType, Consumer<Point> action) {
        return enqueue(() -> {
            action.accept(point);
            List<Vector2> symPoints = getSymmetryPointsWithOutOfBounds(point.x, point.y, symmetryType);
            symPoints.forEach(symPoint -> {
                point.setLocation(symPoint.getX(), symPoint.getY());
                action.accept(point);
            });
        });
    }

    protected U applyWithOffset(U other, BiConsumer<Point, T> action, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int biggerSize = StrictMath.max(size, otherSize);
            Map<Integer, Integer> coordinateXMap = new LinkedHashMap<>();
            Map<Integer, Integer> coordinateYMap = new LinkedHashMap<>();
            if (smallerSize == otherSize) {
                if (symmetrySettings.getSpawnSymmetry().isPerfectSymmetry()) {
                    generateCoordinateMaps(xOffset, yOffset, center, wrapEdges, otherSize, size, coordinateXMap, coordinateYMap);
                    other.apply(point -> {
                        int shiftX = coordinateXMap.get(point.x);
                        int shiftY = coordinateYMap.get(point.y);
                        if (inBounds(shiftX, shiftY)) {
                            T value = other.get(point);
                            applyAtSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN, spoint -> action.accept(spoint, value));
                        }
                    });
                } else {
                    applyAtSymmetryPointsWithOutOfBounds(xOffset, yOffset, SymmetryType.SPAWN, spoint -> {
                        generateCoordinateMaps(spoint.x, spoint.y, center, wrapEdges, otherSize, size, coordinateXMap, coordinateYMap);
                        other.apply(point -> {
                            int shiftX = coordinateXMap.get(point.x);
                            int shiftY = coordinateYMap.get(point.y);
                            if (inBounds(shiftX, shiftY)) {
                                action.accept(new Point(shiftX, shiftY), other.get(point));
                            }
                        });
                    });
                }
            } else {
                generateCoordinateMaps(xOffset, yOffset, center, wrapEdges, size, otherSize, coordinateXMap, coordinateYMap);
                apply(point -> {
                    int shiftX = coordinateXMap.get(point.x);
                    int shiftY = coordinateYMap.get(point.y);
                    if (other.inBounds(shiftX, shiftY)) {
                        T value = other.get(shiftX, shiftY);
                        action.accept(point, value);
                    }
                });
            }
        });
    }

    protected void loop(Consumer<Point> maskAction) {
        assertNotPipelined();
        int size = getSize();
        Point point = new Point();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                point.setLocation(x, y);
                maskAction.accept(point);
            }
        }
    }

    protected void loopWithSymmetry(SymmetryType symmetryType, Consumer<Point> maskAction) {
        assertNotPipelined();
        int minX = getMinXBound(symmetryType);
        int maxX = getMaxXBound(symmetryType);
        Point point = new Point();
        for (int x = minX; x < maxX; x++) {
            int minY = getMinYBound(x, symmetryType);
            int maxY = getMaxYBound(x, symmetryType);
            for (int y = minY; y < maxY; y++) {
                point.setLocation(x, y);
                maskAction.accept(point);
            }
        }
    }

    private void generateCoordinateMaps(int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges, int fromSize, int toSize, Map<Integer, Integer> coordinateXMap, Map<Integer, Integer> coordinateYMap) {
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

    protected U enqueue(Runnable function) {
        return enqueue(ignored -> function.run());
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
            if (((DebugUtil.DEBUG && isVisualDebug()) || (DebugUtil.VISUALIZE && !isMock())) && visible) {
                String callingMethod = DebugUtil.getStackTraceMethodInPackage("com.faforever.neroxis.mask", "enqueue", "apply", "applyWithSymmetry");
                String callingLine = DebugUtil.getStackTraceLineInPackage("com.faforever.neroxis.mask", "enqueue", "apply", "applyWithSymmetry");
                VisualDebugger.visualizeMask(this, callingMethod, callingLine);
            }
        }
        return (U) this;
    }

    protected void assertCompatibleMask(Mask<?, ?> other) {
        int otherSize = other.getSize();
        int size = getSize();
        String name = getName();
        String otherName = other.getName();
        if (otherSize != size) {
            throw new IllegalArgumentException(String.format("Masks not the same size: %s is %d and %s is %d", name, size, otherName, otherSize));
        }
        SymmetrySettings symmetrySettings = getSymmetrySettings();
        SymmetrySettings otherSymmetrySettings = other.getSymmetrySettings();
        if (symmetrySettings.getSpawnSymmetry() != Symmetry.NONE && !symmetrySettings.equals(otherSymmetrySettings)) {
            throw new IllegalArgumentException(String.format("Masks not the same symmetry: %s is %s and %s is %s", name, symmetrySettings, otherName, otherSymmetrySettings));
        }
        if (isParallel() && !Pipeline.isRunning() && !other.isParallel()) {
            throw new IllegalArgumentException(String.format("Masks not the same processing chain: %s and %s", name, otherName));
        }
    }

    protected void assertSmallerSize(int size) {
        int actualSize = getSize();
        if (size > actualSize) {
            throw new IllegalArgumentException("Intended mask size is larger than base mask size: Mask is " + actualSize + " and size is " + size);
        }
    }

    protected void assertSize(int size) {
        int actualSize = getSize();
        if (size != actualSize) {
            throw new IllegalArgumentException("Mask size is incorrect: Mask is " + actualSize + " and size is " + size);
        }
    }

    protected void assertMutable() {
        if (immutable) {
            throw new IllegalStateException("Mask is a mock and cannot be modified");
        }
    }

    protected void assertNotPipelined() {
        if (parallel && !Pipeline.isRunning()) {
            throw new IllegalStateException("Mask is pipelined and cannot return an immediate result");
        }
    }

    public U getFinalMask() {
        Pipeline.await(this);
        U finalMask = copy();
        finalMask.setParallel(false);
        return finalMask;
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

    public U fillSides(int extent, T value) {
        return fillSides(extent, value, SymmetryType.TEAM);
    }

    @GraphMethod
    public U fillSides(int extent, T value, SymmetryType symmetryType) {
        return enqueue(() -> {
            int size = getSize();
            switch (symmetrySettings.getSymmetry(symmetryType)) {
                case Z -> fillRect(0, 0, extent / 2, size, value).fillRect(size - extent / 2, 0, size - extent / 2, size, value);
                case X -> fillRect(0, 0, size, extent / 2, value).fillRect(0, size - extent / 2, size, extent / 2, value);
                case XZ -> fillParallelogram(0, 0, size, extent * 3 / 4, 0, -1, value).fillParallelogram(size - extent * 3 / 4, size, size, extent * 3 / 4, 0, -1, value);
                case ZX -> fillParallelogram(size - extent * 3 / 4, 0, extent * 3 / 4, extent * 3 / 4, 1, 0, value).fillParallelogram(-extent * 3 / 4, size - extent * 3 / 4, extent * 3 / 4, extent * 3 / 4, 1, 0, value);
            }
            forceSymmetry(symmetryType);
        });
    }

    public U fillCenter(int extent, T value) {
        return fillCenter(extent, value, SymmetryType.SPAWN);
    }

    @GraphMethod
    public U fillCenter(int extent, T value, SymmetryType symmetryType) {
        return enqueue(() -> {
            int size = getSize();
            switch (symmetrySettings.getSymmetry(symmetryType)) {
                case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> fillCircle((float) size / 2, (float) size / 2, extent * 3 / 4f, value);
                case Z -> fillRect(0, size / 2 - extent / 2, size, extent, value);
                case X -> fillRect(size / 2 - extent / 2, 0, extent, size, value);
                case XZ -> fillDiagonal(extent * 3 / 4, false, value);
                case ZX -> fillDiagonal(extent * 3 / 4, true, value);
                case DIAG -> {
                    if (symmetrySettings.getTeamSymmetry() == Symmetry.DIAG) {
                        fillDiagonal(extent * 3 / 8, false, value);
                        fillDiagonal(extent * 3 / 8, true, value);
                    } else {
                        fillDiagonal(extent * 3 / 16, false, value);
                        fillDiagonal(extent * 3 / 16, true, value);
                        fillCenter(extent, value, SymmetryType.TEAM);
                    }
                }
                case QUAD -> {
                    if (symmetrySettings.getTeamSymmetry() == Symmetry.QUAD) {
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
        coordinates.forEach(location -> applyAtSymmetryPoints(location, SymmetryType.SPAWN, point -> set(point, value)));
        return (U) this;
    }

    @Override
    public String toString() {
        if (name != null) {
            return String.format("Mask(name=%s,size=%d)", name, getSize());
        } else {
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        }
    }
}
