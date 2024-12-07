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
public final class Vector4 extends Vector<Vector4> {
    private float x;
    private float y;
    private float z;
    private float w;

    public Vector4(Vector4 other) {
        this(other.getX(), other.getY(), other.getZ(), other.getW());
    }

    @Override
    protected VectorComponentGetter<Vector4> getComponentGetter(int i) {
        return switch (i) {
            case Vector.X -> Vector4::getX;
            case Vector.Y -> Vector4::getY;
            case Vector.Z -> Vector4::getZ;
            case Vector.W -> Vector4::getW;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    protected VectorComponentSetter<Vector4> getComponentSetter(int i) {
        return switch (i) {
            case Vector.X -> Vector4::setX;
            case Vector.Y -> Vector4::setY;
            case Vector.Z -> Vector4::setZ;
            case Vector.W -> Vector4::setW;
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
