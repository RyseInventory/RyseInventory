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

package io.github.rysefoxx.other;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx | Rysefoxx#7880
 * @since 9/4/2022
 */
@Getter
@Accessors(fluent = true)
public class Page {

    private final int page;
    private final int rows;

    @Contract(pure = true)
    private Page(@Nonnegative int page, @Nonnegative int rows) {
        this.page = page;
        this.rows = rows;
    }

    /**
     * Gives the page a specified number of rows.
     *
     * @param page The page number. First page is 0.
     * @param rows The number of rows. Must be between 1 and 6.
     * @return The page with the specified number of rows.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Page of(@Nonnegative int page, @Nonnegative int rows) {
        return new Page(page, rows);
    }
}
