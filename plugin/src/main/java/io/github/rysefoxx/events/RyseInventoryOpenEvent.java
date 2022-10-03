/*
 * MIT License
 *
 * Copyright (c) 2022. Rysefoxx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.github.rysefoxx.events;

import io.github.rysefoxx.pagination.RyseInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 7/4/2022
 */
public class RyseInventoryOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private final Player player;
    private RyseInventory inventory;

    private boolean isCancelled;

    /**
     * @param player    The player who opened the inventory.
     * @param inventory The inventory that was opened.
     *                  The event is called when a new RyseInventory is opened.
     */
    public RyseInventoryOpenEvent(@NotNull Player player, @NotNull RyseInventory inventory) {
        this.player = player;
        this.inventory = inventory;
        this.isCancelled = false;
    }

    @Contract(pure = true)
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    /**
     * If true, the inventory will not be opened.
     */
    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    /**
     * @return The inventory, which is opened to the player.
     */
    public RyseInventory getInventory() {
        return this.inventory;
    }

    /**
     * @param inventory The new RyseInventory, which will be opened to the player.
     */
    public void setInventory(@NotNull RyseInventory inventory) {
        this.inventory = inventory;
    }

    /**
     * @return The player for whom the inventory is opened.
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }
}
