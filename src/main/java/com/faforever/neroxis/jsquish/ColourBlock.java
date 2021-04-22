/* -----------------------------------------------------------------------------

	Copyright (c) 2006 Simon Brown                          si@sjbrown.co.uk

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	"Software"), to	deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be included
	in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
	OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   -------------------------------------------------------------------------- */

package com.faforever.neroxis.jsquish;

import java.util.Arrays;

import static com.faforever.neroxis.jsquish.CompressorColourFit.*;
import static java.lang.Math.round;

final strictfp class ColourBlock {

    private static final int[] remapped = new int[16];

    private static final int[] indices = new int[16];

    private static final int[] codes = new int[16];

    private ColourBlock() {
    }

    static int gammaColour(final float colour, final float scale) {
        //return round(scale * (float)Math.pow(colour, 1.0 / 2.2));
        return round(scale * colour);
    }

    private static int floatTo565(final Vec colour) {
        // get the components in the correct range
        final int r = round(GRID_X * colour.x());
        final int g = round(GRID_Y * colour.y());
        final int b = round(GRID_Z * colour.z());

        // pack into a single value
        return (r << 11) | (g << 5) | b;
    }

    private static void writeColourBlock(final int a, final int b, final int[] indices, final byte[] block, final int offset) {
        // write the endpoints
        block[offset] = (byte) (a & 0xFF);
        block[offset + 1] = (byte) ((a >> 8) & 0xFF);
        block[offset + 2] = (byte) (b & 0xff);
        block[offset + 3] = (byte) ((b >> 8) & 0xFF);

        // write the indices
        for (int i = 0; i < 4; ++i) {
            final int index = 4 * i;
            block[offset + 4 + i] = (byte) (indices[index] | (indices[index + 1] << 2) | (indices[index + 2] << 4) | (indices[index + 3] << 6));
        }
    }

    static void writeColourBlock3(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
        // get the packed values
        int a = floatTo565(start);
        int b = floatTo565(end);

        // remap the indices
        if (a <= b) {
            // use the indices directly
            System.arraycopy(indices, 0, remapped, 0, 16);
        } else {
            // swap a and b
            final int tmp = a;
            a = b;
            b = tmp;
            for (int i = 0; i < 16; ++i) {
                if (indices[i] == 0)
                    remapped[i] = 1;
                else if (indices[i] == 1)
                    remapped[i] = 0;
                else
                    remapped[i] = indices[i];
            }
        }

        // write the block
        writeColourBlock(a, b, remapped, block, offset);
    }

    static void writeColourBlock4(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
        // get the packed values
        int a = floatTo565(start);
        int b = floatTo565(end);

        // remap the indices

        if (a < b) {
            // swap a and b
            final int tmp = a;
            a = b;
            b = tmp;
            for (int i = 0; i < 16; ++i)
                remapped[i] = (indices[i] ^ 0x1) & 0x3;
        } else if (a == b) {
            // use index 0
            Arrays.fill(remapped, 0);
        } else {
            // use the indices directly
            System.arraycopy(indices, 0, remapped, 0, 16);
        }

        // write the block
        writeColourBlock(a, b, remapped, block, offset);
    }

    static void decompressColour(final byte[] rgba, final byte[] block, final int offset, final boolean isDXT1) {
        // unpack the endpoints
        final int[] codes = ColourBlock.codes;

        final int color0 = unpack565(block, offset, codes, 0);
        final int color1 = unpack565(block, offset + 2, codes, 4);

        // generate the midpoints
        final int color2;
        final int color3;

        if (color0 <= color1) {
            color2 = (color0 + color1) / 2;
            color3 = 0;
        } else {
            color2 = (2 * color0 + color1) / 3;
            color3 = (color0 + 2 * color1) / 3;
        }

        unpack565(color2, codes, 8);
        unpack565(color3, codes, 12);

        // fill in alpha for the intermediate values
        codes[12 + 3] = (color0 <= color1) ? 0 : 255;

        // unpack the indices
        final int[] indices = ColourBlock.indices;

        for (int i = 0; i < 4; ++i) {
            final int index = 4 * i;
            final int packed = (block[offset + 4 + i]);

            indices[index] = packed & 0x3;
            indices[index + 1] = (packed >> 2) & 0x3;
            indices[index + 2] = (packed >> 4) & 0x3;
            indices[index + 3] = (packed >> 6) & 0x3;
        }

        // store out the colours
        for (int i = 0; i < 16; ++i) {
            final int index = 4 * indices[i];
            for (int j = 0; j < 4; ++j)
                rgba[4 * i + j] = (byte) codes[index + j];
        }
    }

    private static int unpack565(final byte[] packed, final int pOffset, final int[] colour, final int cOffset) {
        // build the packed value
        int value = (packed[pOffset] & 0xFF) | ((packed[pOffset + 1] & 0xFF) << 8);

        // get the components in the stored range
        int red = (value >> 11) & 0x1f;
        int green = (value >> 5) & 0x3f;
        int blue = value & 0x1f;

        // scale up to 8 bits
        colour[cOffset] = (red << 3) | (red >> 2);
        colour[cOffset + 1] = (green << 2) | (green >> 4);
        colour[cOffset + 2] = (blue << 3) | (blue >> 2);
        colour[cOffset + 3] = 255;

        // return the value
        return value;
    }

    private static void unpack565(final int value, final int[] colour, final int cOffset) {

        // get the components in the stored range
        int red = (value >> 11) & 0x1f;
        int green = (value >> 5) & 0x3f;
        int blue = value & 0x1f;

        // scale up to 8 bits
        colour[cOffset] = (red << 3) | (red >> 2);
        colour[cOffset + 1] = (green << 2) | (green >> 4);
        colour[cOffset + 2] = (blue << 3) | (blue >> 2);
        colour[cOffset + 3] = 255;
    }
}