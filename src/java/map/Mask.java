package map;

import lombok.Getter;
import util.Vector2f;
import util.Vector3f;

@Getter
public strictfp abstract class Mask {
    protected SymmetryHierarchy symmetryHierarchy;

    abstract void startVisualDebugger(String maskName);

    abstract int getSize();

    public boolean inBounds(Vector3f location) {
        return inBounds(new Vector2f(location));
    }

    public boolean inBounds(Vector2f location) {
        return inBounds((int) location.x, (int) location.y);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < getSize() && y >= 0 && y < getSize();
    }

    public Vector2f getSymmetryPoint(Vector3f v) {
        return getSymmetryPoint(new Vector2f(v));
    }

    public Vector2f getSymmetryPoint(Vector2f v) {
        return getSymmetryPoint(v, symmetryHierarchy.getSpawnSymmetry());
    }

    public Vector2f getSymmetryPoint(Vector2f v, Symmetry symmetry) {
        return getSymmetryPoint(v.x, v.y, symmetry);
    }

    public Vector2f getSymmetryPoint(float x, float y) {
        return getSymmetryPoint(x, y, symmetryHierarchy.getSpawnSymmetry());
    }

    public Vector2f getSymmetryPoint(float x, float y, Symmetry symmetry) {
        return switch (symmetry) {
            case POINT -> new Vector2f(getSize() - x - 1, getSize() - y - 1);
            case X -> new Vector2f(getSize() - x - 1, y);
            case Z -> new Vector2f(x, getSize() - y - 1);
            case XZ -> new Vector2f(y, x);
            case ZX -> new Vector2f(getSize() - y - 1, getSize() - x - 1);
            default -> null;
        };
    }

    public Vector2f[] getTerrainSymmetryPoints(Vector2f location) {
        return getTerrainSymmetryPoints(location.x, location.y, symmetryHierarchy.getTerrainSymmetry());
    }

    public Vector2f[] getTerrainSymmetryPoints(Vector2f location, Symmetry symmetry) {
        return getTerrainSymmetryPoints(location.x, location.y, symmetry);
    }

    public Vector2f[] getTerrainSymmetryPoints(float x, float y, Symmetry symmetry) {
        Vector2f[] symmetryPoints;
        Vector2f symPoint1;
        Vector2f symPoint2;
        Vector2f symPoint3;
        switch (symmetry) {
            case POINT, Z, X, XZ, ZX -> {
                symPoint1 = getSymmetryPoint(x, y);
                symmetryPoints = new Vector2f[]{symPoint1};
            }
            case QUAD -> {
                symPoint1 = getSymmetryPoint(x, y, Symmetry.Z);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.X);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.X);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
            }
            case DIAG -> {
                symPoint1 = getSymmetryPoint(x, y, Symmetry.ZX);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.XZ);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.XZ);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
            }
            default -> symmetryPoints = new Vector2f[0];
        }
        return symmetryPoints;
    }

    public float getReflectedRotation(float rot) {
        return getReflectedRotation(rot, symmetryHierarchy.getSpawnSymmetry());
    }

    public float getReflectedRotation(float rot, Symmetry symmetry) {
        return switch (symmetry) {
            case POINT -> rot + (float) StrictMath.PI;
            case X -> (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
            case Z -> (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
            case XZ, ZX -> (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
            default -> rot;
        };
    }

    public int getMinXBound() {
        return getMinXBound(symmetryHierarchy.getTerrainSymmetry());
    }

    public int getMinXBound(Symmetry symmetry) {
        return switch (symmetry) {
            default -> 0;
        };
    }

    public int getMaxXBound() {
        return getMaxXBound(symmetryHierarchy.getTerrainSymmetry());
    }

    public int getMaxXBound(Symmetry symmetry) {
        return switch (symmetry) {
            case POINT -> getMaxXBound(symmetryHierarchy.getTeamSymmetry());
            case X, QUAD, DIAG -> getSize() / 2 + 1;
            default -> getSize();
        };
    }

    public int getMinYBound(int x) {
        return getMinYBound(x, symmetryHierarchy.getTerrainSymmetry());
    }

    public int getMinYBound(int x, Symmetry symmetry) {
        return switch (symmetry) {
            case DIAG -> x;
            default -> 0;
        };
    }

    public int getMaxYBound(int x) {
        return getMaxYBound(x, symmetryHierarchy.getTerrainSymmetry());
    }

    public int getMaxYBound(int x, Symmetry symmetry) {
        return switch (symmetry) {
            case POINT -> getMaxYBound(x, symmetryHierarchy.getTeamSymmetry());
            case XZ -> x + 1;
            case ZX, DIAG -> getSize() - x;
            case Z -> getSize() / 2 + 1;
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
        return (x >= getMinXBound() && x < getMaxXBound() && y >= getMinYBound(x) && y < getMaxYBound(x)) ^ reverse && inBounds(x, y);
    }
}
