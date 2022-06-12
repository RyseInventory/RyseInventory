package io.github.rysefoxx.pattern;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.enums.IntelligentType;
import io.github.rysefoxx.pagination.InventoryContents;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/11/2022
 */
public class ContentPattern {

    private final InventoryContents contents;
    private final List<String> lines = new ArrayList<>();

    public ContentPattern(InventoryContents contents) {
        this.contents = contents;
    }

    /**
     * Defines the pattern to be searched for.
     *
     * @param lines The lines of the pattern.
     * @throws IllegalArgumentException If the line length is not 9.
     */
    public void define(String... lines) throws IllegalArgumentException {
        long count = Arrays.stream(lines).filter(line -> line.length() != 9).count();
        if (count > 0) {
            throw new IllegalArgumentException("Passed pattern must contain 9 characters");
        }

        this.lines.addAll(Arrays.asList(lines));
    }

    /**
     * Places items in the inventory based on the pattern.
     *
     * @param frame The frame to place the items in.
     * @param item  The item to place.
     */
    public void set(char frame, IntelligentItem item) {
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("No pattern have been defined.");
        }

        int slot = -1;

        for (String line : this.lines) {
            for (int i = 0; i < line.toCharArray().length; i++) {
                slot++;
                char c = line.charAt(i);
                if (c != frame) continue;

                this.contents.set(slot, item);
            }
        }
    }

    /**
     * Places items in the inventory based on the pattern.
     *
     * @param frame The frame to place the items in.
     * @param item  The item to place.
     */
    public void set(char frame, ItemStack item) {
        set(frame, IntelligentItem.empty(item));
    }

    /**
     * Places items in the inventory based on the pattern.
     *
     * @param frame The frame to place the items in.
     * @param item  The item to place.
     * @param type  The type of the item.
     */
    public void set(char frame, ItemStack item, IntelligentType type) {
        if (type == IntelligentType.EMPTY) {
            set(frame, IntelligentItem.empty(item));
            return;
        }
        if(type == IntelligentType.IGNORED) {
            set(frame, IntelligentItem.ignored(item));
        }
    }

    /**
     * @return The pattern specified in the {@link #define(String...)} method.
     */
    public List<String> getPattern() {
        return this.lines;
    }
}
