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

package io.github.rysefoxx.pattern;

import com.google.common.annotations.Beta;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @apiNote The class is marked as 'beta' because of possible bugs.
 * @since 6/11/2022
 */
@Beta
public class SlotIteratorPattern {
    private @Getter List<String> lines = new ArrayList<>();
    private @Getter char attachedChar;

    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> lines = new ArrayList<>();
        private Character attachedChar;

        /**
         * Defines the pattern.
         *
         * @param lines The lines of the pattern.
         * @throws IllegalArgumentException If the line length is not 9.
         */
        @Contract("_ -> this")
        public @NotNull Builder define(String @NotNull ... lines) throws IllegalArgumentException {
            long count = Arrays.stream(lines).filter(line -> line.length() != 9).count();
            if (count > 0)
                throw new IllegalArgumentException("Passed pattern must contain 9 characters");

            this.lines.addAll(Arrays.asList(lines));
            return this;
        }

        /**
         * Defines the pattern.
         *
         * @param line The line of the pattern.
         * @param amount How often this pattern should be repeated.
         * @throws IllegalArgumentException If the line length is not 9 or the amount is higher than 6.
         */
        public @NotNull Builder define(@NotNull String line, @Nonnegative int amount) throws IllegalArgumentException {
            if (line.length() != 9)
                throw new IllegalArgumentException("Passed pattern must contain 9 characters");

            if(amount > 6)
                throw new IllegalArgumentException("Passed amount must be lower than 6");

            for (int i = 0; i < amount; i++)
                this.lines.add(line);

            return this;
        }

        /**
         * Using this method, you can decide which frame will receive the items.
         *
         * @param frame The frame to place the items in.
         */
        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull Builder attach(char frame) {
            this.attachedChar = frame;
            return this;
        }

        /**
         * Builds the pattern.
         *
         * @return The pattern.
         * @throws IllegalStateException If no pattern have been defined.
         */
        public @NotNull SlotIteratorPattern buildPattern() throws IllegalStateException {
            SlotIteratorPattern pattern = new SlotIteratorPattern();
            if (this.lines.isEmpty())
                throw new IllegalStateException("No pattern have been defined.");

            if (this.attachedChar == null)
                throw new IllegalStateException("No frame has been attached.");

            boolean foundChar = false;
            for (String line : this.lines) {
                for (char c : line.toCharArray()) {
                    if (c == this.attachedChar) {
                        foundChar = true;
                        break;
                    }
                }
            }

            if (!foundChar) throw new IllegalStateException("The attached frame is not in the pattern.");

            pattern.lines = this.lines;
            pattern.attachedChar = this.attachedChar;
            return pattern;
        }
    }
}
