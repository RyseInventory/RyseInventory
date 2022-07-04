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
public class RyseInventoryTitleChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private final Player player;
    private final String oldTitle;
    private String newTitle;

    private boolean isCancelled;

    /**
     * This event is called when the title of the inventory changes.
     */
    public RyseInventoryTitleChangeEvent(@NotNull Player player, @NotNull String oldTitle, @NotNull String newTitle) {
        this.player = player;
        this.isCancelled = false;
        this.oldTitle = oldTitle;
        this.newTitle = newTitle;
    }

    /**
     * If true, the title set via {@link #setNewTitle(String)} will be ignored.
     */
    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * @return The previous title of the inventory.
     */
    public @NotNull String getOldTitle() {
        return this.oldTitle;
    }

    /**
     * @return The new title of the inventory.
     */
    public @NotNull String getNewTitle() {
        return this.newTitle;
    }

    /**
     * @return The player who's inventory is being changed.
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * Gives the inventory a new title.
     * @param newTitle The new title of the inventory.
     * @apiNote If isCancelled is true, the title will not be set.
     */
    public void setNewTitle(@NotNull String newTitle) {
        if (this.isCancelled) return;

        this.newTitle = newTitle;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    @Contract(pure = true)
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
