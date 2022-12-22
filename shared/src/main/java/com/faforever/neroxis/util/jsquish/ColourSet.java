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

package com.faforever.neroxis.util.jsquish;

import com.faforever.neroxis.util.jsquish.Squish.CompressionType;

final class ColourSet {
    private final Vec[] points = new Vec[16];
    private final float[] weights = new float[16];
    private final int[] remap = new int[16];
    private int count;
    private boolean transparent;

    ColourSet() {
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vec();
        }
    }

    void init(final byte[] rgba, final int mask, final CompressionType type, final boolean weightAlpha) {
        // check the compression mode for dxt1
        final boolean isDXT1 = type == CompressionType.DXT1;

        count = 0;
        transparent = false;

        // create the minimal set
        for (int i = 0; i < 16; ++i) {
            // check this pixel is enabled
            final int bit = 1 << i;
            if ((mask & bit) == 0) {
                remap[i] = -1;
                continue;
            }

            // check for transparent pixels when using dxt1
            if (isDXT1 && (rgba[4 * i + 3] & 0xFF) < 128) {
                remap[i] = -1;
                transparent = true;
                continue;
            }

            // loop over previous points for a match
            for (int j = 0; ; ++j) {
                // allocate a new point
                if (j == i) {
                    // normalise coordinates to [0,1]
                    final float r = (rgba[4 * i] & 0xFF) / 255.0f;
                    final float g = (rgba[4 * i + 1] & 0xFF) / 255.0f;
                    final float b = (rgba[4 * i + 2] & 0xFF) / 255.0f;

                    // add the point
                    points[count].set(r, g, b);
                    // ensure there is always non-zero weight even for zero alpha
                    weights[count] = (weightAlpha ? ((rgba[4 * i + 3] & 0xFF) + 1) / 256.0f : 1.0f);
                    remap[i] = count++; // advance
                    break;
                }

                // check for a match
                final int oldBit = 1 << j;
                final boolean match = (mask & oldBit) != 0
                                      && rgba[4 * i] == rgba[4 * j]
                                      && rgba[4 * i + 1] == rgba[4
                                                                 * j
                                                                 + 1]
                                      && rgba[4 * i + 2] == rgba[4 * j + 2]
                                      && !isDXT1;

                if (match) {
                    // get the index of the match
                    final int index = remap[j];

                    // ensure there is always non-zero weight even for zero alpha
                    // map to this point and increase the weight
                    weights[index] += (weightAlpha ? ((rgba[4 * i + 3] & 0xFF) + 1) / 256.0f : 1.0f);
                    remap[i] = index;
                    break;
                }
            }
        }
    }

    int getCount() {
        return count;
    }

    Vec[] getPoints() {
        return points;
    }

    float[] getWeights() {
        return weights;
    }

    boolean isTransparent() {
        return transparent;
    }

    void remapIndices(final int[] source, final int[] target) {
        for (int i = 0; i < 16; ++i) {
            int j = remap[i];
            if (j == -1) {
                target[i] = 3;
            } else {
                target[i] = source[j];
            }
        }
    }
}