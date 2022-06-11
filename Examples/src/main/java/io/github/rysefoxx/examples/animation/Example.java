package io.github.rysefoxx.examples.animation;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.content.IntelligentItemColor;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.enums.AnimatorDirection;
import io.github.rysefoxx.enums.TimeSetting;
import io.github.rysefoxx.pagination.*;
import io.github.rysefoxx.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/10/2022
 */
public class Example extends JavaPlugin {

    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        this.inventoryManager = new InventoryManager(this);
        this.inventoryManager.invoke();
    }

    private void titleAnimation() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Title Animation")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        IntelligentTitleAnimator titleAnimator = IntelligentTitleAnimator.builder(Example.this)
                                .loop()
                                .delay(1, TimeSetting.SECONDS)
                                .period(3, TimeSetting.MILLISECONDS)
                                .type(IntelligentItemAnimatorType.WORD_BY_WORD)
                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
                                        IntelligentItemColor.builder().bukkitColor(ChatColor.BLUE).build(),
                                        IntelligentItemColor.builder().paragraph("§4").bold().build(),
                                        IntelligentItemColor.builder().rgbColor(182, 143, 38).bold().build(),
                                        IntelligentItemColor.builder().hexColor("#5e114b").underline().build())
                                .frames("ABCD")
                                .build(contents);
                        titleAnimator.animate(player);
                    }
                })
                .build(this);
    }

    private void materialAnimation() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Material Animation")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        IntelligentItem item = IntelligentItem.empty(new ItemStack(Material.STONE));
                        contents.set(5, item);

                        IntelligentMaterialAnimator materialAnimator = IntelligentMaterialAnimator.builder(Example.this)
                                .item(item)
                                .slot(5)
                                .loop()
                                .delay(1, TimeSetting.SECONDS)
                                .period(3, TimeSetting.MILLISECONDS)
                                .materials(Arrays.asList('A', 'B', 'C', 'D'),
                                        Material.ANVIL,
                                        Material.DIAMOND_BLOCK,
                                        Material.EMERALD_BLOCK,
                                        Material.GOLD_BLOCK)
                                .frame("ABCD")
                                .build(contents);
                        materialAnimator.animate();
                    }
                })
                .build(this);
    }

    private void itemNameAnimation() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Item-Name Animation")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        IntelligentItem item = IntelligentItem.empty(new ItemStack(Material.STONE));
                        contents.set(5, item);

                        IntelligentItemNameAnimator itemNameAnimator = IntelligentItemNameAnimator.builder(Example.this)
                                .loop()
                                .item(item)
                                .slot(5)
                                .delay(1, TimeSetting.SECONDS)
                                .period(3, TimeSetting.MILLISECONDS)
                                .type(IntelligentItemAnimatorType.WORD_BY_WORD)
                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
                                        IntelligentItemColor.builder().paragraph("§a").bold().build(),
                                        IntelligentItemColor.builder().rgbColor(23, 53, 234).bold().build(),
                                        IntelligentItemColor.builder().bukkitColor(ChatColor.GRAY).build(),
                                        IntelligentItemColor.builder().hexColor("#126b58").underline().build())
                                .frames("ABCD")
                                .build(contents);
                        itemNameAnimator.animate();
                    }
                })
                .build(this);
    }

    private void loreAnimaton() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Lore Animation")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        IntelligentItem item = IntelligentItem.empty(new ItemBuilder(Material.STONE).lore("This is a lore", "This is a lore 2").build());
                        contents.set(5, item);

                        IntelligentItemLoreAnimator loreAnimator = IntelligentItemLoreAnimator.builder(Example.this)
                                .loop()
                                .item(item)
                                .slot(5)
                                .delay(1, TimeSetting.SECONDS)
                                .period(3, TimeSetting.MILLISECONDS)
                                .type(IntelligentItemAnimatorType.WORD_BY_WORD)
                                .colors(Arrays.asList('A', 'B', 'C', 'D'),
                                        IntelligentItemColor.builder().paragraph("§9").bold().build(),
                                        IntelligentItemColor.builder().rgbColor(250, 1, 52).bold().build(),
                                        IntelligentItemColor.builder().bukkitColor(ChatColor.DARK_GRAY).build(),
                                        IntelligentItemColor.builder().hexColor("#e610c9").underline().build())
                                .lore(0, "ABCD")
                                .lore(1, "DCBA")
                                .build(contents);
                        loreAnimator.animate();
                    }
                })
                .build(this);
    }

    private void slideAnimation() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Slide Animation")
                .rows(6)
                .animation(SlideAnimation.builder(this)
                        .from(3)
                        .to(30)
                        .items(IntelligentItem.empty(new ItemStack(Material.STONE)))
                        .delay(1, TimeSetting.SECONDS)
                        .period(11, TimeSetting.MILLISECONDS)
                        .blockClickEvent()
                        .direction(AnimatorDirection.VERTICAL_UP_DOWN)
                        .build())
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents, SlideAnimation animation) {
                        IntelligentItem item = IntelligentItem.empty(new ItemBuilder(Material.STONE).displayName("I am an object that does nothing").build());
                        contents.set(0, item);

                        animation.animate(contents);
                    }
                })
                .build(this);
    }
}
