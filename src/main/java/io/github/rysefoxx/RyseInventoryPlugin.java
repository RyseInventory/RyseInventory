package io.github.rysefoxx;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.pagination.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class RyseInventoryPlugin extends JavaPlugin {

    private @Getter
    static InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        inventoryManager = new InventoryManager(this);
        inventoryManager.invoke();

        RyseInventory.builder()
                .manager(inventoryManager)
                .rows(6)
                .title("lol")
                .animation(SlideAnimation.builder(this)
                        .from(Arrays.asList(17, 17, 17, 35, 35, 35))
                        .to(Arrays.asList(11, 13, 15, 29, 31, 33))
                        .item(Arrays.asList(
                                IntelligentItem.empty(new ItemStack(Material.GRANITE)),
                                IntelligentItem.empty(new ItemStack(Material.GRANITE)),
                                IntelligentItem.empty(new ItemStack(Material.CHEST)),
                                IntelligentItem.empty(new ItemStack(Material.CHEST)),
                                IntelligentItem.empty(new ItemStack(Material.CHEST)),
                                IntelligentItem.empty(new ItemStack(Material.CHEST))))
                        .direction(AnimatorDirection.HORIZONTAL_RIGHT_LEFT)
                        .delay(11, TimeSetting.MILLISECONDS)
                        .period(5, TimeSetting.MILLISECONDS)
                        .build()
                )
                .provider(new InventoryProvider() {
                    @Override
                    public void init(@NotNull Player player, @NotNull InventoryContents contents, @NotNull SlideAnimation animation) {
                        animation.animate(RyseInventoryPlugin.this, contents);
                    }
                })
                .build(this)
                .open(Bukkit.getPlayer("rysefoxx"));

//        RyseInventory.builder()
//                .manager(inventoryManager)
//                .rows(6)
//                .type(InventoryOpenerType.ENDER_CHEST)
//                .title("§71234 5678")
//                .delay(2, TimeSetting.SECONDS)
//                .provider(new InventoryProvider() {
//                    @Override
//                    public void init(@NotNull Player player, @NotNull InventoryContents contents) {
//
//                        IntelligentTitleAnimator titleAnimator = IntelligentTitleAnimator.builder()
//                                .loop()
//                                .delay(1, TimeSetting.SECONDS)
//                                .period(3, TimeSetting.MILLISECONDS)
//                                .type(IntelligentItemAnimatorType.WORD_BY_WORD)
//                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.AQUA).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.GOLD).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.YELLOW).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.RED).build())
//                                .frame("ACDB")
//                                .build(contents);
//                        titleAnimator.animate(RyseInventoryPlugin.this, player);

//                        IntelligentItem item = IntelligentItem.empty(new ItemBuilder(Material.DIAMOND).lore("Animation System", "RyseInventory", "Rysefoxx").build());
//                        IntelligentItemLoreAnimator loreAnimator = IntelligentItemLoreAnimator.builder()
//                                .item(item)
//                                .slot(0)
//                                .delay(1, TimeSetting.SECONDS)
//                                .period(3, TimeSetting.MILLISECONDS)
//                                .type(IntelligentItemAnimatorType.FULL_WORD)
//                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.AQUA).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.GOLD).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.DARK_RED).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.RED).build())
//                                .lore(0, "BCD")
//                                .lore(1, "ADC")
//                                .lore(2, "CDC")
//                                .loop()
//                                .build(contents);
//                        loreAnimator.animate(RyseInventoryPlugin.this);
//                        contents.set(0, item);

////

//                        IntelligentItemNameAnimator itemNameAnimator = IntelligentItemNameAnimator.builder()
//                                .loop()
//                                .item(item)
//                                .slot(0)
//                                .delay(1, TimeSetting.SECONDS)
//                                .period(3, TimeSetting.MILLISECONDS)
//                                .type(IntelligentItemAnimatorType.FLASH)
//                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.AQUA).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.GOLD).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.DARK_RED).build(),
//                                        IntelligentItemColor.builder().bukkitColor(ChatColor.RED).build())
//                                .frame("ABCD")
//                                .build(contents);
//                        itemNameAnimator.animate(RyseInventoryPlugin.this);
//                    }
//                })
//                .build(this)
//                .open(Bukkit.getPlayer("rysefoxx"));
//
    }
}
