/*
 * MIT License
 *
 * Copyright (c) 2022. Rysefoxx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.github.rysefoxx.util;


import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/12/2022
 */
@UtilityClass
public class SlotUtils {

    /**
     * Convert a row and a column to a slot.
     *
     * @param row    The row
     * @param column The column
     * @return The slot
     */
    public int toSlot(int row, int column) {
        return row * 9 + column;
    }

    /**
     * Converts a slot to a row and column.
     *
     * @param slot The slot to convert to a row and column
     * @return A pair of the row and column. Pair#getLeft() is the row and Pair#getRight() is the column.
     */
    public Pair<Integer, Integer> toRowAndColumn(int slot) {
        return Pair.of(slot / 9, slot % 9);
    }
}
