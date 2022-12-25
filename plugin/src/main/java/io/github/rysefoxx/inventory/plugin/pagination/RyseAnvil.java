package io.github.rysefoxx.inventory.plugin.pagination;

import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import lombok.AccessLevel;
import lombok.Getter;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * @author Rysefoxx | Rysefoxx#7880
 * @since 12/23/2022
 */
public class RyseAnvil {

    private final String title;
    private final InventoryContents contents;
    private final Player player;

    private IntelligentItem itemLeft;
    private IntelligentItem itemRight;
    private IntelligentItem itemOutput;
    private Function<AnvilGUI.Completion, List<AnvilGUI.ResponseAction>> completeFunction;

    @Getter(AccessLevel.PROTECTED)
    @Nullable
    private AnvilGUI anvilGUI;

    @Contract(pure = true)
    public RyseAnvil(@NotNull Player player,
                     @NotNull String title,
                     @NotNull InventoryContents contents) {
        this.player = player;
        this.title = title;
        this.contents = contents;
    }

    /**
     * Places on the left side of the Anvil the item
     *
     * @param itemLeft The item that will be placed in the left slot.
     */
    public void itemLeft(@NotNull IntelligentItem itemLeft) {
        this.itemLeft = itemLeft;
        contents.set(0, itemLeft);
    }

    /**
     * Places on the left side of the Anvil the item
     *
     * @param itemLeft The item that will be placed in the left slot.
     */
    public void itemLeft(@NotNull ItemStack itemLeft) {
        itemLeft(IntelligentItem.empty(itemLeft));
    }

    /**
     * Places in the right of the Anvil the item
     *
     * @param itemRight The item that will be placed in the right slot.
     */
    public void itemRight(@NotNull IntelligentItem itemRight) {
        this.itemRight = itemRight;
        contents.set(1, itemRight);
    }

    /**
     * Places in the right of the Anvil the item
     *
     * @param itemRight The item that will be placed in the right slot.
     */
    public void itemRight(@NotNull ItemStack itemRight) {
        itemRight(IntelligentItem.empty(itemRight));
    }

    /**
     * Places on the output of the Anvil the item
     *
     * @param itemOutput The item that will be placed in the output slot.
     */
    public void itemOutput(@NotNull IntelligentItem itemOutput) {
        this.itemOutput = itemOutput;
        contents.set(2, itemOutput);
    }

    /**
     * Places on the output of the Anvil the item
     *
     * @param itemOutput The item that will be placed in the output slot.
     */
    public void itemOutput(@NotNull ItemStack itemOutput) {
        itemOutput(IntelligentItem.empty(itemOutput));
    }

    /**
     * Called when you click on the output item.
     *
     * @param completeFunction The function that will be called when you click on the output item.
     */
    public void onComplete(@NotNull Function<AnvilGUI.Completion, List<AnvilGUI.ResponseAction>> completeFunction) {
        this.completeFunction = completeFunction;
    }

    /**
     * Opens the Anvil for the player.
     *
     * @param plugin The plugin that will be used to open the Anvil.
     */
    protected void open(@NotNull Plugin plugin, boolean contentDelay, int[] ignoredSlots) {
        AnvilGUI.Builder builder = new AnvilGUI.Builder()
                .title(title)
                .plugin(plugin);

        if (!contentDelay) {
            if (this.itemLeft != null) builder.itemLeft(this.itemLeft.getItemStack());
            if (this.itemRight != null) builder.itemRight(this.itemRight.getItemStack());
            if (this.itemOutput != null) builder.itemOutput(this.itemOutput.getItemStack());
        }
        if (this.completeFunction != null) builder.onComplete(this.completeFunction);

        builder.interactableSlots(ignoredSlots);

        this.anvilGUI = builder.open(this.player);
    }
}
