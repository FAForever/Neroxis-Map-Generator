package map;

import lombok.Getter;
import util.Vector2f;
import util.Vector3f;

@Getter
public strictfp abstract class Mask {
    abstract void startVisualDebugger();
    abstract int getSize();

    protected SymmetryHierarchy symmetryHierarchy;

    public Vector2f getSymmetryPoint(Vector3f v) {
        return getSymmetryPoint(new Vector2f(v));
    }

    public Vector2f getSymmetryPoint(Vector2f v) {
        return getSymmetryPoint(v, symmetryHierarchy.getSpawnSymmetry());
    }

    public Vector2f getSymmetryPoint(Vector2f v, Symmetry symmetry) {
        return getSymmetryPoint(StrictMath.round(v.x), StrictMath.round(v.y), symmetry);
    }

    public Vector2f getSymmetryPoint(int x, int y) {
        return getSymmetryPoint(x, y, symmetryHierarchy.getSpawnSymmetry());
    }

    public Vector2f getSymmetryPoint(int x, int y, Symmetry symmetry) {
        switch (symmetry) {
            case POINT:
                return new Vector2f(getSize() - x - 1, getSize() - y - 1);
            case X:
                return new Vector2f(getSize() - x - 1, y);
            case Y:
                return new Vector2f(x, getSize() - y - 1);
            case XY:
                return new Vector2f(y, x);
            case YX:
                return new Vector2f(getSize() - y - 1, getSize() - x - 1);
            default:
                return null;
        }
    }

    public Vector2f[] getTerrainSymmetryPoints(int x, int y, Symmetry symmetry) {
        Vector2f[] symmetryPoints;
        Vector2f symPoint1;
        Vector2f symPoint2;
        Vector2f symPoint3;
        switch (symmetry) {
            case POINT:
            case Y:
            case X:
            case XY:
            case YX:
                symPoint1 = getSymmetryPoint(x, y);
                symmetryPoints = new Vector2f[]{symPoint1};
                break;
            case QUAD:
                symPoint1 = getSymmetryPoint(x, y, Symmetry.Y);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.X);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.X);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
                break;
            case DIAG:
                symPoint1 = getSymmetryPoint(x, y, Symmetry.YX);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.XY);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.XY);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
                break;
            default:
                symmetryPoints = null;
                break;
        }
        return symmetryPoints;
    }

    public float getReflectedRotation(float rot) {
        return getReflectedRotation(rot, symmetryHierarchy.getSpawnSymmetry());
    }

    public float getReflectedRotation(float rot, Symmetry symmetry) {
        switch (symmetry) {
            case POINT:
                return rot + (float) StrictMath.PI;
            case X:
                return (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
            case Y:
                return (float) StrictMath.atan2(StrictMath.sin(rot), -StrictMath.cos(rot));
            case XY:
            case YX:
                return (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
            default:
                return rot;
        }
    }

    public int getMinXBound(Symmetry symmetry) {
        switch (symmetry) {
            default:
                return 0;
        }
    }

    public int getMaxXBound(Symmetry symmetry) {
        switch (symmetry) {
            case X:
            case QUAD:
            case DIAG:
                return getSize() / 2;
            default:
                return getSize();
        }
    }

    public int getMinYBound(int x, Symmetry symmetry) {
        switch (symmetry) {
            case DIAG:
                return x;
            default:
                return 0;
        }
    }

    public int getMaxYBound(int x, Symmetry symmetry) {
        switch (symmetry) {
            case X:
                return getSize();
            case XY:
                return x + 1;
            case YX:
            case DIAG:
                return getSize() - x;
            default:
                return getSize() / 2;
        }
    }
}
