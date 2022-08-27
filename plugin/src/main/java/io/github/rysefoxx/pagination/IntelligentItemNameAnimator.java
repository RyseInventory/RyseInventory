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

package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.content.IntelligentItemColor;
import io.github.rysefoxx.enums.TimeSetting;
import io.github.rysefoxx.util.StringConstants;
import io.github.rysefoxx.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx(Rysefoxx # 6772)
 * @since 4/12/2022
 */
public class IntelligentItemNameAnimator {

    private static Plugin plugin;
    private List<String> frames = new ArrayList<>();
    private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
    private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
    private int period = 20;
    private int delay = 0;
    private int slot = -1;
    private BukkitTask task;
    private boolean loop;
    private RyseInventory inventory;
    private InventoryContents contents;
    private IntelligentItem intelligentItem;
    private String displayName;
    private Object identifier;

    @Contract("_ -> new")
    public static @NotNull Builder builder(@NotNull Plugin plugin) {
        IntelligentItemNameAnimator.plugin = plugin;
        return new Builder();
    }

    /**
     * This starts the animation for the item.
     */
    public void animate() {
        this.inventory.addItemAnimator(this);
        animateByType();
    }

    private void animateByType() {
        if (this.type == IntelligentItemAnimatorType.FULL_WORD) {
            animateByFullWord();
            return;
        }
        if (this.type == IntelligentItemAnimatorType.WORD_BY_WORD) {
            animateWordByWord();
            return;
        }
        if (this.type == IntelligentItemAnimatorType.FLASH) {
            animateWithFlash();
        }
    }

    private void animateWithFlash() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(displayName).toCharArray();
            final List<String> framesCopy = frames;
            final String fixedDisplayName = ChatColor.stripColor(displayName);

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;

            @Override
            public void run() {
                resetWhenFrameFinished();

                if (cancelIfListIsEmpty()) return;

                char[] currentFrames = updateFramesWhenRequired();

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                String currentName = itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + this.fixedDisplayName;

                this.colorState++;
                this.subStringIndex++;
                updateDisplayName(contents, currentName);
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();
                if (this.colorState < currentFrames.length) return currentFrames;

                this.colorState = 0;

                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
                    return true;
                }
                return false;
            }

            private void resetWhenFrameFinished() {
                if (this.subStringIndex < this.letters.length) return;

                if (!loop)
                    this.framesCopy.remove(0);
                this.colorState = 0;
                this.subStringIndex = 0;

                if (this.currentFrameIndex + 1 < this.framesCopy.size())
                    return;
                this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    private void animateByFullWord() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(displayName).toCharArray();
            final List<String> framesCopy = frames;
            final List<String> previous = new ArrayList<>();
            final String currentNameFixed = ChatColor.stripColor(displayName);

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;

            @Override
            public void run() {
                resetWhenFrameFinished();

                if (cancelIfListIsEmpty()) return;

                char[] currentFrames = updateFramesWhenRequired();

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                String letter = String.valueOf(this.letters[this.subStringIndex]);
                String rest = this.currentNameFixed.substring(this.subStringIndex + 1);
                boolean addColor = !letter.equals(" ");

                StringBuilder newString = new StringBuilder();
                if (this.subStringIndex != 0)
                    this.previous.forEach(newString::append);

                newString.append(itemColor.getColor())
                        .append(itemColor.isBold() ? "§l" : "")
                        .append(itemColor.isUnderline() ? "§n" : "")
                        .append(itemColor.isItalic() ? "§o" : "")
                        .append(itemColor.isObfuscated() ? "§k" : "")
                        .append(itemColor.isStrikeThrough() ? "§m" : "")
                        .append(letter);

                String currentName = newString
                        .append(ChatColor.WHITE).append(rest)
                        .toString();

                this.previous.add(newString.toString());

                this.subStringIndex++;

                if (!addColor) return;

                this.colorState++;
                updateDisplayName(contents, currentName);
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
                    return true;
                }
                return false;
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                if (this.colorState < currentFrames.length) return currentFrames;

                this.colorState = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private void resetWhenFrameFinished() {
                if (this.subStringIndex < this.letters.length) return;

                if (!loop)
                    this.framesCopy.remove(0);
                this.colorState = 0;
                this.subStringIndex = 0;
                this.previous.clear();

                if (this.currentFrameIndex + 1 < this.framesCopy.size())
                    return;

                this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    private void animateWordByWord() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(displayName).toCharArray();
            final List<String> framesCopy = frames;

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            String currentName = "";

            @Override
            public void run() {
                resetWhenFrameFinished();

                if (cancelIfListIsEmpty()) return;

                char[] currentFrames = updateFramesWhenRequired();

                String letter = String.valueOf(this.letters[this.subStringIndex]);
                boolean addColor = !letter.equals(" ");

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                this.currentName = this.currentName + itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + letter;

                this.subStringIndex++;

                if (!addColor) return;

                this.colorState++;
                updateDisplayName(contents, this.currentName);
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();
                if (this.colorState < currentFrames.length) return currentFrames;

                this.colorState = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
                    return true;
                }
                return false;
            }

            private void resetWhenFrameFinished() {
                if (this.subStringIndex < this.letters.length) return;

                if (!loop)
                    this.framesCopy.remove(0);
                this.colorState = 0;
                this.subStringIndex = 0;
                this.currentName = "";

                if (this.currentFrameIndex + 1 < this.framesCopy.size())
                    return;

                this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    private void updateDisplayName(@NotNull InventoryContents contents, @NotNull String currentName) {
        ItemStack itemStack = new ItemStack(intelligentItem.getItemStack());

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(currentName);
        itemStack.setItemMeta(itemMeta);

        contents.update(slot, itemStack);
    }

    protected @NotNull BukkitTask getTask() {
        return this.task;
    }

    public @Nullable Object getIdentifier() {
        return this.identifier;
    }

    public static class Builder {

        private IntelligentItemNameAnimator preset;

        private IntelligentItem intelligentItem;
        private String displayName;
        private List<String> frames = new ArrayList<>();
        private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
        private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
        private int period = 20;
        private int delay = 0;
        private int slot = -1;

        private boolean loop;
        private Object identifier;

        /**
         * This tells which item is to be animated.
         *
         * @param intelligentItem
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder item(@NotNull IntelligentItem intelligentItem) {
            this.intelligentItem = intelligentItem;
            ItemStack itemStack = this.intelligentItem.getItemStack();
            this.displayName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                    ? ChatColor.translateAlternateColorCodes('&', itemStack.getItemMeta().getDisplayName())
                    : itemStack.getType().name();
            return this;
        }


        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder copy(@NotNull IntelligentItemNameAnimator preset) {
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
         * Decides how the name of the item should be animated.
         *
         * @param type The animation type
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder type(@NotNull IntelligentItemAnimatorType type) {
            this.type = type;
            return this;
        }

        /**
         * This tells us in which slot the animation should take place.
         *
         * @param slot The slot to be animated.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot > 53
         */
        public @NotNull Builder slot(@Nonnegative int slot) throws IllegalArgumentException {
            if (slot > 53)
                throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

            this.slot = slot;
            return this;
        }

        /**
         * Assigns a color to a frame.
         *
         * @param frame The frame that should receive the color.
         * @param color The color you want the frame to have.
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder color(char frame, @NotNull IntelligentItemColor color) {
            this.frameColor.put(frame, color);
            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames The frames that should receive the color.
         * @param color  The color you want the frames to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder colors(@NotNull List<Character> frames, IntelligentItemColor @NotNull ... color) throws IllegalArgumentException {
            Preconditions.checkArgument(frames.size() == color.length, StringConstants.INVALID_COLOR_FRAME);

            for (int i = 0; i < frames.size(); i++)
                color(frames.get(i), color[i]);

            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames The frames that should receive the color.
         * @param color  The color you want the frames to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder colors(Character @NotNull [] frames, IntelligentItemColor @NotNull ... color) {
            Preconditions.checkArgument(frames.length == color.length, StringConstants.INVALID_COLOR_FRAME);

            for (int i = 0; i < frames.length; i++)
                color(frames[i], color[i]);

            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames The frames that should receive the color.
         * @param color  The color you want the frames to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder colors(Character @NotNull [] frames, @NotNull List<IntelligentItemColor> color) {
            Preconditions.checkArgument(frames.length == color.size(), StringConstants.INVALID_COLOR_FRAME);

            for (int i = 0; i < frames.length; i++)
                color(frames[i], color.get(i));

            return this;
        }

        /**
         * Adds another frame.
         *
         * @param frame The frame to be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
         */
        public @NotNull Builder frame(@NotNull String frame) throws IllegalArgumentException {
            for (char c : frame.toCharArray()) {
                if (this.frameColor.containsKey(c)) continue;
                throw new IllegalArgumentException("The letter " + c + " has not yet been assigned a color.");
            }

            this.frames.add(frame);
            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames The frames to be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
         */
        public @NotNull Builder frames(String @NotNull ... frames) {
            for (String frame : frames)
                frame(frame);

            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames The frames to be added.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
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
         * @apiNote When copying the animator, the identification is not copied if present!
         */
        public @NotNull Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link IntelligentItemNameAnimator#animate()}
         *
         * @return The animation class
         * @throws IllegalArgumentException if no slot was specified, if frameColor is empty, if frames is empty or if no color has been assigned to a frame.
         * @throws NullPointerException     if item is null.
         */
        public @NotNull IntelligentItemNameAnimator build(@NotNull InventoryContents contents) throws IllegalArgumentException, NullPointerException {
            if (this.preset != null) {
                this.intelligentItem = this.preset.intelligentItem;
                this.displayName = this.preset.displayName;
                this.frames = this.preset.frames;
                this.frameColor = this.preset.frameColor;
                this.type = this.preset.type;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.slot = this.preset.slot;
                this.loop = this.preset.loop;
            }

            if (this.slot == -1)
                throw new IllegalArgumentException("Please specify a slot where the item is located.");

            if (this.frameColor.isEmpty())
                throw new IllegalArgumentException("Please specify a color for each frame.");

            if (this.intelligentItem == null)
                throw new NullPointerException("An IntelligentItem must be passed.");

            if (this.frames.isEmpty())
                throw new IllegalArgumentException("Please specify at least one frame.");


            for (String frame : this.frames) {
                for (char c : frame.toCharArray()) {
                    if (frameColor.containsKey(c)) continue;
                    throw new IllegalArgumentException("You created the frame " + frame + ", but the letter " + c + " was not assigned a color.");
                }
            }

            IntelligentItemNameAnimator animator = new IntelligentItemNameAnimator();
            animator.intelligentItem = this.intelligentItem;
            animator.delay = this.delay;
            animator.displayName = this.displayName;
            animator.frameColor = this.frameColor;
            animator.frames = this.frames;
            animator.loop = this.loop;
            animator.period = this.period;
            animator.slot = this.slot;
            animator.type = this.type;
            animator.identifier = this.identifier;
            animator.contents = contents;
            animator.inventory = contents.pagination().inventory();
            return animator;
        }
    }
}
