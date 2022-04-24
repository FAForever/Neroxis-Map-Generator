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

final strictfp class SingleColourLookup4 {

    static final int[][][] LOOKUP_5_4 = {{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}}, {{0, 0, 1}, {0, 0, 1}, {0, 1, 1}, {1, 0, 1}}, {{0, 0, 2}, {0, 0, 2}, {0, 1, 0}, {1, 0, 0}}, {{0, 0, 3}, {0, 0, 3}, {0, 1, 1}, {1, 0, 1}}, {{0, 0, 4}, {0, 0, 4}, {0, 2, 1}, {0, 1, 1}}, {{1, 0, 3}, {0, 1, 3}, {0, 2, 0}, {0, 1, 0}}, {{1, 0, 2}, {0, 1, 2}, {0, 2, 1}, {0, 1, 1}}, {{1, 0, 1}, {0, 1, 1}, {0, 3, 1}, {1, 1, 1}}, {{1, 0, 0}, {0, 1, 0}, {0, 3, 0}, {1, 1, 0}}, {{1, 0, 1}, {0, 1, 1}, {1, 2, 1}, {0, 2, 1}}, {{1, 0, 2}, {0, 1, 2}, {1, 2, 0}, {0, 2, 0}}, {{1, 0, 3}, {0, 1, 3}, {0, 4, 0}, {4, 0, 0}}, {{1, 0, 4}, {0, 1, 4}, {0, 5, 1}, {1, 2, 1}}, {{2, 0, 3}, {0, 2, 3}, {0, 5, 0}, {1, 2, 0}}, {{2, 0, 2}, {0, 2, 2}, {0, 5, 1}, {1, 2, 1}}, {{2, 0, 1}, {0, 2, 1}, {0, 6, 1}, {0, 3, 1}}, {{2, 0, 0}, {0, 2, 0}, {0, 6, 0}, {0, 3, 0}}, {{2, 0, 1}, {0, 2, 1}, {2, 3, 1}, {1, 3, 1}}, {{2, 0, 2}, {0, 2, 2}, {2, 3, 0}, {1, 3, 0}}, {{2, 0, 3}, {0, 2, 3}, {0, 7, 0}, {5, 1, 0}}, {{2, 0, 4}, {0, 2, 4}, {1, 6, 1}, {2, 3, 1}}, {{3, 0, 3}, {0, 3, 3}, {1, 6, 0}, {2, 3, 0}}, {{3, 0, 2}, {0, 3, 2}, {0, 8, 0}, {0, 4, 0}}, {{3, 0, 1}, {0, 3, 1}, {0, 9, 1}, {1, 4, 1}}, {{3, 0, 0}, {0, 3, 0}, {0, 9, 0}, {1, 4, 0}}, {{3, 0, 1}, {0, 3, 1}, {0, 9, 1}, {1, 4, 1}}, {{3, 0, 2}, {0, 3, 2}, {0, 10, 1}, {0, 5, 1}}, {{3, 0, 3}, {0, 3, 3}, {0, 10, 0}, {0, 5, 0}}, {{3, 0, 4}, {0, 3, 4}, {2, 7, 1}, {5, 3, 1}}, {{4, 0, 4}, {0, 4, 4}, {2, 7, 0}, {5, 3, 0}}, {{4, 0, 3}, {0, 4, 3}, {0, 11, 0}, {1, 5, 0}}, {{4, 0, 2}, {0, 4, 2}, {1, 10, 1}, {0, 6, 1}}, {{4, 0, 1}, {0, 4, 1}, {1, 10, 0}, {0, 6, 0}}, {{4, 0, 0}, {0, 4, 0}, {0, 12, 0}, {4, 4, 0}}, {{4, 0, 1}, {0, 4, 1}, {0, 13, 1}, {1, 6, 1}}, {{4, 0, 2}, {0, 4, 2}, {0, 13, 0}, {1, 6, 0}}, {{4, 0, 3}, {0, 4, 3}, {0, 13, 1}, {1, 6, 1}}, {{4, 0, 4}, {0, 4, 4}, {0, 14, 1}, {0, 7, 1}}, {{5, 0, 3}, {0, 5, 3}, {0, 14, 0}, {0, 7, 0}}, {{5, 0, 2}, {0, 5, 2}, {2, 11, 1}, {1, 7, 1}}, {{5, 0, 1}, {0, 5, 1}, {2, 11, 0}, {1, 7, 0}}, {{5, 0, 0}, {0, 5, 0}, {0, 15, 0}, {5, 5, 0}}, {{5, 0, 1}, {0, 5, 1}, {1, 14, 1}, {2, 7, 1}}, {{5, 0, 2}, {0, 5, 2}, {1, 14, 0}, {2, 7, 0}}, {{5, 0, 3}, {0, 5, 3}, {0, 16, 0}, {0, 8, 0}}, {{5, 0, 4}, {0, 5, 4}, {0, 17, 1}, {1, 8, 1}}, {{6, 0, 3}, {0, 6, 3}, {0, 17, 0}, {1, 8, 0}}, {{6, 0, 2}, {0, 6, 2}, {0, 17, 1}, {1, 8, 1}}, {{6, 0, 1}, {0, 6, 1}, {0, 18, 1}, {0, 9, 1}}, {{6, 0, 0}, {0, 6, 0}, {0, 18, 0}, {0, 9, 0}}, {{6, 0, 1}, {0, 6, 1}, {2, 15, 1}, {5, 7, 1}}, {{6, 0, 2}, {0, 6, 2}, {2, 15, 0}, {5, 7, 0}}, {{6, 0, 3}, {0, 6, 3}, {0, 19, 0}, {1, 9, 0}}, {{6, 0, 4}, {0, 6, 4}, {1, 18, 1}, {0, 10, 1}}, {{7, 0, 3}, {0, 7, 3}, {1, 18, 0}, {0, 10, 0}}, {{7, 0, 2}, {0, 7, 2}, {0, 20, 0}, {4, 8, 0}}, {{7, 0, 1}, {0, 7, 1}, {0, 21, 1}, {1, 10, 1}}, {{7, 0, 0}, {0, 7, 0}, {0, 21, 0}, {1, 10, 0}}, {{7, 0, 1}, {0, 7, 1}, {0, 21, 1}, {1, 10, 1}}, {{7, 0, 2}, {0, 7, 2}, {0, 22, 1}, {0, 11, 1}}, {{7, 0, 3}, {0, 7, 3}, {0, 22, 0}, {0, 11, 0}}, {{7, 0, 4}, {0, 7, 4}, {2, 19, 1}, {1, 11, 1}}, {{8, 0, 4}, {0, 8, 4}, {2, 19, 0}, {1, 11, 0}}, {{8, 0, 3}, {0, 8, 3}, {0, 23, 0}, {5, 9, 0}}, {{8, 0, 2}, {0, 8, 2}, {1, 22, 1}, {2, 11, 1}}, {{8, 0, 1}, {0, 8, 1}, {1, 22, 0}, {2, 11, 0}}, {{8, 0, 0}, {0, 8, 0}, {0, 24, 0}, {0, 12, 0}}, {{8, 0, 1}, {0, 8, 1}, {0, 25, 1}, {1, 12, 1}}, {{8, 0, 2}, {0, 8, 2}, {0, 25, 0}, {1, 12, 0}}, {{8, 0, 3}, {0, 8, 3}, {0, 25, 1}, {1, 12, 1}}, {{8, 0, 4}, {0, 8, 4}, {0, 26, 1}, {0, 13, 1}}, {{9, 0, 3}, {0, 9, 3}, {0, 26, 0}, {0, 13, 0}}, {{9, 0, 2}, {0, 9, 2}, {2, 23, 1}, {5, 11, 1}}, {{9, 0, 1}, {0, 9, 1}, {2, 23, 0}, {5, 11, 0}}, {{9, 0, 0}, {0, 9, 0}, {0, 27, 0}, {1, 13, 0}}, {{9, 0, 1}, {0, 9, 1}, {1, 26, 1}, {0, 14, 1}}, {{9, 0, 2}, {0, 9, 2}, {1, 26, 0}, {0, 14, 0}}, {{9, 0, 3}, {0, 9, 3}, {0, 28, 0}, {4, 12, 0}}, {{9, 0, 4}, {0, 9, 4}, {0, 29, 1}, {1, 14, 1}}, {{10, 0, 3}, {0, 10, 3}, {0, 29, 0}, {1, 14, 0}}, {{10, 0, 2}, {0, 10, 2}, {0, 29, 1}, {1, 14, 1}}, {{10, 0, 1}, {0, 10, 1}, {0, 30, 1}, {0, 15, 1}}, {{10, 0, 0}, {0, 10, 0}, {0, 30, 0}, {0, 15, 0}}, {{10, 0, 1}, {0, 10, 1}, {2, 27, 1}, {1, 15, 1}}, {{10, 0, 2}, {0, 10, 2}, {2, 27, 0}, {1, 15, 0}}, {{10, 0, 3}, {0, 10, 3}, {0, 31, 0}, {5, 13, 0}}, {{10, 0, 4}, {0, 10, 4}, {1, 30, 1}, {2, 15, 1}}, {{11, 0, 3}, {0, 11, 3}, {1, 30, 0}, {2, 15, 0}}, {{11, 0, 2}, {0, 11, 2}, {4, 24, 0}, {0, 16, 0}}, {{11, 0, 1}, {0, 11, 1}, {1, 31, 1}, {1, 16, 1}}, {{11, 0, 0}, {0, 11, 0}, {1, 31, 0}, {1, 16, 0}}, {{11, 0, 1}, {0, 11, 1}, {1, 31, 1}, {1, 16, 1}}, {{11, 0, 2}, {0, 11, 2}, {2, 30, 1}, {0, 17, 1}}, {{11, 0, 3}, {0, 11, 3}, {2, 30, 0}, {0, 17, 0}}, {{11, 0, 4}, {0, 11, 4}, {2, 31, 1}, {5, 15, 1}}, {{12, 0, 4}, {0, 12, 4}, {2, 31, 0}, {5, 15, 0}}, {{12, 0, 3}, {0, 12, 3}, {4, 27, 0}, {1, 17, 0}}, {{12, 0, 2}, {0, 12, 2}, {3, 30, 1}, {0, 18, 1}}, {{12, 0, 1}, {0, 12, 1}, {3, 30, 0}, {0, 18, 0}}, {{12, 0, 0}, {0, 12, 0}, {4, 28, 0}, {4, 16, 0}}, {{12, 0, 1}, {0, 12, 1}, {3, 31, 1}, {1, 18, 1}}, {{12, 0, 2}, {0, 12, 2}, {3, 31, 0}, {1, 18, 0}}, {{12, 0, 3}, {0, 12, 3}, {3, 31, 1}, {1, 18, 1}}, {{12, 0, 4}, {0, 12, 4}, {4, 30, 1}, {0, 19, 1}}, {{13, 0, 3}, {0, 13, 3}, {4, 30, 0}, {0, 19, 0}}, {{13, 0, 2}, {0, 13, 2}, {6, 27, 1}, {1, 19, 1}}, {{13, 0, 1}, {0, 13, 1}, {6, 27, 0}, {1, 19, 0}}, {{13, 0, 0}, {0, 13, 0}, {4, 31, 0}, {5, 17, 0}}, {{13, 0, 1}, {0, 13, 1}, {5, 30, 1}, {2, 19, 1}}, {{13, 0, 2}, {0, 13, 2}, {5, 30, 0}, {2, 19, 0}}, {{13, 0, 3}, {0, 13, 3}, {8, 24, 0}, {0, 20, 0}}, {{13, 0, 4}, {0, 13, 4}, {5, 31, 1}, {1, 20, 1}}, {{14, 0, 3}, {0, 14, 3}, {5, 31, 0}, {1, 20, 0}}, {{14, 0, 2}, {0, 14, 2}, {5, 31, 1}, {1, 20, 1}}, {{14, 0, 1}, {0, 14, 1}, {6, 30, 1}, {0, 21, 1}}, {{14, 0, 0}, {0, 14, 0}, {6, 30, 0}, {0, 21, 0}}, {{14, 0, 1}, {0, 14, 1}, {6, 31, 1}, {5, 19, 1}}, {{14, 0, 2}, {0, 14, 2}, {6, 31, 0}, {5, 19, 0}}, {{14, 0, 3}, {0, 14, 3}, {8, 27, 0}, {1, 21, 0}}, {{14, 0, 4}, {0, 14, 4}, {7, 30, 1}, {0, 22, 1}}, {{15, 0, 3}, {0, 15, 3}, {7, 30, 0}, {0, 22, 0}}, {{15, 0, 2}, {0, 15, 2}, {8, 28, 0}, {4, 20, 0}}, {{15, 0, 1}, {0, 15, 1}, {7, 31, 1}, {1, 22, 1}}, {{15, 0, 0}, {0, 15, 0}, {7, 31, 0}, {1, 22, 0}}, {{15, 0, 1}, {0, 15, 1}, {7, 31, 1}, {1, 22, 1}}, {{15, 0, 2}, {0, 15, 2}, {8, 30, 1}, {0, 23, 1}}, {{15, 0, 3}, {0, 15, 3}, {8, 30, 0}, {0, 23, 0}}, {{15, 0, 4}, {0, 15, 4}, {10, 27, 1}, {1, 23, 1}}, {{16, 0, 4}, {0, 16, 4}, {10, 27, 0}, {1, 23, 0}}, {{16, 0, 3}, {0, 16, 3}, {8, 31, 0}, {5, 21, 0}}, {{16, 0, 2}, {0, 16, 2}, {9, 30, 1}, {2, 23, 1}}, {{16, 0, 1}, {0, 16, 1}, {9, 30, 0}, {2, 23, 0}}, {{16, 0, 0}, {0, 16, 0}, {12, 24, 0}, {0, 24, 0}}, {{16, 0, 1}, {0, 16, 1}, {9, 31, 1}, {1, 24, 1}}, {{16, 0, 2}, {0, 16, 2}, {9, 31, 0}, {1, 24, 0}}, {{16, 0, 3}, {0, 16, 3}, {9, 31, 1}, {1, 24, 1}}, {{16, 0, 4}, {0, 16, 4}, {10, 30, 1}, {0, 25, 1}}, {{17, 0, 3}, {0, 17, 3}, {10, 30, 0}, {0, 25, 0}}, {{17, 0, 2}, {0, 17, 2}, {10, 31, 1}, {5, 23, 1}}, {{17, 0, 1}, {0, 17, 1}, {10, 31, 0}, {5, 23, 0}}, {{17, 0, 0}, {0, 17, 0}, {12, 27, 0}, {1, 25, 0}}, {{17, 0, 1}, {0, 17, 1}, {11, 30, 1}, {0, 26, 1}}, {{17, 0, 2}, {0, 17, 2}, {11, 30, 0}, {0, 26, 0}}, {{17, 0, 3}, {0, 17, 3}, {12, 28, 0}, {4, 24, 0}}, {{17, 0, 4}, {0, 17, 4}, {11, 31, 1}, {1, 26, 1}}, {{18, 0, 3}, {0, 18, 3}, {11, 31, 0}, {1, 26, 0}}, {{18, 0, 2}, {0, 18, 2}, {11, 31, 1}, {1, 26, 1}}, {{18, 0, 1}, {0, 18, 1}, {12, 30, 1}, {0, 27, 1}}, {{18, 0, 0}, {0, 18, 0}, {12, 30, 0}, {0, 27, 0}}, {{18, 0, 1}, {0, 18, 1}, {14, 27, 1}, {1, 27, 1}}, {{18, 0, 2}, {0, 18, 2}, {14, 27, 0}, {1, 27, 0}}, {{18, 0, 3}, {0, 18, 3}, {12, 31, 0}, {5, 25, 0}}, {{18, 0, 4}, {0, 18, 4}, {13, 30, 1}, {2, 27, 1}}, {{19, 0, 3}, {0, 19, 3}, {13, 30, 0}, {2, 27, 0}}, {{19, 0, 2}, {0, 19, 2}, {16, 24, 0}, {0, 28, 0}}, {{19, 0, 1}, {0, 19, 1}, {13, 31, 1}, {1, 28, 1}}, {{19, 0, 0}, {0, 19, 0}, {13, 31, 0}, {1, 28, 0}}, {{19, 0, 1}, {0, 19, 1}, {13, 31, 1}, {1, 28, 1}}, {{19, 0, 2}, {0, 19, 2}, {14, 30, 1}, {0, 29, 1}}, {{19, 0, 3}, {0, 19, 3}, {14, 30, 0}, {0, 29, 0}}, {{19, 0, 4}, {0, 19, 4}, {14, 31, 1}, {5, 27, 1}}, {{20, 0, 4}, {0, 20, 4}, {14, 31, 0}, {5, 27, 0}}, {{20, 0, 3}, {0, 20, 3}, {16, 27, 0}, {1, 29, 0}}, {{20, 0, 2}, {0, 20, 2}, {15, 30, 1}, {0, 30, 1}}, {{20, 0, 1}, {0, 20, 1}, {15, 30, 0}, {0, 30, 0}}, {{20, 0, 0}, {0, 20, 0}, {16, 28, 0}, {4, 28, 0}}, {{20, 0, 1}, {0, 20, 1}, {15, 31, 1}, {1, 30, 1}}, {{20, 0, 2}, {0, 20, 2}, {15, 31, 0}, {1, 30, 0}}, {{20, 0, 3}, {0, 20, 3}, {15, 31, 1}, {1, 30, 1}}, {{20, 0, 4}, {0, 20, 4}, {16, 30, 1}, {0, 31, 1}}, {{21, 0, 3}, {0, 21, 3}, {16, 30, 0}, {0, 31, 0}}, {{21, 0, 2}, {0, 21, 2}, {18, 27, 1}, {1, 31, 1}}, {{21, 0, 1}, {0, 21, 1}, {18, 27, 0}, {1, 31, 0}}, {{21, 0, 0}, {0, 21, 0}, {16, 31, 0}, {5, 29, 0}}, {{21, 0, 1}, {0, 21, 1}, {17, 30, 1}, {2, 31, 1}}, {{21, 0, 2}, {0, 21, 2}, {17, 30, 0}, {2, 31, 0}}, {{21, 0, 3}, {0, 21, 3}, {20, 24, 0}, {8, 28, 0}}, {{21, 0, 4}, {0, 21, 4}, {17, 31, 1}, {3, 31, 1}}, {{22, 0, 3}, {0, 22, 3}, {17, 31, 0}, {3, 31, 0}}, {{22, 0, 2}, {0, 22, 2}, {17, 31, 1}, {3, 31, 1}}, {{22, 0, 1}, {0, 22, 1}, {18, 30, 1}, {4, 31, 1}}, {{22, 0, 0}, {0, 22, 0}, {18, 30, 0}, {4, 31, 0}}, {{22, 0, 1}, {0, 22, 1}, {18, 31, 1}, {5, 31, 1}}, {{22, 0, 2}, {0, 22, 2}, {18, 31, 0}, {5, 31, 0}}, {{22, 0, 3}, {0, 22, 3}, {20, 27, 0}, {9, 29, 0}}, {{22, 0, 4}, {0, 22, 4}, {19, 30, 1}, {6, 31, 1}}, {{23, 0, 3}, {0, 23, 3}, {19, 30, 0}, {6, 31, 0}}, {{23, 0, 2}, {0, 23, 2}, {20, 28, 0}, {12, 28, 0}}, {{23, 0, 1}, {0, 23, 1}, {19, 31, 1}, {7, 31, 1}}, {{23, 0, 0}, {0, 23, 0}, {19, 31, 0}, {7, 31, 0}}, {{23, 0, 1}, {0, 23, 1}, {19, 31, 1}, {7, 31, 1}}, {{23, 0, 2}, {0, 23, 2}, {20, 30, 1}, {8, 31, 1}}, {{23, 0, 3}, {0, 23, 3}, {20, 30, 0}, {8, 31, 0}}, {{23, 0, 4}, {0, 23, 4}, {22, 27, 1}, {9, 31, 1}}, {{24, 0, 4}, {0, 24, 4}, {22, 27, 0}, {9, 31, 0}}, {{24, 0, 3}, {0, 24, 3}, {20, 31, 0}, {13, 29, 0}}, {{24, 0, 2}, {0, 24, 2}, {21, 30, 1}, {10, 31, 1}}, {{24, 0, 1}, {0, 24, 1}, {21, 30, 0}, {10, 31, 0}}, {{24, 0, 0}, {0, 24, 0}, {24, 24, 0}, {16, 28, 0}}, {{24, 0, 1}, {0, 24, 1}, {21, 31, 1}, {11, 31, 1}}, {{24, 0, 2}, {0, 24, 2}, {21, 31, 0}, {11, 31, 0}}, {{24, 0, 3}, {0, 24, 3}, {21, 31, 1}, {11, 31, 1}}, {{24, 0, 4}, {0, 24, 4}, {22, 30, 1}, {12, 31, 1}}, {{25, 0, 3}, {0, 25, 3}, {22, 30, 0}, {12, 31, 0}}, {{25, 0, 2}, {0, 25, 2}, {22, 31, 1}, {13, 31, 1}}, {{25, 0, 1}, {0, 25, 1}, {22, 31, 0}, {13, 31, 0}}, {{25, 0, 0}, {0, 25, 0}, {24, 27, 0}, {17, 29, 0}}, {{25, 0, 1}, {0, 25, 1}, {23, 30, 1}, {14, 31, 1}}, {{25, 0, 2}, {0, 25, 2}, {23, 30, 0}, {14, 31, 0}}, {{25, 0, 3}, {0, 25, 3}, {24, 28, 0}, {20, 28, 0}}, {{25, 0, 4}, {0, 25, 4}, {23, 31, 1}, {15, 31, 1}}, {{26, 0, 3}, {0, 26, 3}, {23, 31, 0}, {15, 31, 0}}, {{26, 0, 2}, {0, 26, 2}, {23, 31, 1}, {15, 31, 1}}, {{26, 0, 1}, {0, 26, 1}, {24, 30, 1}, {16, 31, 1}}, {{26, 0, 0}, {0, 26, 0}, {24, 30, 0}, {16, 31, 0}}, {{26, 0, 1}, {0, 26, 1}, {26, 27, 1}, {17, 31, 1}}, {{26, 0, 2}, {0, 26, 2}, {26, 27, 0}, {17, 31, 0}}, {{26, 0, 3}, {0, 26, 3}, {24, 31, 0}, {21, 29, 0}}, {{26, 0, 4}, {0, 26, 4}, {25, 30, 1}, {18, 31, 1}}, {{27, 0, 3}, {0, 27, 3}, {25, 30, 0}, {18, 31, 0}}, {{27, 0, 2}, {0, 27, 2}, {28, 24, 0}, {24, 28, 0}}, {{27, 0, 1}, {0, 27, 1}, {25, 31, 1}, {19, 31, 1}}, {{27, 0, 0}, {0, 27, 0}, {25, 31, 0}, {19, 31, 0}}, {{27, 0, 1}, {0, 27, 1}, {25, 31, 1}, {19, 31, 1}}, {{27, 0, 2}, {0, 27, 2}, {26, 30, 1}, {20, 31, 1}}, {{27, 0, 3}, {0, 27, 3}, {26, 30, 0}, {20, 31, 0}}, {{27, 0, 4}, {0, 27, 4}, {26, 31, 1}, {21, 31, 1}}, {{28, 0, 4}, {0, 28, 4}, {26, 31, 0}, {21, 31, 0}}, {{28, 0, 3}, {0, 28, 3}, {28, 27, 0}, {25, 29, 0}}, {{28, 0, 2}, {0, 28, 2}, {27, 30, 1}, {22, 31, 1}}, {{28, 0, 1}, {0, 28, 1}, {27, 30, 0}, {22, 31, 0}}, {{28, 0, 0}, {0, 28, 0}, {28, 28, 0}, {28, 28, 0}}, {{28, 0, 1}, {0, 28, 1}, {27, 31, 1}, {23, 31, 1}}, {{28, 0, 2}, {0, 28, 2}, {27, 31, 0}, {23, 31, 0}}, {{28, 0, 3}, {0, 28, 3}, {27, 31, 1}, {23, 31, 1}}, {{28, 0, 4}, {0, 28, 4}, {28, 30, 1}, {24, 31, 1}}, {{29, 0, 3}, {0, 29, 3}, {28, 30, 0}, {24, 31, 0}}, {{29, 0, 2}, {0, 29, 2}, {30, 27, 1}, {25, 31, 1}}, {{29, 0, 1}, {0, 29, 1}, {30, 27, 0}, {25, 31, 0}}, {{29, 0, 0}, {0, 29, 0}, {28, 31, 0}, {29, 29, 0}}, {{29, 0, 1}, {0, 29, 1}, {29, 30, 1}, {26, 31, 1}}, {{29, 0, 2}, {0, 29, 2}, {29, 30, 0}, {26, 31, 0}}, {{29, 0, 3}, {0, 29, 3}, {29, 30, 1}, {26, 31, 1}}, {{29, 0, 4}, {0, 29, 4}, {29, 31, 1}, {27, 31, 1}}, {{30, 0, 3}, {0, 30, 3}, {29, 31, 0}, {27, 31, 0}}, {{30, 0, 2}, {0, 30, 2}, {29, 31, 1}, {27, 31, 1}}, {{30, 0, 1}, {0, 30, 1}, {30, 30, 1}, {28, 31, 1}}, {{30, 0, 0}, {0, 30, 0}, {30, 30, 0}, {28, 31, 0}}, {{30, 0, 1}, {0, 30, 1}, {30, 31, 1}, {29, 31, 1}}, {{30, 0, 2}, {0, 30, 2}, {30, 31, 0}, {29, 31, 0}}, {{30, 0, 3}, {0, 30, 3}, {30, 31, 1}, {29, 31, 1}}, {{30, 0, 4}, {0, 30, 4}, {31, 30, 1}, {30, 31, 1}}, {{31, 0, 3}, {0, 31, 3}, {31, 30, 0}, {30, 31, 0}}, {{31, 0, 2}, {0, 31, 2}, {31, 30, 1}, {30, 31, 1}}, {{31, 0, 1}, {0, 31, 1}, {31, 31, 1}, {31, 31, 1}}, {{31, 0, 0}, {0, 31, 0}, {31, 31, 0}, {31, 31, 0}}};
    static final int[][][] LOOKUP_6_4 = {{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}}, {{0, 0, 1}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}}, {{0, 0, 2}, {0, 0, 2}, {0, 2, 0}, {0, 1, 0}}, {{1, 0, 1}, {0, 1, 1}, {0, 3, 1}, {1, 1, 1}}, {{1, 0, 0}, {0, 1, 0}, {0, 3, 0}, {1, 1, 0}}, {{1, 0, 1}, {0, 1, 1}, {0, 4, 0}, {0, 2, 0}}, {{1, 0, 2}, {0, 1, 2}, {0, 5, 0}, {1, 2, 0}}, {{2, 0, 1}, {0, 2, 1}, {0, 6, 1}, {0, 3, 1}}, {{2, 0, 0}, {0, 2, 0}, {0, 6, 0}, {0, 3, 0}}, {{2, 0, 1}, {0, 2, 1}, {0, 7, 0}, {1, 3, 0}}, {{2, 0, 2}, {0, 2, 2}, {0, 8, 0}, {0, 4, 0}}, {{3, 0, 1}, {0, 3, 1}, {0, 9, 1}, {1, 4, 1}}, {{3, 0, 0}, {0, 3, 0}, {0, 9, 0}, {1, 4, 0}}, {{3, 0, 1}, {0, 3, 1}, {0, 10, 0}, {0, 5, 0}}, {{3, 0, 2}, {0, 3, 2}, {0, 11, 0}, {1, 5, 0}}, {{4, 0, 1}, {0, 4, 1}, {0, 12, 1}, {0, 6, 1}}, {{4, 0, 0}, {0, 4, 0}, {0, 12, 0}, {0, 6, 0}}, {{4, 0, 1}, {0, 4, 1}, {0, 13, 0}, {1, 6, 0}}, {{4, 0, 2}, {0, 4, 2}, {0, 14, 0}, {0, 7, 0}}, {{5, 0, 1}, {0, 5, 1}, {0, 15, 1}, {1, 7, 1}}, {{5, 0, 0}, {0, 5, 0}, {0, 15, 0}, {1, 7, 0}}, {{5, 0, 1}, {0, 5, 1}, {0, 16, 0}, {0, 8, 0}}, {{5, 0, 2}, {0, 5, 2}, {1, 15, 0}, {1, 8, 0}}, {{6, 0, 1}, {0, 6, 1}, {0, 17, 0}, {17, 0, 0}}, {{6, 0, 0}, {0, 6, 0}, {0, 18, 0}, {0, 9, 0}}, {{6, 0, 1}, {0, 6, 1}, {0, 19, 0}, {1, 9, 0}}, {{6, 0, 2}, {0, 6, 2}, {3, 14, 0}, {0, 10, 0}}, {{7, 0, 1}, {0, 7, 1}, {0, 20, 0}, {16, 2, 0}}, {{7, 0, 0}, {0, 7, 0}, {0, 21, 0}, {1, 10, 0}}, {{7, 0, 1}, {0, 7, 1}, {0, 22, 0}, {0, 11, 0}}, {{7, 0, 2}, {0, 7, 2}, {4, 15, 0}, {1, 11, 0}}, {{8, 0, 1}, {0, 8, 1}, {0, 23, 0}, {17, 3, 0}}, {{8, 0, 0}, {0, 8, 0}, {0, 24, 0}, {0, 12, 0}}, {{8, 0, 1}, {0, 8, 1}, {0, 25, 0}, {1, 12, 0}}, {{8, 0, 2}, {0, 8, 2}, {6, 14, 0}, {0, 13, 0}}, {{9, 0, 1}, {0, 9, 1}, {0, 26, 0}, {16, 5, 0}}, {{9, 0, 0}, {0, 9, 0}, {0, 27, 0}, {1, 13, 0}}, {{9, 0, 1}, {0, 9, 1}, {0, 28, 0}, {0, 14, 0}}, {{9, 0, 2}, {0, 9, 2}, {7, 15, 0}, {1, 14, 0}}, {{10, 0, 1}, {0, 10, 1}, {0, 29, 0}, {17, 6, 0}}, {{10, 0, 0}, {0, 10, 0}, {0, 30, 0}, {0, 15, 0}}, {{10, 0, 1}, {0, 10, 1}, {0, 31, 0}, {1, 15, 0}}, {{10, 0, 2}, {0, 10, 2}, {9, 14, 0}, {2, 15, 0}}, {{11, 0, 1}, {0, 11, 1}, {0, 32, 0}, {0, 16, 0}}, {{11, 0, 0}, {0, 11, 0}, {0, 33, 0}, {1, 16, 0}}, {{11, 0, 1}, {0, 11, 1}, {2, 30, 0}, {4, 15, 0}}, {{11, 0, 2}, {0, 11, 2}, {0, 34, 0}, {0, 17, 0}}, {{12, 0, 1}, {0, 12, 1}, {0, 35, 0}, {1, 17, 0}}, {{12, 0, 0}, {0, 12, 0}, {0, 36, 0}, {0, 18, 0}}, {{12, 0, 1}, {0, 12, 1}, {3, 31, 0}, {7, 15, 0}}, {{12, 0, 2}, {0, 12, 2}, {0, 37, 0}, {1, 18, 0}}, {{13, 0, 1}, {0, 13, 1}, {0, 38, 0}, {0, 19, 0}}, {{13, 0, 0}, {0, 13, 0}, {0, 39, 0}, {1, 19, 0}}, {{13, 0, 1}, {0, 13, 1}, {5, 30, 0}, {10, 15, 0}}, {{13, 0, 2}, {0, 13, 2}, {0, 40, 0}, {0, 20, 0}}, {{14, 0, 1}, {0, 14, 1}, {0, 41, 0}, {1, 20, 0}}, {{14, 0, 0}, {0, 14, 0}, {0, 42, 0}, {0, 21, 0}}, {{14, 0, 1}, {0, 14, 1}, {6, 31, 0}, {13, 15, 0}}, {{14, 0, 2}, {0, 14, 2}, {0, 43, 0}, {1, 21, 0}}, {{15, 0, 1}, {0, 15, 1}, {0, 44, 0}, {0, 22, 0}}, {{15, 0, 0}, {0, 15, 0}, {0, 45, 0}, {1, 22, 0}}, {{15, 0, 1}, {0, 15, 1}, {8, 30, 0}, {16, 15, 0}}, {{15, 0, 2}, {0, 15, 2}, {0, 46, 0}, {0, 23, 0}}, {{16, 0, 2}, {0, 16, 2}, {0, 47, 0}, {1, 23, 0}}, {{16, 0, 1}, {0, 16, 1}, {1, 46, 0}, {0, 24, 0}}, {{16, 0, 0}, {0, 16, 0}, {0, 48, 0}, {16, 16, 0}}, {{16, 0, 1}, {0, 16, 1}, {0, 49, 0}, {1, 24, 0}}, {{16, 0, 2}, {0, 16, 2}, {0, 50, 0}, {0, 25, 0}}, {{17, 0, 1}, {0, 17, 1}, {2, 47, 0}, {1, 25, 0}}, {{17, 0, 0}, {0, 17, 0}, {0, 51, 0}, {17, 17, 0}}, {{17, 0, 1}, {0, 17, 1}, {0, 52, 0}, {0, 26, 0}}, {{17, 0, 2}, {0, 17, 2}, {0, 53, 0}, {1, 26, 0}}, {{18, 0, 1}, {0, 18, 1}, {4, 46, 0}, {0, 27, 0}}, {{18, 0, 0}, {0, 18, 0}, {0, 54, 0}, {16, 19, 0}}, {{18, 0, 1}, {0, 18, 1}, {0, 55, 0}, {1, 27, 0}}, {{18, 0, 2}, {0, 18, 2}, {0, 56, 0}, {0, 28, 0}}, {{19, 0, 1}, {0, 19, 1}, {5, 47, 0}, {1, 28, 0}}, {{19, 0, 0}, {0, 19, 0}, {0, 57, 0}, {17, 20, 0}}, {{19, 0, 1}, {0, 19, 1}, {0, 58, 0}, {0, 29, 0}}, {{19, 0, 2}, {0, 19, 2}, {0, 59, 0}, {1, 29, 0}}, {{20, 0, 1}, {0, 20, 1}, {7, 46, 0}, {0, 30, 0}}, {{20, 0, 0}, {0, 20, 0}, {0, 60, 0}, {16, 22, 0}}, {{20, 0, 1}, {0, 20, 1}, {0, 61, 0}, {1, 30, 0}}, {{20, 0, 2}, {0, 20, 2}, {0, 62, 0}, {0, 31, 0}}, {{21, 0, 1}, {0, 21, 1}, {8, 47, 0}, {1, 31, 0}}, {{21, 0, 0}, {0, 21, 0}, {0, 63, 0}, {17, 23, 0}}, {{21, 0, 1}, {0, 21, 1}, {1, 62, 0}, {0, 32, 0}}, {{21, 0, 2}, {0, 21, 2}, {1, 63, 0}, {3, 31, 0}}, {{22, 0, 1}, {0, 22, 1}, {10, 46, 0}, {1, 32, 0}}, {{22, 0, 0}, {0, 22, 0}, {2, 62, 0}, {0, 33, 0}}, {{22, 0, 1}, {0, 22, 1}, {2, 63, 0}, {1, 33, 0}}, {{22, 0, 2}, {0, 22, 2}, {3, 62, 0}, {6, 31, 0}}, {{23, 0, 1}, {0, 23, 1}, {11, 47, 0}, {0, 34, 0}}, {{23, 0, 0}, {0, 23, 0}, {3, 63, 0}, {1, 34, 0}}, {{23, 0, 1}, {0, 23, 1}, {4, 62, 0}, {0, 35, 0}}, {{23, 0, 2}, {0, 23, 2}, {4, 63, 0}, {9, 31, 0}}, {{24, 0, 1}, {0, 24, 1}, {13, 46, 0}, {1, 35, 0}}, {{24, 0, 0}, {0, 24, 0}, {5, 62, 0}, {0, 36, 0}}, {{24, 0, 1}, {0, 24, 1}, {5, 63, 0}, {1, 36, 0}}, {{24, 0, 2}, {0, 24, 2}, {6, 62, 0}, {12, 31, 0}}, {{25, 0, 1}, {0, 25, 1}, {14, 47, 0}, {0, 37, 0}}, {{25, 0, 0}, {0, 25, 0}, {6, 63, 0}, {1, 37, 0}}, {{25, 0, 1}, {0, 25, 1}, {7, 62, 0}, {0, 38, 0}}, {{25, 0, 2}, {0, 25, 2}, {7, 63, 0}, {15, 31, 0}}, {{26, 0, 1}, {0, 26, 1}, {16, 45, 0}, {1, 38, 0}}, {{26, 0, 0}, {0, 26, 0}, {8, 62, 0}, {0, 39, 0}}, {{26, 0, 1}, {0, 26, 1}, {8, 63, 0}, {1, 39, 0}}, {{26, 0, 2}, {0, 26, 2}, {9, 62, 0}, {18, 31, 0}}, {{27, 0, 1}, {0, 27, 1}, {16, 48, 0}, {0, 40, 0}}, {{27, 0, 0}, {0, 27, 0}, {9, 63, 0}, {1, 40, 0}}, {{27, 0, 1}, {0, 27, 1}, {10, 62, 0}, {0, 41, 0}}, {{27, 0, 2}, {0, 27, 2}, {10, 63, 0}, {16, 33, 0}}, {{28, 0, 1}, {0, 28, 1}, {16, 51, 0}, {1, 41, 0}}, {{28, 0, 0}, {0, 28, 0}, {11, 62, 0}, {0, 42, 0}}, {{28, 0, 1}, {0, 28, 1}, {11, 63, 0}, {1, 42, 0}}, {{28, 0, 2}, {0, 28, 2}, {12, 62, 0}, {17, 34, 0}}, {{29, 0, 1}, {0, 29, 1}, {16, 54, 0}, {0, 43, 0}}, {{29, 0, 0}, {0, 29, 0}, {12, 63, 0}, {1, 43, 0}}, {{29, 0, 1}, {0, 29, 1}, {13, 62, 0}, {0, 44, 0}}, {{29, 0, 2}, {0, 29, 2}, {13, 63, 0}, {16, 36, 0}}, {{30, 0, 1}, {0, 30, 1}, {16, 57, 0}, {1, 44, 0}}, {{30, 0, 0}, {0, 30, 0}, {14, 62, 0}, {0, 45, 0}}, {{30, 0, 1}, {0, 30, 1}, {14, 63, 0}, {1, 45, 0}}, {{30, 0, 2}, {0, 30, 2}, {15, 62, 0}, {17, 37, 0}}, {{31, 0, 1}, {0, 31, 1}, {16, 60, 0}, {0, 46, 0}}, {{31, 0, 0}, {0, 31, 0}, {15, 63, 0}, {1, 46, 0}}, {{31, 0, 1}, {0, 31, 1}, {24, 46, 0}, {0, 47, 0}}, {{31, 0, 2}, {0, 31, 2}, {16, 62, 0}, {16, 39, 0}}, {{32, 0, 2}, {0, 32, 2}, {16, 63, 0}, {1, 47, 0}}, {{32, 0, 1}, {0, 32, 1}, {17, 62, 0}, {2, 47, 0}}, {{32, 0, 0}, {0, 32, 0}, {25, 47, 0}, {0, 48, 0}}, {{32, 0, 1}, {0, 32, 1}, {17, 63, 0}, {1, 48, 0}}, {{32, 0, 2}, {0, 32, 2}, {18, 62, 0}, {0, 49, 0}}, {{33, 0, 1}, {0, 33, 1}, {18, 63, 0}, {5, 47, 0}}, {{33, 0, 0}, {0, 33, 0}, {27, 46, 0}, {1, 49, 0}}, {{33, 0, 1}, {0, 33, 1}, {19, 62, 0}, {0, 50, 0}}, {{33, 0, 2}, {0, 33, 2}, {19, 63, 0}, {1, 50, 0}}, {{34, 0, 1}, {0, 34, 1}, {20, 62, 0}, {8, 47, 0}}, {{34, 0, 0}, {0, 34, 0}, {28, 47, 0}, {0, 51, 0}}, {{34, 0, 1}, {0, 34, 1}, {20, 63, 0}, {1, 51, 0}}, {{34, 0, 2}, {0, 34, 2}, {21, 62, 0}, {0, 52, 0}}, {{35, 0, 1}, {0, 35, 1}, {21, 63, 0}, {11, 47, 0}}, {{35, 0, 0}, {0, 35, 0}, {30, 46, 0}, {1, 52, 0}}, {{35, 0, 1}, {0, 35, 1}, {22, 62, 0}, {0, 53, 0}}, {{35, 0, 2}, {0, 35, 2}, {22, 63, 0}, {1, 53, 0}}, {{36, 0, 1}, {0, 36, 1}, {23, 62, 0}, {14, 47, 0}}, {{36, 0, 0}, {0, 36, 0}, {31, 47, 0}, {0, 54, 0}}, {{36, 0, 1}, {0, 36, 1}, {23, 63, 0}, {1, 54, 0}}, {{36, 0, 2}, {0, 36, 2}, {24, 62, 0}, {0, 55, 0}}, {{37, 0, 1}, {0, 37, 1}, {24, 63, 0}, {17, 47, 0}}, {{37, 0, 0}, {0, 37, 0}, {32, 47, 0}, {1, 55, 0}}, {{37, 0, 1}, {0, 37, 1}, {25, 62, 0}, {0, 56, 0}}, {{37, 0, 2}, {0, 37, 2}, {25, 63, 0}, {1, 56, 0}}, {{38, 0, 1}, {0, 38, 1}, {26, 62, 0}, {17, 48, 0}}, {{38, 0, 0}, {0, 38, 0}, {32, 50, 0}, {0, 57, 0}}, {{38, 0, 1}, {0, 38, 1}, {26, 63, 0}, {1, 57, 0}}, {{38, 0, 2}, {0, 38, 2}, {27, 62, 0}, {0, 58, 0}}, {{39, 0, 1}, {0, 39, 1}, {27, 63, 0}, {16, 50, 0}}, {{39, 0, 0}, {0, 39, 0}, {32, 53, 0}, {1, 58, 0}}, {{39, 0, 1}, {0, 39, 1}, {28, 62, 0}, {0, 59, 0}}, {{39, 0, 2}, {0, 39, 2}, {28, 63, 0}, {1, 59, 0}}, {{40, 0, 1}, {0, 40, 1}, {29, 62, 0}, {17, 51, 0}}, {{40, 0, 0}, {0, 40, 0}, {32, 56, 0}, {0, 60, 0}}, {{40, 0, 1}, {0, 40, 1}, {29, 63, 0}, {1, 60, 0}}, {{40, 0, 2}, {0, 40, 2}, {30, 62, 0}, {0, 61, 0}}, {{41, 0, 1}, {0, 41, 1}, {30, 63, 0}, {16, 53, 0}}, {{41, 0, 0}, {0, 41, 0}, {32, 59, 0}, {1, 61, 0}}, {{41, 0, 1}, {0, 41, 1}, {31, 62, 0}, {0, 62, 0}}, {{41, 0, 2}, {0, 41, 2}, {31, 63, 0}, {1, 62, 0}}, {{42, 0, 1}, {0, 42, 1}, {32, 61, 0}, {17, 54, 0}}, {{42, 0, 0}, {0, 42, 0}, {32, 62, 0}, {0, 63, 0}}, {{42, 0, 1}, {0, 42, 1}, {32, 63, 0}, {1, 63, 0}}, {{42, 0, 2}, {0, 42, 2}, {41, 46, 0}, {2, 63, 0}}, {{43, 0, 1}, {0, 43, 1}, {33, 62, 0}, {16, 56, 0}}, {{43, 0, 0}, {0, 43, 0}, {33, 63, 0}, {3, 63, 0}}, {{43, 0, 1}, {0, 43, 1}, {34, 62, 0}, {4, 63, 0}}, {{43, 0, 2}, {0, 43, 2}, {42, 47, 0}, {5, 63, 0}}, {{44, 0, 1}, {0, 44, 1}, {34, 63, 0}, {17, 57, 0}}, {{44, 0, 0}, {0, 44, 0}, {35, 62, 0}, {6, 63, 0}}, {{44, 0, 1}, {0, 44, 1}, {35, 63, 0}, {7, 63, 0}}, {{44, 0, 2}, {0, 44, 2}, {44, 46, 0}, {8, 63, 0}}, {{45, 0, 1}, {0, 45, 1}, {36, 62, 0}, {16, 59, 0}}, {{45, 0, 0}, {0, 45, 0}, {36, 63, 0}, {9, 63, 0}}, {{45, 0, 1}, {0, 45, 1}, {37, 62, 0}, {10, 63, 0}}, {{45, 0, 2}, {0, 45, 2}, {45, 47, 0}, {11, 63, 0}}, {{46, 0, 1}, {0, 46, 1}, {37, 63, 0}, {17, 60, 0}}, {{46, 0, 0}, {0, 46, 0}, {38, 62, 0}, {12, 63, 0}}, {{46, 0, 1}, {0, 46, 1}, {38, 63, 0}, {13, 63, 0}}, {{46, 0, 2}, {0, 46, 2}, {47, 46, 0}, {14, 63, 0}}, {{47, 0, 1}, {0, 47, 1}, {39, 62, 0}, {16, 62, 0}}, {{47, 0, 0}, {0, 47, 0}, {39, 63, 0}, {15, 63, 0}}, {{47, 0, 1}, {0, 47, 1}, {40, 62, 0}, {16, 63, 0}}, {{47, 0, 2}, {0, 47, 2}, {48, 46, 0}, {32, 55, 0}}, {{48, 0, 2}, {0, 48, 2}, {40, 63, 0}, {17, 63, 0}}, {{48, 0, 1}, {0, 48, 1}, {41, 62, 0}, {18, 63, 0}}, {{48, 0, 0}, {0, 48, 0}, {41, 63, 0}, {19, 63, 0}}, {{48, 0, 1}, {0, 48, 1}, {48, 49, 0}, {33, 56, 0}}, {{48, 0, 2}, {0, 48, 2}, {42, 62, 0}, {20, 63, 0}}, {{49, 0, 1}, {0, 49, 1}, {42, 63, 0}, {21, 63, 0}}, {{49, 0, 0}, {0, 49, 0}, {43, 62, 0}, {22, 63, 0}}, {{49, 0, 1}, {0, 49, 1}, {48, 52, 0}, {32, 58, 0}}, {{49, 0, 2}, {0, 49, 2}, {43, 63, 0}, {23, 63, 0}}, {{50, 0, 1}, {0, 50, 1}, {44, 62, 0}, {24, 63, 0}}, {{50, 0, 0}, {0, 50, 0}, {44, 63, 0}, {25, 63, 0}}, {{50, 0, 1}, {0, 50, 1}, {48, 55, 0}, {33, 59, 0}}, {{50, 0, 2}, {0, 50, 2}, {45, 62, 0}, {26, 63, 0}}, {{51, 0, 1}, {0, 51, 1}, {45, 63, 0}, {27, 63, 0}}, {{51, 0, 0}, {0, 51, 0}, {46, 62, 0}, {28, 63, 0}}, {{51, 0, 1}, {0, 51, 1}, {48, 58, 0}, {32, 61, 0}}, {{51, 0, 2}, {0, 51, 2}, {46, 63, 0}, {29, 63, 0}}, {{52, 0, 1}, {0, 52, 1}, {47, 62, 0}, {30, 63, 0}}, {{52, 0, 0}, {0, 52, 0}, {47, 63, 0}, {31, 63, 0}}, {{52, 0, 1}, {0, 52, 1}, {48, 61, 0}, {33, 62, 0}}, {{52, 0, 2}, {0, 52, 2}, {48, 62, 0}, {32, 63, 0}}, {{53, 0, 1}, {0, 53, 1}, {56, 47, 0}, {33, 63, 0}}, {{53, 0, 0}, {0, 53, 0}, {48, 63, 0}, {49, 55, 0}}, {{53, 0, 1}, {0, 53, 1}, {49, 62, 0}, {34, 63, 0}}, {{53, 0, 2}, {0, 53, 2}, {49, 63, 0}, {35, 63, 0}}, {{54, 0, 1}, {0, 54, 1}, {58, 46, 0}, {36, 63, 0}}, {{54, 0, 0}, {0, 54, 0}, {50, 62, 0}, {48, 57, 0}}, {{54, 0, 1}, {0, 54, 1}, {50, 63, 0}, {37, 63, 0}}, {{54, 0, 2}, {0, 54, 2}, {51, 62, 0}, {38, 63, 0}}, {{55, 0, 1}, {0, 55, 1}, {59, 47, 0}, {39, 63, 0}}, {{55, 0, 0}, {0, 55, 0}, {51, 63, 0}, {49, 58, 0}}, {{55, 0, 1}, {0, 55, 1}, {52, 62, 0}, {40, 63, 0}}, {{55, 0, 2}, {0, 55, 2}, {52, 63, 0}, {41, 63, 0}}, {{56, 0, 1}, {0, 56, 1}, {61, 46, 0}, {42, 63, 0}}, {{56, 0, 0}, {0, 56, 0}, {53, 62, 0}, {48, 60, 0}}, {{56, 0, 1}, {0, 56, 1}, {53, 63, 0}, {43, 63, 0}}, {{56, 0, 2}, {0, 56, 2}, {54, 62, 0}, {44, 63, 0}}, {{57, 0, 1}, {0, 57, 1}, {62, 47, 0}, {45, 63, 0}}, {{57, 0, 0}, {0, 57, 0}, {54, 63, 0}, {49, 61, 0}}, {{57, 0, 1}, {0, 57, 1}, {55, 62, 0}, {46, 63, 0}}, {{57, 0, 2}, {0, 57, 2}, {55, 63, 0}, {47, 63, 0}}, {{58, 0, 1}, {0, 58, 1}, {56, 62, 1}, {48, 63, 1}}, {{58, 0, 0}, {0, 58, 0}, {56, 62, 0}, {48, 63, 0}}, {{58, 0, 1}, {0, 58, 1}, {56, 63, 0}, {49, 63, 0}}, {{58, 0, 2}, {0, 58, 2}, {57, 62, 0}, {50, 63, 0}}, {{59, 0, 1}, {0, 59, 1}, {57, 63, 1}, {51, 63, 1}}, {{59, 0, 0}, {0, 59, 0}, {57, 63, 0}, {51, 63, 0}}, {{59, 0, 1}, {0, 59, 1}, {58, 62, 0}, {52, 63, 0}}, {{59, 0, 2}, {0, 59, 2}, {58, 63, 0}, {53, 63, 0}}, {{60, 0, 1}, {0, 60, 1}, {59, 62, 1}, {54, 63, 1}}, {{60, 0, 0}, {0, 60, 0}, {59, 62, 0}, {54, 63, 0}}, {{60, 0, 1}, {0, 60, 1}, {59, 63, 0}, {55, 63, 0}}, {{60, 0, 2}, {0, 60, 2}, {60, 62, 0}, {56, 63, 0}}, {{61, 0, 1}, {0, 61, 1}, {60, 63, 1}, {57, 63, 1}}, {{61, 0, 0}, {0, 61, 0}, {60, 63, 0}, {57, 63, 0}}, {{61, 0, 1}, {0, 61, 1}, {61, 62, 0}, {58, 63, 0}}, {{61, 0, 2}, {0, 61, 2}, {61, 63, 0}, {59, 63, 0}}, {{62, 0, 1}, {0, 62, 1}, {62, 62, 1}, {60, 63, 1}}, {{62, 0, 0}, {0, 62, 0}, {62, 62, 0}, {60, 63, 0}}, {{62, 0, 1}, {0, 62, 1}, {62, 63, 0}, {61, 63, 0}}, {{62, 0, 2}, {0, 62, 2}, {63, 62, 0}, {62, 63, 0}}, {{63, 0, 1}, {0, 63, 1}, {63, 63, 1}, {63, 63, 1}}, {{63, 0, 0}, {0, 63, 0}, {63, 63, 0}, {63, 63, 0}}};

    private SingleColourLookup4() {
    }
}