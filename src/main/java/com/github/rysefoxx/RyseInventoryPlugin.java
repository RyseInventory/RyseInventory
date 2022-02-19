package com.github.rysefoxx;

import com.github.rysefoxx.content.IntelligentItem;
import com.github.rysefoxx.pagination.InventoryContents;
import com.github.rysefoxx.content.InventoryProvider;
import com.github.rysefoxx.pagination.InventoryManager;
import com.github.rysefoxx.pagination.Pagination;
import com.github.rysefoxx.pagination.RyseInventory;
import com.github.rysefoxx.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class RyseInventoryPlugin extends JavaPlugin {

    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        inventoryManager = new InventoryManager(this);
        inventoryManager.invoke();

        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("Test")
                .size(6 * 9)
                .identifier("CUSTOM_INVENTORY")
                .delay(2)
                .period(5)
                .provider(new InventoryProvider() {
                    @Override
                    public void update(@NotNull Player player, @NotNull InventoryContents contents) {
                    }

                    @Override
                    public void init(@NotNull Player player, @NotNull InventoryContents contents) {
                        contents.fillBorders(IntelligentItem.empty(new ItemBuilder(Material.BLACK_STAINED_GLASS).build()));
                        contents.set(11, IntelligentItem.empty(new ItemBuilder(Material.END_STONE).build()));
                        Pagination pagination = contents.pagination();

                        pagination.setItemsPerPage(6);
                        pagination.setItems(Arrays.asList(
                                IntelligentItem.empty(new ItemBuilder(Material.GUNPOWDER).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.TNT).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.SPAWNER).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.OAK_DOOR).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.SHEARS).build()),
                                IntelligentItem.empty(new ItemBuilder(Material.WRITTEN_BOOK).build())));

                        SlotIterator slotIterator = SlotIterator.builder().slot(10).type(SlotIterator.SlotIteratorType.HORIZONTAL).build();
                        pagination.iterator(slotIterator);
                    }
                })
                .build()
                .open(Bukkit.getPlayer("rysefoxx"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
