package io.github.rysefoxx;

import io.github.rysefoxx.pagination.InventoryManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class RyseInventoryPlugin extends JavaPlugin {

    private @Getter
    static InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        inventoryManager = new InventoryManager(this);
        inventoryManager.invoke();
    }
}
