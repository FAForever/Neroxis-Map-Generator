package map;

import lombok.Getter;
import util.Vector2f;

@Getter
public strictfp abstract class Mask {
    protected int width;
    protected int height;
    protected Symmetry symmetry;

    public Vector2f getSymmetryPoint(Vector2f v) {
        return getSymmetryPoint((int) v.x, (int) v.y);
    }

    public Vector2f getSymmetryPoint(int x, int y) {
        switch (symmetry) {
            case POINT:
                return new Vector2f(width - x - 1, height - y - 1);
            case X:
                return new Vector2f(width - x - 1, y);
            case Y:
                return new Vector2f(x, height - y - 1);
            case XY:
                return new Vector2f(y, x);
            case YX:
                return new Vector2f(width - y - 1, height - x - 1);
            default:
                return null;
        }
    }
}
