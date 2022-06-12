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
