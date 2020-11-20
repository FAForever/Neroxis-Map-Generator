package map;

import generator.VisualDebugger;
import lombok.Getter;
import util.Util;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.Random;

@Getter
public strictfp abstract class Mask<T> {
    protected final Random random;
    protected T[][] mask;
    protected SymmetrySettings symmetrySettings;

    protected Mask(Long seed) {
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
    }

    protected abstract T[][] getEmptyMask(int size);

    abstract T get(Vector2f location);

    abstract T get(int x, int y);

    abstract void set(Vector2f location, T value);

    abstract void set(int x, int y, T value);

    abstract int getSize();

    public Mask<T> setSize(int size) {
        if (getSize() < size)
            enlarge(size);
        if (getSize() > size) {
            shrink(size);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public boolean inBounds(Vector3f location) {
        return inBounds(new Vector2f(location));
    }

    public boolean inBounds(Vector2f location) {
        return inBounds((int) location.x, (int) location.y);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < getSize() && y >= 0 && y < getSize();
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(Vector3f v) {
        return getSymmetryPoints(new Vector2f(v));
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(Vector2f v) {
        return getSymmetryPoints(v, symmetrySettings.getSpawnSymmetry());
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(Vector2f v, Symmetry symmetry) {
        return getSymmetryPoints(v.x, v.y, symmetry);
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(float x, float y) {
        return getSymmetryPoints(x, y, symmetrySettings.getSpawnSymmetry());
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(float x, float y, Symmetry symmetry) {
        ArrayList<SymmetryPoint> symmetryPoints = new ArrayList<>();
        switch (symmetry) {
            case POINT2 -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.POINT2));
            case POINT4 -> {
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.POINT2));
                symmetryPoints.add(new SymmetryPoint(new Vector2f(y, getSize() - x - 1), Symmetry.POINT2));
                symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, x), Symmetry.POINT2));
            }
            case X -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, y), Symmetry.X));
            case Z -> symmetryPoints.add(new SymmetryPoint(new Vector2f(x, getSize() - y - 1), Symmetry.Z));
            case XZ -> symmetryPoints.add(new SymmetryPoint(new Vector2f(y, x), Symmetry.XZ));
            case ZX -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, getSize() - x - 1), Symmetry.ZX));
            case QUAD -> {
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.Z));
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.X));
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.POINT2));
            }
            case DIAG -> {
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.ZX));
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.XZ));
                symmetryPoints.addAll(getSymmetryPoints(x, y, Symmetry.POINT2));
            }
        }
        return symmetryPoints;
    }

    public ArrayList<Float> getSymmetryRotation(float rot) {
        return getSymmetryRotation(rot, symmetrySettings.getSpawnSymmetry());
    }

    public ArrayList<Float> getSymmetryRotation(float rot, Symmetry symmetry) {
        ArrayList<Float> symmetryRotation = new ArrayList<>();
        switch (symmetry) {
            case POINT2 -> symmetryRotation.add(rot + (float) StrictMath.PI);
            case POINT4 -> {
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.POINT2));
                symmetryRotation.add(rot + (float) StrictMath.PI / 2);
                symmetryRotation.add(rot - (float) StrictMath.PI / 2);
            }
            case X -> symmetryRotation.add((float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot)));
            case Z -> symmetryRotation.add((float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot)));
            case XZ, ZX -> symmetryRotation.add((float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot)));
            case QUAD -> {
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.Z));
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.X));
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.POINT2));
            }
            case DIAG -> {
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.ZX));
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.XZ));
                symmetryRotation.addAll(getSymmetryRotation(rot, Symmetry.POINT2));
            }
        }
        return symmetryRotation;
    }

    public int getMinXBound() {
        return getMinXBound(symmetrySettings.getTerrainSymmetry());
    }

    public int getMinXBound(Symmetry symmetry) {
        return switch (symmetry) {
            default -> 0;
        };
    }

    public int getMaxXBound() {
        return getMaxXBound(symmetrySettings.getTerrainSymmetry());
    }

    public int getMaxXBound(Symmetry symmetry) {
        return switch (symmetry) {
            case POINT2, POINT4 -> getMaxXBound(symmetrySettings.getTeamSymmetry());
            case X, QUAD, DIAG -> getSize() / 2;
            default -> getSize();
        };
    }

    public int getMinYBound(int x) {
        return getMinYBound(x, symmetrySettings.getTerrainSymmetry());
    }

    public int getMinYBound(int x, Symmetry symmetry) {
        return switch (symmetry) {
            case POINT2, POINT4 -> getMinYBound(x, symmetrySettings.getTeamSymmetry());
            case DIAG -> x;
            default -> 0;
        };
    }

    public int getMaxYBound(int x) {
        return getMaxYBound(x, symmetrySettings.getTerrainSymmetry());
    }

    public int getMaxYBound(int x, Symmetry symmetry) {
        return switch (symmetry) {
            case POINT2, POINT4 -> getMaxYBound(x, symmetrySettings.getTeamSymmetry());
            case XZ -> x + 1;
            case ZX, DIAG -> getSize() - x;
            case Z, QUAD -> getSize() / 2;
            default -> getSize();
        };
    }

    public boolean inHalf(Vector3f pos, boolean reverse) {
        return inHalf(new Vector2f(pos), reverse);
    }

    public boolean inHalf(Vector2f pos, boolean reverse) {
        return inHalf((int) pos.x, (int) pos.y, reverse);
    }

    public boolean inHalf(int x, int y, boolean reverse) {
        return (x >= getMinXBound(symmetrySettings.getTeamSymmetry()) && x < getMaxXBound(symmetrySettings.getTeamSymmetry()) && y >= getMinYBound(x, symmetrySettings.getTeamSymmetry()) && y < getMaxYBound(x, symmetrySettings.getTeamSymmetry())) ^ reverse && inBounds(x, y);
    }

    public boolean inHalf(Vector3f pos, float angle) {
        return inHalf(new Vector2f(pos), angle);
    }

    public boolean inHalf(int x, int y, float angle) {
        return inHalf(new Vector2f(x, y), angle);
    }

    public boolean inHalf(Vector2f pos, float angle) {
        float vectorAngle = (float) ((new Vector2f(getSize() / 2f, getSize() / 2f).getAngle(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < (angle + 180f) % 360f) && inBounds(pos);
        } else {
            return (vectorAngle >= angle && vectorAngle < (angle + 180f) % 360f) && inBounds(pos);
        }
    }

    public void applySymmetry() {
        applySymmetry(symmetrySettings.getTerrainSymmetry());
    }

    public void applySymmetry(Symmetry symmetry) {
        applySymmetry(symmetry, false);
    }

    public void applySymmetry(boolean reverse) {
        applySymmetry(symmetrySettings.getTerrainSymmetry(), reverse);
    }

    public void applySymmetry(Symmetry symmetry, boolean reverse) {
        for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
            for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                ArrayList<SymmetryPoint> symPoints = getSymmetryPoints(x, y, symmetry);
                int finalX = x;
                int finalY = y;
                symPoints.forEach(symmetryPoint -> {
                    if (reverse) {
                        set(finalX, finalY, get(symmetryPoint.getLocation()));
                    } else {
                        set(symmetryPoint.getLocation(), get(finalX, finalY));
                    }
                });
            }
        }
    }

    public void applySymmetry(float angle) {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (inHalf(x, y, angle)) {
                    ArrayList<SymmetryPoint> symPoints = getSymmetryPoints(x, y, Symmetry.POINT2);
                    int finalX = x;
                    int finalY = y;
                    symPoints.forEach(symmetryPoint -> set(symmetryPoint.getLocation(), get(finalX, finalY)));
                }
            }
        }
    }

    public Mask<T> enlarge(int size) {
        return enlarge(size, symmetrySettings.getSpawnSymmetry());
    }

    public Mask<T> enlarge(int size, Symmetry symmetry) {
        T[][] largeMask = getEmptyMask(size);
        int smallX;
        int smallY;
        for (int x = 0; x < size; x++) {
            smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
            for (int y = 0; y < size; y++) {
                smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
                largeMask[x][y] = get(smallX, smallY);
            }
        }
        mask = largeMask;
        applySymmetry(symmetry);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> shrink(int size) {
        return shrink(size, symmetrySettings.getSpawnSymmetry());
    }

    public Mask<T> shrink(int size, Symmetry symmetry) {
        T[][] smallMask = getEmptyMask(size);
        int largeX;
        int largeY;
        for (int x = 0; x < size; x++) {
            largeX = (x * getSize()) / size + (getSize() / size / 2);
            if (largeX >= getSize())
                largeX = getSize() - 1;
            for (int y = 0; y < size; y++) {
                largeY = (y * getSize()) / size + (getSize() / size / 2);
                if (largeY >= getSize())
                    largeY = getSize() - 1;
                smallMask[x][y] = get(largeX, largeY);
            }
        }
        mask = smallMask;
        applySymmetry(symmetry);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> flip(Symmetry symmetry) {
        if (symmetry.getNumSymPoints() != 2) {
            throw new IllegalArgumentException("Cannot flip non single axis symmetry");
        }
        T[][] newMask = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(x, y, symmetry);
                newMask[x][y] = get(symmetryPoints.get(0).getLocation());
            }
        }
        this.mask = newMask;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> startVisualDebugger(String maskName) {
        return startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public Mask<T> startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
        return this;
    }

    public void show() {
        VisualDebugger.visualizeMask(this);
    }
}
