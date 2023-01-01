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

package io.github.rysefoxx.inventory.plugin.animator;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import io.github.rysefoxx.inventory.plugin.enums.TimeSetting;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import io.github.rysefoxx.inventory.plugin.util.StringConstants;
import io.github.rysefoxx.inventory.plugin.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx(Rysefoxx # 6772) |
 * @since 4/12/2022
 */
public class IntelligentMaterialAnimator {

    private static Plugin plugin;
    private List<String> frames = new ArrayList<>();
    private HashMap<Character, Material> frameMaterial = new HashMap<>();
    private int period = 20;
    private int delay = 0;
    private int slot = -1;
    private BukkitTask task;
    private boolean loop;
    private RyseInventory inventory;
    private IntelligentItem intelligentItem;
    private Object identifier;
    private InventoryContents contents;

    @Contract("_ -> new")
    public static @NotNull Builder builder(@NotNull Plugin plugin) {
        IntelligentMaterialAnimator.plugin = plugin;
        return new Builder();
    }

    /**
     * This starts the animation for the item.
     */
    public void animate() {
        this.inventory.addMaterialAnimator(this);
        animateItem();
    }

    /**
     * This stops the animation for the item.
     *
     * @return true if the animation was stopped.
     */
    public boolean stop() {
        if (this.task == null || !Bukkit.getScheduler().isQueued(this.task.getTaskId()))
            return false;

        this.task.cancel();
        return true;
    }

    /**
     * It loops through the frames, and updates the material of the item in the inventory
     */
    private void animateItem() {
        int finalLength = getFrameLength();

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final List<String> framesCopy = frames;
            final ItemStack itemStack = new ItemStack(intelligentItem.getItemStack());
            int materialState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            Material currentMaterial;

            @Override
            public void run() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                resetWhenFrameFinished(currentFrames);

                if (cancelIfListIsEmpty()) return;

                currentFrames = updateFramesWhenRequired(currentFrames);

                char singleFrame = currentFrames[this.materialState];

                this.currentMaterial = frameMaterial.get(singleFrame);
                this.materialState++;
                this.subStringIndex++;

                this.itemStack.setType(this.currentMaterial);
                contents.update(slot, this.itemStack);
            }

            private char @NotNull [] updateFramesWhenRequired(char @NotNull [] currentFrames) {
                if (this.materialState < currentFrames.length) return currentFrames;

                this.materialState = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeMaterialAnimator(IntelligentMaterialAnimator.this);
                    return true;
                }
                return false;
            }

            private void resetWhenFrameFinished(char[] currentFrames) {
                if (this.subStringIndex < finalLength) return;

                if (!loop)
                    this.framesCopy.remove(0);
                this.materialState = 0;
                this.subStringIndex = 0;

                if (!this.framesCopy.isEmpty())
                    this.currentMaterial = frameMaterial.get(currentFrames[this.materialState]);

                if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                    this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    /**
     * This function returns the task that is currently running.
     * <br> <br>
     * <font color="red">This is an internal method! <b>ANYTHING</b> about this method can change. It is not recommended to use this method.</font>
     * <br> <br>
     *
     * @return The task that is being run.
     */
    @ApiStatus.Internal
    public @NotNull BukkitTask getTask() {
        return this.task;
    }

    /**
     * Returns the identifier of this object, or null if it has none.
     *
     * @return The identifier of the object.
     */
    public @Nullable Object getIdentifier() {
        return this.identifier;
    }

    /**
     * This function returns the length of the frames array.
     *
     * @return The length of the frames array.
     */
    @Contract(pure = true)
    private int getFrameLength() {
        int length = 0;
        for (String frame : frames) {
            length += frame.length();
        }
        return length;
    }

    public static class Builder {

        private IntelligentMaterialAnimator preset;

        private IntelligentItem intelligentItem;
        private List<String> frames = new ArrayList<>();
        private HashMap<Character, Material> frameMaterial = new HashMap<>();
        private int period = 20;
        private int delay = 0;
        private int slot = -1;

        private boolean loop;
        private Object identifier;

        /**
         * This tells which item is to be animated.
         *
         * @param intelligentItem The item that is to be animated.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder item(@NotNull IntelligentItem intelligentItem) {
            this.intelligentItem = intelligentItem;
            return this;
        }


        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         * <p>
         * When copying the animator, the identification is not copied if present!
         */
        public @NotNull Builder copy(@NotNull IntelligentMaterialAnimator preset) {
            this.preset = preset;
            return this;
        }

        /**
         * Keeps the animation running until the player closes the inventory.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder loop() {
            this.loop = true;
            return this;
        }

        /**
         * This tells us in which slot the animation should take place.
         *
         * @param slot The slot in which the animation should take place.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is greater than 53
         */
        public @NotNull Builder slot(@Nonnegative int slot) throws IllegalArgumentException {
            if (slot > 53)
                throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

            this.slot = slot;
            return this;
        }

        /**
         * Assigns a material to a frame.
         *
         * @param frame    The frame that should receive the material.
         * @param material The material you want the frame to have.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder material(char frame, @NotNull Material material) {
            this.frameMaterial.put(frame, material);
            return this;
        }

        /**
         * Several frames are assigned individual materials.
         *
         * @param frames    The frames that should receive the material.
         * @param materials The materials you want the frame to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder materials(@NotNull List<Character> frames, Material @NotNull ... materials) throws IllegalArgumentException {
            Preconditions.checkArgument(frames.size() == materials.length, StringConstants.INVALID_MATERIAL_FRAME);

            for (int i = 0; i < frames.size(); i++)
                material(frames.get(i), materials[i]);

            return this;
        }

        /**
         * Several frames are assigned individual materials.
         *
         * @param frames    The frames that should receive the material.
         * @param materials The materials you want the frame to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder materials(Character @NotNull [] frames, Material @NotNull ... materials) {
            Preconditions.checkArgument(frames.length == materials.length, StringConstants.INVALID_MATERIAL_FRAME);

            for (int i = 0; i < frames.length; i++)
                material(frames[i], materials[i]);

            return this;
        }

        /**
         * Several frames are assigned individual materials.
         *
         * @param frames    The frames that should receive the material.
         * @param materials The materials you want the frame to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder materials(Character @NotNull [] frames, @NotNull List<Material> materials) {
            Preconditions.checkArgument(frames.length == materials.size(), StringConstants.INVALID_MATERIAL_FRAME);

            for (int i = 0; i < frames.length; i++)
                material(frames[i], materials.get(i));

            return this;
        }

        /**
         * Adds another frame.
         *
         * @param frame The frame that should be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link Builder#material(char, Material)}
         */
        public @NotNull Builder frame(@NotNull String frame) throws IllegalArgumentException {
            this.frames.add(frame);
            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames The frames that should be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link Builder#material(char, Material)}
         */
        public @NotNull Builder frames(String @NotNull ... frames) {
            for (String frame : frames)
                frame(frame);

            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames The frames that should be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link Builder#material(char, Material)}
         */
        public @NotNull Builder frames(@NotNull List<String> frames) {
            frames.forEach(this::frame);
            return this;
        }

        /**
         * Sets the speed of the animation in the scheduler.
         *
         * @param time    The time.
         * @param setting The time setting.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder period(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.period = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Specifies the delay before the animation starts.
         *
         * @param time    The delay.
         * @param setting The time setting.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder delay(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.delay = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Gives the Animation an identification
         *
         * @param identifier The ID through which you can get the animation
         * @return The Builder to perform further editing
         * <p>
         * When copying the animator, the identification is not copied if present!
         */
        public @NotNull Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link IntelligentMaterialAnimator#animate()}
         *
         * @param contents The contents of the inventory.
         * @return The animation class
         * @throws IllegalArgumentException if no slot was specified, if frameMaterial is empty, if frames is empty or if no material has been assigned to a frame.
         * @throws NullPointerException     if item is null.
         */
        public IntelligentMaterialAnimator build(@NotNull InventoryContents contents) throws IllegalArgumentException, NullPointerException {
            if (this.preset != null) {
                this.intelligentItem = this.preset.intelligentItem;
                this.frames = this.preset.frames;
                this.frameMaterial = this.preset.frameMaterial;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.slot = this.preset.slot;
                this.loop = this.preset.loop;
            }

            if (this.slot == -1)
                throw new IllegalArgumentException("Please specify a slot where the item is located.");

            if (this.frameMaterial.isEmpty())
                throw new IllegalArgumentException("Please specify a material for each frame.");

            if (this.intelligentItem == null)
                throw new NullPointerException("Please specify an item to animate.");

            if (this.frames.isEmpty())
                throw new IllegalArgumentException("No frames have been defined yet!");


            for (String frame : this.frames) {
                for (char c : frame.toCharArray()) {
                    if (frameMaterial.containsKey(c)) continue;
                    throw new IllegalArgumentException("You created the frame " + frame + ", but the letter " + c + " was not assigned a material.");
                }
            }

            IntelligentMaterialAnimator animator = new IntelligentMaterialAnimator();
            animator.intelligentItem = this.intelligentItem;
            animator.delay = this.delay;
            animator.frameMaterial = this.frameMaterial;
            animator.frames = this.frames;
            animator.loop = this.loop;
            animator.period = this.period;
            animator.slot = this.slot;
            animator.identifier = this.identifier;
            animator.contents = contents;
            animator.inventory = contents.pagination().inventory();
            return animator;
        }
    }
}
