package io.github.rysefoxx.content;

import org.bukkit.entity.Player;

public interface IntelligentItemError {

    default void cantClick(Player player, IntelligentItem item){}

    default void cantSee(Player player, IntelligentItem item){}

}
