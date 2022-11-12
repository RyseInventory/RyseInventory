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

package io.github.rysefoxx.inventory.plugin.pattern;

import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/11/2022
 */
public class SearchPattern {

    private final InventoryContents contents;
    private final List<String> lines = new ArrayList<>();

    public SearchPattern(@NotNull InventoryContents contents) {
        this.contents = contents;
    }

    /**
     * Defines the pattern to be searched for.
     *
     * @param lines The lines of the pattern.
     * @throws IllegalArgumentException If the line length is not 9.
     */
    public void define(String @NotNull ... lines) throws IllegalArgumentException {
        long count = Arrays.stream(lines).filter(line -> line.length() != 9).count();
        if (count > 0)
            throw new IllegalArgumentException("Passed pattern must contain 9 characters");

        this.lines.addAll(Arrays.asList(lines));
    }

    /**
     * Defines the pattern to be searched for.
     *
     * @param line The line of the pattern.
     * @param amount How often this pattern should be repeated.
     * @throws IllegalArgumentException If the line length is not 9 or the amount is higher than 6.
     */
    public void define(@NotNull String line, @Nonnegative int amount) throws IllegalArgumentException {
        if (line.length() != 9)
            throw new IllegalArgumentException("Passed pattern must contain 9 characters");

        if(amount > 6)
            throw new IllegalArgumentException("Passed amount must be lower than 6");

        for (int i = 0; i < amount; i++)
            this.lines.add(line);
    }

    /**
     * Method to search all IntelligentItems based on the frame.
     *
     * @param frame The frame to search for.
     * @return A list of IntelligentItems that match the frame.
     * @throws IllegalStateException If no pattern have been defined.
     */
    public @NotNull List<IntelligentItem> searchForIntelligentItems(char frame) throws IllegalStateException {
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
     *
     * @param frame The frame to search for.
     * @return A list of IntelligentItems that match the frame.
     * @throws IllegalStateException If no pattern have been defined.
     */
    public @NotNull List<ItemStack> searchForItemStacks(char frame) throws IllegalStateException {
        return searchForIntelligentItems(frame).stream().map(IntelligentItem::getItemStack).collect(Collectors.toList());
    }

    /**
     * @return The pattern specified in the {@link #define(String...)} method.
     * @throws UnsupportedOperationException If list gets modified.
     */
    @Unmodifiable
    public @NotNull List<String> getPattern() throws UnsupportedOperationException {
        return Collections.unmodifiableList(this.lines);
    }
}
