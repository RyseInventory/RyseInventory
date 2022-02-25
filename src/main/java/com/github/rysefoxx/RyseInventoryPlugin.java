package com.github.rysefoxx;

import com.github.rysefoxx.content.IntelligentItem;
import com.github.rysefoxx.content.InventoryProvider;
import com.github.rysefoxx.other.InventoryOptions;
import com.github.rysefoxx.pagination.InventoryContents;
import com.github.rysefoxx.pagination.InventoryManager;
import com.github.rysefoxx.pagination.Pagination;
import com.github.rysefoxx.pagination.RyseInventory;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

public final class RyseInventoryPlugin extends JavaPlugin {

    private @Getter
    static InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        getCommand("test").setExecutor(new TestCommand());
        getCommand("testtwo").setExecutor(new TestTwoCommand());
        inventoryManager = new InventoryManager(this);
        inventoryManager.invoke();

        Player player = Bukkit.getPlayer("rysefoxx");

        RyseInventory inventory = RyseInventory.builder()
                .manager(inventoryManager)
                .title("Test")
                .rows(6)
                .identifier("CUSTOM_INVENTORY")
                .openDelay(2)
                .delay(5)
                .options(InventoryOptions.values())
                .share()
                .provider(new InventoryProvider() {
                    @Override
                    public void update(@NotNull Player player, @NotNull InventoryContents contents) {
//                        contents.removeFirst(new ItemStack(Material.BLACK_STAINED_GLASS));
                        contents.set(21, IntelligentItem.empty(new ItemStack(Material.EMERALD)));
                    }


                    @Override
                    public void init(@NotNull Player player, @NotNull InventoryContents contents) {
                        contents.fillBorders(IntelligentItem.empty(new ItemStack(Material.BLACK_STAINED_GLASS, 50)));
                        Pagination pagination = contents.pagination();

                        contents.set(23, IntelligentItem.empty(new ItemStack(Material.DIAMOND)));

                        contents.set(5, 3, IntelligentItem.of(new ItemStack(Material.ARROW), new Consumer<InventoryClickEvent>() {
                                    @Override
                                    public void accept(InventoryClickEvent event) {
                                        if (pagination.isFirst()) {
                                            player.sendMessage("DU BIST AUF ERSTE SEITE");
                                            return;
                                        }

                                        RyseInventory inventory = pagination.inventory();
                                        inventory.open(player, pagination.previous().page());
                                    }
                                })
                                .canSee(() -> player.hasPermission("test.test.test"))
                                .canClick(() -> player.hasPermission("")));


                        pagination.setItemsPerPage(1);
                        pagination.setItems(Arrays.asList(
                                IntelligentItem.empty(new ItemStack(Material.GUNPOWDER)),
                                IntelligentItem.empty(new ItemStack(Material.TNT)),
                                IntelligentItem.empty(new ItemStack(Material.SPAWNER)),
                                IntelligentItem.empty(new ItemStack(Material.OAK_DOOR)),
                                IntelligentItem.empty(new ItemStack(Material.SHEARS)),
                                IntelligentItem.empty(new ItemStack(Material.WRITTEN_BOOK))));

                        SlotIterator slotIterator = SlotIterator.builder().slot(10).type(SlotIterator.SlotIteratorType.HORIZONTAL).build();
                        pagination.iterator(slotIterator);


                        contents.set(5, 5, IntelligentItem.of(new ItemStack(Material.ARROW), new Consumer<InventoryClickEvent>() {
                            @Override
                            public void accept(InventoryClickEvent event) {
                                if (pagination.isLast()) {
                                    player.sendMessage("DU BIST LETZTE");
                                    return;
                                }

                                RyseInventory inventory = pagination.inventory();
                                inventory.open(player, pagination.next().page());
                            }
                        }));
                    }
                })
                .build(this);

        inventory.open(player);
    }
}
