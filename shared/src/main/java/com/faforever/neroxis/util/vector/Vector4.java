package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.util.functional.FloatConsumer;
import com.faforever.neroxis.util.functional.FloatSupplier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vector4 extends Vector<Vector4> {
    private float x;
    private float y;
    private float z;
    private float w;

    public Vector4(Vector4 other) {
        this(other.getX(), other.getY(), other.getZ(), other.getW());
    }

    @Override
    protected FloatSupplier getComponentGetter(int i) {
        return switch (i) {
            case Vector.X -> this::getX;
            case Vector.Y -> this::getY;
            case Vector.Z -> this::getZ;
            case Vector.W -> this::getW;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    protected FloatConsumer getComponentSetter(int i) {
        return switch (i) {
            case Vector.X -> this::setX;
            case Vector.Y -> this::setY;
            case Vector.Z -> this::setZ;
            case Vector.W -> this::setW;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public int getDimension() {
        return 4;
    }

    @Override
    public float[] toArray() {
        return new float[]{x, y, z, w};
    }

    @Override
    public Vector4 copy() {
        return new Vector4(this);
    }
}
