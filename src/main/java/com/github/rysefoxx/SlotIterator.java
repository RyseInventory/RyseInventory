package com.github.rysefoxx;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;


public class SlotIterator {

    private int slot = -1;
    private int row;
    private int column;
    private SlotIteratorType type;
    private boolean override;
    private List<Integer> blackList = new ArrayList<>();

    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int slot = -1;
        private int row;
        private int column;
        private SlotIteratorType type;
        private boolean override;
        private List<Integer> blackList = new ArrayList<>();

        //Todo:
        public Builder addBlackList(@Nonnegative int slot) {
            this.blackList.add(slot);
            return this;
        }

        public Builder blackList(@NotNull List<Integer> slots) {
            this.blackList = slots;
            return this;
        }

        public Builder slot(@Nonnegative int startSlot) {
            this.slot = startSlot;
            return this;
        }

        public Builder row(@Nonnegative int row) {
            this.row = row;
            return this;
        }

        public Builder column(@Nonnegative int column) {
            this.column = column;
            return this;
        }

        public Builder type(@NotNull SlotIteratorType type) {
            this.type = type;
            return this;
        }

        public Builder override(boolean bool) {
            this.override = bool;
            return this;
        }

        public SlotIterator build() {
            SlotIterator slotIterator = new SlotIterator();
            slotIterator.slot = this.slot;
            slotIterator.row = this.row;
            slotIterator.column = this.column;
            slotIterator.type = this.type;
            slotIterator.override = this.override;
            slotIterator.blackList = this.blackList;

            return slotIterator;
        }
    }

    /**
     * @return the start slot
     */
    public int getSlot() {
        return this.slot;
    }

    /**
     * @return the start column
     */
    public int getColumn() {
        return this.column;
    }

    /**
     * @return the start row
     */
    public int getRow() {
        return this.row;
    }

    /**
     * @return the SlotIteratorType
     */
    public SlotIteratorType getType() {
        return this.type;
    }

    /**
     * @return true if the variable is true
     */
    public boolean isOverride() {
        return this.override;
    }

    public enum SlotIteratorType {
        HORIZONTAL,
        VERTICAL
    }

}
