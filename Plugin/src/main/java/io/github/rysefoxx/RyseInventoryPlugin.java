package io.github.rysefoxx;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.content.IntelligentItemColor;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.enums.TimeSetting;
import io.github.rysefoxx.pagination.*;
import io.github.rysefoxx.util.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class RyseInventoryPlugin extends JavaPlugin {

    private @Getter
    static InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        inventoryManager = new InventoryManager(this);
        inventoryManager.invoke();

        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Title Animation")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        IntelligentTitleAnimator titleAnimator = IntelligentTitleAnimator.builder(RyseInventoryPlugin.this)
                                .loop()
                                .delay(1, TimeSetting.SECONDS)
                                .period(3, TimeSetting.MILLISECONDS)
                                .type(IntelligentItemAnimatorType.WORD_BY_WORD)
                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
                                        IntelligentItemColor.builder().hexColor("#FF6F45").build(),
                                        IntelligentItemColor.builder().rgbColor(1,60,210).bold().build(),
                                        IntelligentItemColor.builder().rgbColor(182, 143, 38).bold().build(),
                                        IntelligentItemColor.builder().hexColor("#5e114b").underline().build())
                                .frames("ABCD")
                                .build(contents);
                        titleAnimator.animate(player);
                    }
                })
                .build(this).open(Bukkit.getPlayer("rysefoxx"));
    }
}
