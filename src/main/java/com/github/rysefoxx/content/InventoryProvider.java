package com.github.rysefoxx.content;

import com.github.rysefoxx.pagination.InventoryContents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public interface InventoryProvider {

    void update(@NotNull Player player, @NotNull InventoryContents contents);

    void init(@NotNull Player player, @NotNull InventoryContents contents);

}
