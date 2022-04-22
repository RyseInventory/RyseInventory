package io.github.rysefoxx.content;

import io.github.rysefoxx.pagination.InventoryContents;
import io.github.rysefoxx.pagination.SlideAnimation;
import org.bukkit.entity.Player;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public interface InventoryProvider {

    default void update(Player player, InventoryContents contents) {
    }

    default void init(Player player, InventoryContents contents) {
    }

    default void init(Player player, InventoryContents contents, SlideAnimation animation) {
    }

}
