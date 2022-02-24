package com.github.rysefoxx.content;

import lombok.Getter;
import org.apache.commons.lang.Validate;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/18/2022
 */
@Getter
public class IntelligentItem {

    private final ItemStack itemStack;
    private final Consumer<InventoryClickEvent> consumer;

    @Contract(pure = true)
    public IntelligentItem(@NotNull ItemStack itemStack, @Nullable Consumer<InventoryClickEvent> eventConsumer) {
        this.itemStack = itemStack;
        this.consumer = eventConsumer;
    }

    /**
     * Creates a intelligent ItemStack with an InventoryClickEvent
     *
     * @param itemStack     The ItemStack to be displayed in the inventory.
     * @param eventConsumer The InventoryClickEvent, which is performed when the player clicks on the item.
     * @return An intelligent ItemStack
     */
    @Contract(pure = true)
    public static @NotNull IntelligentItem of(@NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer);
    }

    /**
     * Creates a intelligent ItemStack without a InventoryClickEvent
     *
     * @param itemStack The itemStack to be displayed in the inventory.
     * @return An intelligent ItemStack
     */
    @Contract(pure = true)
    public static @NotNull IntelligentItem empty(@NotNull ItemStack itemStack) {
        return new IntelligentItem(itemStack, event -> {
        });
    }


    /**
     * Changes the ItemStack of an existing ItemStack without changing the consumer.
     *
     * @param newItemStack The new ItemStack
     * @return The new intelligent ItemStack
     */
    public @NotNull IntelligentItem update(@NotNull ItemStack newItemStack) {
        return new IntelligentItem(newItemStack, this.consumer);
    }
}
