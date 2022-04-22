package io.github.rysefoxx.content;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/18/2022
 */
@Getter
public class IntelligentItem {

    private final ItemStack itemStack;
    private Consumer<InventoryClickEvent> consumer;
    private final IntelligentItemError error;
    private boolean canClick = true;
    private boolean canSee = true;

    public IntelligentItem(ItemStack itemStack, Consumer<InventoryClickEvent> eventConsumer, IntelligentItemError error) {
        this.itemStack = itemStack;
        this.consumer = eventConsumer;
        this.error = error;
    }


    public static IntelligentItem of(ItemStack itemStack, IntelligentItemError error, Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, error);
    }


    public static IntelligentItem of(ItemStack itemStack, Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, null);
    }


    public static IntelligentItem empty(ItemStack itemStack) {
        return new IntelligentItem(itemStack, event -> {
        }, null);
    }


    public static IntelligentItem empty(ItemStack itemStack, IntelligentItemError error) {
        return new IntelligentItem(itemStack, event -> {
        }, error);
    }


    public static IntelligentItem ignored(ItemStack itemStack) {
        return new IntelligentItem(itemStack, null, null);
    }


    public static IntelligentItem ignored(ItemStack itemStack, IntelligentItemError error) {
        return new IntelligentItem(itemStack, null, error);
    }

    /**
     * Removes the consumer from an IntelligentItem
     */
    public void clearConsumer() {
        this.consumer = event -> {
        };
    }

    public IntelligentItem canClick(BooleanSupplier supplier) {
        this.canClick = supplier.getAsBoolean();
        return this;
    }

    public IntelligentItem canSee(BooleanSupplier supplier) {
        this.canSee = supplier.getAsBoolean();
        return this;
    }


    /**
     * Changes the ItemStack of an existing ItemStack without changing the consumer.
     *
     * @param newItemStack The new ItemStack
     * @return The new intelligent ItemStack
     */
    public IntelligentItem update(ItemStack newItemStack) {
        return new IntelligentItem(newItemStack, this.consumer, this.error);
    }

    /**
     * Changes the ItemStack of an existing Intelligent with changing the consumer.
     *
     * @param newIntelligentItem The new IntelligentItem
     * @return The new intelligent ItemStack
     */
    public IntelligentItem update(IntelligentItem newIntelligentItem) {
        return new IntelligentItem(newIntelligentItem.getItemStack(), newIntelligentItem.getConsumer(), this.error);
    }
}
