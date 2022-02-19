package com.github.rysefoxx.other;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */
public record EventCreator<T>(Class<T> aClass, Consumer<T> consumer) {

    @Contract(pure = true)
    public EventCreator(@NotNull Class<T> aClass, @NotNull Consumer<T> consumer) {
        this.aClass = aClass;
        this.consumer = consumer;
    }

    public void accept(T t) {
        this.consumer.accept(t);
    }
}
