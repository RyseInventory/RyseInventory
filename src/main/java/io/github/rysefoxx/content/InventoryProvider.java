package io.github.rysefoxx.content;

import io.github.rysefoxx.pagination.InventoryContents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public interface InventoryProvider {

   default void update(@NotNull Player player, @NotNull InventoryContents contents){}

    void init(@NotNull Player player, @NotNull InventoryContents contents);

}
