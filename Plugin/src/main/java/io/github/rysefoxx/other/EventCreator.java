package io.github.rysefoxx.other;


import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */
public class EventCreator<T> {

    private final Class<T> clazz;
    private final Consumer<T> consumer;

    public EventCreator(Class<T> clazz,  Consumer<T> consumer) {
        this.clazz = clazz;
        this.consumer = consumer;
    }

    public void accept(T t) {
        this.consumer.accept(t);
    }

    public Class<T> getClazz() {
        return clazz;
    }
}
