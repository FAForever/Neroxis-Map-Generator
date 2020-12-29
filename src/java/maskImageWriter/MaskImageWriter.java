package maskImageWriter;

import lombok.Data;
import map.FloatMask;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Data
public class MaskImageWriter {
    private FloatMask redMask;
    private FloatMask greenMask;
    private FloatMask blueMask;

    public MaskImageWriter(FloatMask redMask, FloatMask greenMask, FloatMask blueMask, float scaleMultiplier, String path) throws IOException {
        int size = redMask.getSize();
        if (size != greenMask.getSize() || size != blueMask.getSize()) {
            throw new IllegalArgumentException("Masks not the same size: redMask is " + redMask.getSize() +
                    ", greenMask is " + greenMask.getSize() + " and blueMask is " + blueMask.getSize());
        }
        ArrayList<Byte> byteArrayList = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                byte redByte = (byte) ((Number) (redMask.getValueAt(x, y) * scaleMultiplier)).intValue();
                byte greenByte = (byte) ((Number) (greenMask.getValueAt(x, y) * scaleMultiplier)).intValue();
                byte blueByte = (byte) ((Number) (blueMask.getValueAt(x, y) * scaleMultiplier)).intValue();
                byteArrayList.add(redByte);
                byteArrayList.add(greenByte);
                byteArrayList.add(blueByte);
            }
        }
        Byte[] aComplexByteArray = byteArrayList.toArray(new Byte[0]);
        int length = aComplexByteArray.length;
        final byte[] aByteArray = new byte[length];
        for (int i = 0; i < length; i++) {
            aByteArray[i] = aComplexByteArray[i];
        }
        DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);
        WritableRaster raster = Raster.createInterleavedRaster(buffer, size, size, 3 * size, 3, new int[]{0, 1, 2}, new Point(0, 0));
        ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        RenderedImage image = (RenderedImage) new BufferedImage(cm, raster, true, null);
        ImageIO.write(image, "png", new File(path));
        System.out.println("PNG created at " + path);
    }

    public MaskImageWriter(FloatMask mask, float scaleMultiplier, String path) throws IOException {
        new MaskImageWriter(mask, mask, mask, scaleMultiplier, path);
    }
}
