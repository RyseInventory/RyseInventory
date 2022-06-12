package io.github.rysefoxx.pattern;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.pagination.InventoryContents;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/11/2022
 */
public class SearchPattern {

    private final InventoryContents contents;
    private final List<String> lines = new ArrayList<>();

    public SearchPattern(InventoryContents contents) {
        this.contents = contents;
    }

    /**
     * Defines the pattern to be searched for.
     * @param lines The lines of the pattern.
     * @throws IllegalArgumentException If the line length is not 9.
     */
    public void define(String... lines) throws IllegalArgumentException {
        long count = Arrays.stream(lines).filter(line -> line.length() != 9).count();
        if(count > 0) {
            throw new IllegalArgumentException("Passed pattern must contain 9 characters");
        }

        this.lines.addAll(Arrays.asList(lines));
    }

    /**
     * Method to search all IntelligentItems based on the frame.
     * @param frame The frame to search for.
     * @return A list of IntelligentItems that match the frame.
     * @throws IllegalStateException If no pattern have been defined.
     */
    public List<IntelligentItem> searchForIntelligentItems(char frame) throws IllegalStateException {
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("No pattern have been defined.");
        }
        List<IntelligentItem> itemsFound = new ArrayList<>();

        int slot = -1;

        for (String line : this.lines) {
            for (int i = 0; i < line.toCharArray().length; i++) {
                slot++;
                char c = line.charAt(i);
                if (c != frame) continue;

                this.contents.get(slot).ifPresent(itemsFound::add);
            }
        }
        return itemsFound;
    }

    /**
     * Method to search all ItemStacks based on the frame.
     * @param frame The frame to search for.
     * @return A list of IntelligentItems that match the frame.
     * @throws IllegalStateException If no pattern have been defined.
     */
    public List<ItemStack> searchForItemStacks(char frame) throws IllegalStateException {
        return searchForIntelligentItems(frame).stream().map(IntelligentItem::getItemStack).collect(Collectors.toList());
    }

    /**
     * @return The pattern specified in the {@link #define(String...)} method.
     */
    public List<String> getPattern() {
        return this.lines;
    }

}
