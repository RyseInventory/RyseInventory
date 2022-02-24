package com.github.rysefoxx;

import com.github.rysefoxx.content.IntelligentItem;
import com.github.rysefoxx.content.InventoryProvider;
import com.github.rysefoxx.pagination.InventoryContents;
import com.github.rysefoxx.pagination.InventoryManager;
import com.github.rysefoxx.pagination.Pagination;
import com.github.rysefoxx.pagination.RyseInventory;
import com.github.rysefoxx.util.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public final class RyseInventoryPlugin extends JavaPlugin {

    private @Getter
    static InventoryManager inventoryManager;

    @Override
    public void onEnable() {
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
                .provider(new InventoryProvider() {
                    @Override
                    public void update(@NotNull Player player, @NotNull InventoryContents contents) {
                        contents.removeFirst(new ItemStack(Material.GUNPOWDER));
                    }

                    @Override
                    public void init(@NotNull Player player, @NotNull InventoryContents contents) {
                        contents.fillBorders(IntelligentItem.empty(new ItemBuilder(Material.BLACK_STAINED_GLASS).amount(50).build()));
                        Pagination pagination = contents.pagination();

                        contents.set(5, 3, IntelligentItem.of(new ItemBuilder(Material.ARROW).displayName(pagination.isFirst() ? "Du bist erste seite" : "klicke für seite" + pagination.copy().previous().page()).build(), new Consumer<InventoryClickEvent>() {
                            @Override
                            public void accept(InventoryClickEvent event) {
                                if (pagination.isFirst()) {
                                    player.sendMessage("DU BIST AUF ERSTE SEITE");
                                    return;
                                }

                                RyseInventory inventory = pagination.inventory();
                                inventory.open(player, pagination.previous().page());
                            }
                        }));

                        contents.set(12, IntelligentItem.empty(new ItemStack(Material.CREEPER_SPAWN_EGG)));

                        pagination.setItemsPerPage(1);
                        pagination.setItems(Arrays.asList(
                                IntelligentItem.empty(new ItemBuilder(Material.GUNPOWDER).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.TNT).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.SPAWNER).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.OAK_DOOR).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.SHEARS).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.WRITTEN_BOOK).build())));

                        SlotIterator slotIterator = SlotIterator.builder().slot(10).type(SlotIterator.SlotIteratorType.HORIZONTAL).build();
                        pagination.iterator(slotIterator);


                        contents.set(5, 5, IntelligentItem.of(new ItemBuilder(Material.ARROW).displayName(pagination.isLast() ? "Du bist letzte seite" : "klicke für seite" + pagination.copy().next().page()).build(), new Consumer<InventoryClickEvent>() {
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

        Optional<InventoryContents> contents = inventoryManager.getContents(player);

        contents.ifPresent(contents1 -> {
            System.out.println(contents1.pagination().lastPage());
        });


    }
}
