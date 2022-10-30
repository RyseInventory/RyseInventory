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

package io.github.rysefoxx.inventory.plugin.other;


import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Class to create your own inventory events.
 * {@link RyseInventory.Builder#listener(EventCreator)}
 *
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */
public class EventCreator<T> {

    private final Class<T> clazz;
    private final Consumer<T> consumer;

    @Contract(pure = true)
    public EventCreator(@NotNull Class<T> clazz, @NotNull Consumer<T> consumer) {
        this.clazz = clazz;
        this.consumer = consumer;
    }

    public void accept(T t) {
        this.consumer.accept(t);
    }

    /**
     * Returns the class of the object.
     *
     * @return The class of the object.
     */
    public @NotNull Class<T> getClazz() {
        return clazz;
    }
}
