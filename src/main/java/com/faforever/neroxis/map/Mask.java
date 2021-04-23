package com.faforever.neroxis.map;

import com.faforever.neroxis.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
@ToString(onlyExplicitlyIncluded = true)
public strictfp abstract class Mask<T, U extends Mask<T, U>> {
    @Getter
    protected final SymmetrySettings symmetrySettings;
    @Getter
    @ToString.Include
    private final String name;
    protected final Random random;
    @ToString.Include
    protected int plannedSize;
    @Getter
    @Setter
    private boolean parallel;
    @Getter
    @Setter
    private boolean processing;
    protected T[][] mask;

    protected Mask(Long seed, SymmetrySettings symmetrySettings, String name) {
        this(seed, symmetrySettings, name, false);
    }

    protected Mask(Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        this.symmetrySettings = symmetrySettings;
        this.name = name;
        this.parallel = parallel;
        this.processing = false;
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
    }

    public abstract String toHash() throws NoSuchAlgorithmException;

    protected abstract T[][] getEmptyMask(int size);

    public abstract U interpolate();

    public abstract U blur(int size);

    public abstract U copy();

    protected abstract int[][] getInnerCount();

    protected void calculateInnerValue(int[][] innerCount, int x, int y, int val) {
        innerCount[x][y] = val;
        innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
        innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
        innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
    }

    protected static int getShiftedValue(int val, int offset, int size, boolean wrapEdges) {
        return wrapEdges ? (val + offset + size) % size : val + offset - 1;
    }

    public T getFinalValueAt(Vector3f location) {
        return getFinalValueAt((int) location.getX(), (int) location.getZ());
    }

    public T getFinalValueAt(Vector2f location) {
        return getFinalValueAt((int) location.getX(), (int) location.getY());
    }

    public T getFinalValueAt(int x, int y) {
        Pipeline.await(this);
        return mask[x][y];
    }

    public T getValueAt(Vector3f location) {
        return getValueAt((int) location.getX(), (int) location.getZ());
    }

    public T getValueAt(Vector2f location) {
        return getValueAt((int) location.getX(), (int) location.getY());
    }

    public T getValueAt(int x, int y) {
        return mask[x][y];
    }

    protected void setValueAt(Vector2f location, T value) {
        setValueAt((int) location.getX(), (int) location.getY(), value);
    }

    protected void setValueAt(int x, int y, T value) {
        mask[x][y] = value;
    }

    public int getSize() {
        if (parallel && !processing) {
            return plannedSize;
        } else {
            return mask[0].length;
        }
    }

    public int getImmediateSize() {
        return mask[0].length;
    }

    protected float calculateAreaAverage(int radius, int x, int y, int[][] innerCount) {
        int xLeft = StrictMath.max(0, x - radius);
        int size = getSize();
        int xRight = StrictMath.min(size - 1, x + radius);
        int yUp = StrictMath.max(0, y - radius);
        int yDown = StrictMath.min(size - 1, y + radius);
        int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
        int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
        int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
        int countD = innerCount[xRight][yDown];
        int count = countD + countA - countB - countC;
        int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
        return (float) count / area;
    }

    public U setSize(int newSize) {
        plannedSize = newSize;
        enqueue(() -> {
            int size = getSize();
            if (size < newSize) {
                enlarge(newSize);
            } else if (size > newSize) {
                shrink(newSize);
            }
        });
        return (U) this;
    }

    public U resample(int newSize) {
        plannedSize = newSize;
        enqueue(() -> {
            int size = getSize();
            if (size < newSize) {
                interpolate(newSize);
            } else if (size > newSize) {
                decimate(newSize);
            }
        });
        return (U) this;
    }

    public boolean inBounds(Vector2f location) {
        return inBounds((int) location.getX(), (int) location.getY());
    }

    public boolean inBounds(int x, int y) {
        int size = getSize();
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public boolean onBoundary(Vector2f location) {
        return onBoundary((int) location.getX(), (int) location.getY());
    }

    public boolean onBoundary(int x, int y) {
        int size = getSize();
        return x == 0 || x == size - 1 || y == 0 || y == size - 1;
    }

    public List<Vector2f> getSymmetryPoints(Vector3f v, SymmetryType symmetryType) {
        return getSymmetryPoints(new Vector2f(v), symmetryType);
    }

    public List<Vector2f> getSymmetryPoints(Vector2f v, SymmetryType symmetryType) {
        return getSymmetryPoints(v.getX(), v.getY(), symmetryType);
    }

    public List<Vector2f> getSymmetryPoints(float x, float y, SymmetryType symmetryType) {
        List<Vector2f> symmetryPoints = getSymmetryPointsWithOutOfBounds(x, y, symmetryType);
        symmetryPoints.removeIf(point -> !inBounds(point));
        return symmetryPoints;
    }

    public List<Vector2f> getSymmetryPointsWithOutOfBounds(Vector3f v, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(new Vector2f(v), symmetryType);
    }

    public List<Vector2f> getSymmetryPointsWithOutOfBounds(Vector2f v, SymmetryType symmetryType) {
        return getSymmetryPointsWithOutOfBounds(v.getX(), v.getY(), symmetryType);
    }

    public List<Vector2f> getSymmetryPointsWithOutOfBounds(float x, float y, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int numSymPoints = symmetry.getNumSymPoints();
        List<Vector2f> symmetryPoints = new ArrayList<>(numSymPoints - 1);
        int size = getSize();
        switch (symmetry) {
            case POINT2:
                symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                break;
            case POINT4:
                symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                symmetryPoints.add(new Vector2f(y, size - x - 1));
                symmetryPoints.add(new Vector2f(size - y - 1, x));
                break;
            case POINT6:
            case POINT8:
            case POINT10:
            case POINT12:
            case POINT14:
            case POINT16:
                symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                for (int i = 1; i < numSymPoints / 2; i++) {
                    float angle = (float) (2 * StrictMath.PI * i / numSymPoints);
                    Vector2f rotated = getRotatedPoint(x, y, angle);
                    symmetryPoints.add(rotated);
                    Vector2f antiRotated = getRotatedPoint(x, y, (float) (angle + StrictMath.PI));
                    symmetryPoints.add(antiRotated);
                }
                break;
            case POINT3:
            case POINT5:
            case POINT7:
            case POINT9:
            case POINT11:
            case POINT13:
            case POINT15:
                for (int i = 1; i < numSymPoints; i++) {
                    Vector2f rotated = getRotatedPoint(x, y, (float) (2 * StrictMath.PI * i / numSymPoints));
                    symmetryPoints.add(rotated);
                }
                break;
            case X:
                symmetryPoints.add(new Vector2f(size - x - 1, y));
                break;
            case Z:
                symmetryPoints.add(new Vector2f(x, size - y - 1));
                break;
            case XZ:
                symmetryPoints.add(new Vector2f(y, x));
                break;
            case ZX:
                symmetryPoints.add(new Vector2f(size - y - 1, size - x - 1));
                break;
            case QUAD:
                if (symmetrySettings.getTeamSymmetry() == Symmetry.Z) {
                    symmetryPoints.add(new Vector2f(x, size - y - 1));
                    symmetryPoints.add(new Vector2f(size - x - 1, y));
                    symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                } else {
                    symmetryPoints.add(new Vector2f(size - x - 1, y));
                    symmetryPoints.add(new Vector2f(x, size - y - 1));
                    symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                }
                break;
            case DIAG:
                if (symmetrySettings.getTeamSymmetry() == Symmetry.ZX) {
                    symmetryPoints.add(new Vector2f(size - y - 1, size - x - 1));
                    symmetryPoints.add(new Vector2f(y, x));
                    symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                } else {
                    symmetryPoints.add(new Vector2f(y, x));
                    symmetryPoints.add(new Vector2f(size - y - 1, size - x - 1));
                    symmetryPoints.add(new Vector2f(size - x - 1, size - y - 1));
                }
                break;
        }
        return symmetryPoints;
    }

    public ArrayList<Float> getSymmetryRotation(float rot) {
        return getSymmetryRotation(rot, SymmetryType.SPAWN);
    }

    private Vector2f getRotatedPoint(float x, float y, float angle) {
        float halfSize = getSize() / 2f;
        float xOffset = x - halfSize;
        float yOffset = y - halfSize;
        double cosAngle = StrictMath.cos(angle);
        double sinAngle = StrictMath.sin(angle);
        float newX = (float) (xOffset * cosAngle - yOffset * sinAngle + halfSize);
        float newY = (float) (xOffset * sinAngle + yOffset * cosAngle + halfSize);
        return new Vector2f(newX, newY);
    }

    protected int getMinXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        switch (symmetry) {
            default:
                return 0;
        }
    }

    public ArrayList<Float> getSymmetryRotation(float rot, SymmetryType symmetryType) {
        ArrayList<Float> symmetryRotation = new ArrayList<>();
        final float xRotation = (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
        final float zRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
        final float diagRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        Symmetry teamSymmetry = symmetrySettings.getTeamSymmetry();
        switch (symmetry) {
            case POINT2:
            case Z:
            case X:
                symmetryRotation.add(rot + (float) StrictMath.PI);
                break;
            case POINT4:
                symmetryRotation.add(rot + (float) StrictMath.PI);
                symmetryRotation.add(rot + (float) StrictMath.PI / 2);
                symmetryRotation.add(rot - (float) StrictMath.PI / 2);
                break;
            case POINT3:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                int numSymPoints = symmetry.getNumSymPoints();
                for (int i = 1; i < numSymPoints; i++) {
                    symmetryRotation.add(rot + (float) (2 * StrictMath.PI * i / numSymPoints));
                }
                break;
            case XZ:
            case ZX:
                symmetryRotation.add(diagRotation);
                break;
            case QUAD:
                if (teamSymmetry == Symmetry.Z) {
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
                break;
            case DIAG:
                if (teamSymmetry == Symmetry.ZX) {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
                break;
        }
        return symmetryRotation;
    }

    protected int getMinYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        switch (symmetry) {
            case POINT2:
            case POINT3:
            case POINT4:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                return getMinYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case DIAG:
            case XZ:
                return x;
            default:
                return 0;
        }
    }

    protected int getMaxXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        switch (symmetry) {
            case POINT3:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                return StrictMath.max(getMaxXFromAngle(360f / symmetry.getNumSymPoints()), size / 2 + 1);
            case POINT4:
            case X:
            case QUAD:
            case DIAG:
                return size / 2 + 1;
            default:
                return size;
        }
    }

    protected int getMaxYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        int size = getSize();
        switch (symmetry) {
            case POINT3:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                return getMaxYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case ZX:
            case DIAG:
                return size - x;
            case Z:
            case POINT2:
            case POINT4:
            case QUAD:
                return size / 2 + 1;
            default:
                return size;
        }
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

    public boolean inTeam(Vector3f pos, boolean reverse) {
        return inTeam(new Vector2f(pos), reverse);
    }

    public boolean inTeam(Vector2f pos, boolean reverse) {
        return inTeam((int) pos.getX(), (int) pos.getY(), reverse);
    }

    public boolean inTeam(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM) && x < getMaxXBound(SymmetryType.TEAM) && y >= getMinYBound(x, SymmetryType.TEAM) && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse && inBounds(x, y);
    }

    public boolean inTeamNoBounds(Vector3f pos, boolean reverse) {
        return inTeam(new Vector2f(pos), reverse);
    }

    public boolean inTeamNoBounds(Vector2f pos, boolean reverse) {
        return inTeam((int) pos.getX(), (int) pos.getY(), reverse);
    }

    public boolean inTeamNoBounds(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM) && x < getMaxXBound(SymmetryType.TEAM) && y >= getMinYBound(x, SymmetryType.TEAM) && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse;
    }

    public boolean inHalf(Vector3f pos, float angle) {
        return inHalf(new Vector2f(pos), angle);
    }

    public boolean inHalf(int x, int y, float angle) {
        return inHalf(new Vector2f(x, y), angle);
    }

    public boolean inHalf(Vector2f pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2f(halfSize, halfSize).getAngle(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle) && inBounds(pos);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle) && inBounds(pos);
        }
    }

    public boolean inHalfNoBounds(Vector3f pos, float angle) {
        return inHalfNoBounds(new Vector2f(pos), angle);
    }

    public boolean inHalfNoBounds(int x, int y, float angle) {
        return inHalfNoBounds(new Vector2f(x, y), angle);
    }

    public boolean inHalfNoBounds(Vector2f pos, float angle) {
        float halfSize = getSize() / 2f;
        float vectorAngle = (float) ((new Vector2f(halfSize, halfSize).getAngle(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        float adjustedAngle = (angle + 180f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < adjustedAngle);
        } else {
            return (vectorAngle >= angle && vectorAngle < adjustedAngle);
        }
    }

    public void applySymmetry(SymmetryType symmetryType) {
        applySymmetry(symmetryType, false);
    }

    public void applySymmetry(SymmetryType symmetryType, boolean reverse) {
        enqueue(() -> {
            applyWithSymmetry(symmetryType, (x, y) -> {
                Vector2f location = new Vector2f(x, y);
                List<Vector2f> symPoints = getSymmetryPoints(location, symmetryType);
                symPoints.forEach(symmetryPoint -> {
                    if (reverse) {
                        setValueAt(location, getValueAt(symmetryPoint));
                    } else {
                        setValueAt(symmetryPoint, getValueAt(location));
                    }
                });
            });
            if (!symmetrySettings.getSymmetry(symmetryType).isPerfectSymmetry()) {
                interpolate();
            }
        });
    }

    public void applySymmetry(float angle) {
        enqueue(() -> {
            if (symmetrySettings.getSymmetry(SymmetryType.SPAWN) != Symmetry.POINT2) {
                throw new IllegalArgumentException("Spawn Symmetry must equal POINT2");
            }
            apply((x, y) -> {
                if (inHalf(x, y, angle)) {
                    Vector2f location = new Vector2f(x, y);
                    List<Vector2f> symPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
                    symPoints.forEach(symmetryPoint -> setValueAt(symmetryPoint, getValueAt(location)));
                }
            });
        });
    }

    private U enlarge(int size) {
        return enlarge(size, SymmetryType.SPAWN);
    }

    private U shrink(int size) {
        return shrink(size, SymmetryType.SPAWN);
    }

    private U enlarge(int newSize, SymmetryType symmetryType) {
        T[][] smallMask = mask;
        int oldSize = getSize();
        float scale = (float) newSize / oldSize;
        mask = getEmptyMask(newSize);
        modifyWithSymmetry(symmetryType, (x, y) -> {
            int smallX = StrictMath.min((int) (x / scale), oldSize - 1);
            int smallY = StrictMath.min((int) (y / scale), oldSize - 1);
            return smallMask[smallX][smallY];
        });
        return (U) this;
    }

    private U shrink(int newSize, SymmetryType symmetryType) {
        T[][] largeMask = mask;
        int oldSize = getSize();
        float scale = (float) oldSize / newSize;
        mask = getEmptyMask(newSize);
        modifyWithSymmetry(symmetryType, (x, y) -> {
            int largeX = StrictMath.min(StrictMath.round(x * scale + scale / 2), oldSize - 1);
            int largeY = StrictMath.min(StrictMath.round(y * scale + scale / 2), oldSize - 1);
            return largeMask[largeX][largeY];
        });
        return (U) this;
    }

    private U interpolate(int newSize) {
        return interpolate(newSize, SymmetryType.SPAWN);
    }

    private U interpolate(int newSize, SymmetryType symmetryType) {
        int oldSize = getSize();
        enlarge(newSize, symmetryType);
        blur(StrictMath.round((float) newSize / oldSize / 2));
        return (U) this;
    }

    private U decimate(int newSize) {
        return decimate(newSize, SymmetryType.SPAWN);
    }

    private U decimate(int newSize, SymmetryType symmetryType) {
        int oldSize = getSize();
        blur(StrictMath.round((float) oldSize / newSize / 2));
        shrink(newSize, symmetryType);
        return (U) this;
    }

    public U flip(SymmetryType symmetryType) {
        enqueue(() -> {
            Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
            if (symmetry.getNumSymPoints() != 2) {
                throw new IllegalArgumentException("Cannot flip non single axis symmetry");
            }
            int size = getSize();
            T[][] newMask = getEmptyMask(size);
            apply((x, y) -> {
                List<Vector2f> symmetryPoints = getSymmetryPoints(x, y, symmetryType);
                newMask[x][y] = getValueAt(symmetryPoints.get(0));
            });
            this.mask = newMask;
        });
        return (U) this;
    }

    protected void modify(BiFunction<Integer, Integer, T> valueFunction) {
        int size = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                setValueAt(x, y, valueFunction.apply(x, y));
            }
        }
    }

    protected void modifyWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, T> valueFunction) {
        int minX = getMinXBound(symmetryType);
        int maxX = getMaxXBound(symmetryType);
        for (int x = minX; x < maxX; x++) {
            int minY = getMinYBound(x, symmetryType);
            int maxY = getMaxYBound(x, symmetryType);
            for (int y = minY; y < maxY; y++) {
                T value = valueFunction.apply(x, y);
                setValueAt(x, y, value);
                List<Vector2f> symPoints = getSymmetryPoints(x, y, symmetryType);
                symPoints.forEach(symmetryPoint -> setValueAt(symmetryPoint, value));
            }
        }
    }

    protected void apply(BiConsumer<Integer, Integer> maskAction) {
        int size = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                maskAction.accept(x, y);
            }
        }
    }

    protected void applyWithSymmetry(SymmetryType symmetryType, BiConsumer<Integer, Integer> maskAction) {
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

    protected void enqueue(Runnable function) {
        enqueue((ignored) -> function.run());
    }

    protected void enqueue(Consumer<List<Mask<?, ?>>> function, Mask<?, ?>... usedMasks) {
        List<Mask<?, ?>> dependencies = Arrays.asList(usedMasks);
        if (parallel && !processing) {
            if (dependencies.stream().anyMatch(dep -> !dep.isParallel() || dep.isProcessing())) {
                throw new IllegalArgumentException("Non parallel masks used as dependents");
            }
            Pipeline.add(this, dependencies, function);
        } else {
            function.accept(dependencies);
            String callingMethod = Util.getStackTraceMethodInPackage("com.faforever.neroxis.map", "enqueue");
            VisualDebugger.visualizeMask(this, callingMethod);
        }
    }

    protected void assertCompatibleMask(Mask<?, ?> other) {
        int otherSize = other.getSize();
        int size = getSize();
        if (otherSize != size) {
            throw new IllegalArgumentException("Masks not the same size: other is " + otherSize + " and Mask is " + size);
        }
        if (!getSymmetrySettings().equals(other.getSymmetrySettings())) {
            throw new IllegalArgumentException("Masks not the same symmetry: other is " + other.getSymmetrySettings() + " and Mask is " + getSymmetrySettings());
        }
    }

    protected void assertSmallerSize(int size) {
        int actualSize = getSize();
        if (size > actualSize) {
            throw new IllegalArgumentException("Intended mask size is larger than base mask size: Mask is " + actualSize + " and size is " + size);
        }
    }

    public void assertNotParallel() {
        if (parallel && !processing) {
            throw new IllegalStateException("Mask not finished processing results will not be deterministic");
        }
    }

    public U getFinalMask() {
        Pipeline.await(this);
        return copy();
    }

    public U startVisualDebugger() {
        return startVisualDebugger(name == null ? toString() : name, Util.getStackTraceParentClass());
    }

    public U startVisualDebugger(String maskName) {
        return startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public U startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
        return (U) this;
    }

    public void show() {
        VisualDebugger.visualizeMask(this, "show");
    }
}
