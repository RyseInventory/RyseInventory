package io.github.rysefoxx.content;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IntelligentItemError {

    default void cantClick(@NotNull Player player, @NotNull IntelligentItem item){}

    default void cantSee(@NotNull Player player, @NotNull IntelligentItem item){}

}
