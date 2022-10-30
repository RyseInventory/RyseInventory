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

package io.github.rysefoxx.inventory.plugin.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItemColor;
import io.github.rysefoxx.inventory.plugin.enums.TimeSetting;
import io.github.rysefoxx.inventory.plugin.util.StringConstants;
import io.github.rysefoxx.inventory.plugin.util.TimeUtils;
import io.github.rysefoxx.inventory.plugin.util.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author Rysefoxx(Rysefoxx # 6772)
 * <p>
 * The title animation is currently only available for Chest or EnderChest. Other inventory types like BREWING_STAND will not work!
 * @since 4/12/2022
 */
public class IntelligentTitleAnimator {

    private static Plugin plugin;
    private List<String> frames = new ArrayList<>();
    private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
    private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
    private int period = 20;
    private int delay = 0;
    private BukkitTask task;
    private boolean loop;
    private String title;
    private RyseInventory inventory;
    private Object identifier;

    @Contract("_ -> new")
    public static @NotNull Builder builder(@NotNull Plugin plugin) {
        IntelligentTitleAnimator.plugin = plugin;
        return new Builder();
    }

    /**
     * @param player the player to send the title to.
     *               This starts the animation for the item.
     */
    public void animate(@NotNull Player player) {
        this.inventory.addTitleAnimator(this);
        animateByType(player);
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
     * If the type is FULL_WORD, animate by full word, if the type is WORD_BY_WORD, animate word by word, if the type is
     * FLASH, animate with flash
     *
     * @param player The player to animate the item for.
     */
    private void animateByType(@NotNull Player player) {
        if (this.type == IntelligentItemAnimatorType.FULL_WORD) {
            animateByFullWord(player);
            return;
        }
        if (this.type == IntelligentItemAnimatorType.WORD_BY_WORD) {
            animateWordByWord(player);
            return;
        }
        if (type == IntelligentItemAnimatorType.FLASH)
            animateWithFlash(player);
    }

    /**
     * It takes the frames and the title, and then it loops through the frames and the title, and then it updates the title
     * with the current frame and the current letter
     *
     * @param player The player to animate the title for.
     */
    private void animateWithFlash(@NotNull Player player) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(title).toCharArray();
            final List<String> framesCopy = frames;
            final String fixedTitle = ChatColor.stripColor(title);

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

                String currentTitle =
                        itemColor.getColor()
                                + (itemColor.isBold() ? "§l" : "")
                                + (itemColor.isUnderline() ? "§n" : "")
                                + (itemColor.isItalic() ? "§o" : "")
                                + (itemColor.isObfuscated() ? "§k" : "")
                                + (itemColor.isStrikeThrough() ? "§m" : "")
                                + fixedTitle;

                this.colorState++;
                this.subStringIndex++;
                inventory.updateTitle(player, currentTitle);
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                if (this.colorState < currentFrames.length)
                    return currentFrames;

                this.colorState = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
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

    /**
     * It takes the title, splits it into letters, and then adds the colors from the frames to the letters
     *
     * @param player The player to animate the title for.
     */
    private void animateByFullWord(@NotNull Player player) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(title).toCharArray();
            final List<String> framesCopy = frames;
            final List<String> previous = new ArrayList<>();
            final String currentTitleFixed = Objects.requireNonNull(ChatColor.stripColor(title));

            int colorIndex = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            String currentTitle = ChatColor.stripColor(title);

            @Override
            public void run() {
                resetWhenFrameFinished();

                if (cancelIfListIsEmpty()) return;

                char[] currentFrames = updateFramesWhenRequired();
                char singleFrame = currentFrames[this.colorIndex];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                String letter = String.valueOf(this.letters[this.subStringIndex]);
                String rest = this.currentTitleFixed.substring(this.subStringIndex + 1);
                boolean addColor = !letter.equals(" ");

                StringBuilder newString = buildTitleBasedOnPreviousTitle(itemColor, letter);

                this.currentTitle = newString
                        .append(ChatColor.WHITE).append(rest)
                        .toString();

                this.previous.add(newString.toString());

                this.subStringIndex++;

                if (!addColor) return;

                this.colorIndex++;
                inventory.updateTitle(player, this.currentTitle);
            }

            @NotNull
            private StringBuilder buildTitleBasedOnPreviousTitle(@NotNull IntelligentItemColor itemColor, @NotNull String letter) {
                StringBuilder newString = new StringBuilder();

                if (this.subStringIndex != 0)
                    this.previous.forEach(newString::append);

                newString
                        .append(itemColor.getColor())
                        .append(itemColor.isBold() ? "§l" : "")
                        .append(itemColor.isUnderline() ? "§n" : "")
                        .append(itemColor.isItalic() ? "§o" : "")
                        .append(itemColor.isObfuscated() ? "§k" : "")
                        .append(itemColor.isStrikeThrough() ? "§m" : "")
                        .append(letter);
                return newString;
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                if (this.colorIndex < currentFrames.length)
                    return currentFrames;

                this.colorIndex = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
                    return true;
                }
                return false;
            }

            private void resetWhenFrameFinished() {
                if (this.subStringIndex < this.letters.length) return;

                if (!loop)
                    this.framesCopy.remove(0);
                this.colorIndex = 0;
                this.subStringIndex = 0;
                this.previous.clear();
                this.currentTitle = title;

                if (this.currentFrameIndex + 1 < this.framesCopy.size())
                    return;

                this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    /**
     * It takes the title, splits it into letters, and then adds the colors from the frames to the letters
     *
     * @param player The player to animate the title for.
     */
    private void animateWordByWord(@NotNull Player player) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(title).toCharArray();
            final List<String> framesCopy = frames;

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            String currentTitle = "";

            @Override
            public void run() {
                resetWhenFrameFinished();
                String letter = String.valueOf(this.letters[this.subStringIndex]);

                if (VersionUtils.isBelowAnd13()) {
                    this.currentTitle = this.currentTitle + letter;

                    this.subStringIndex++;
                    inventory.updateTitle(player, this.currentTitle);
                    return;
                }

                if (cancelIfListIsEmpty()) return;

                char[] currentFrames = updateFramesWhenRequired();
                boolean addColor = !letter.equals(" ");

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                appendLetterToTitle(letter, itemColor);

                this.subStringIndex++;

                if (!addColor) return;

                this.colorState++;
                inventory.updateTitle(player, this.currentTitle);
            }

            private void appendLetterToTitle(@NotNull String letter, @NotNull IntelligentItemColor itemColor) {
                this.currentTitle = this.currentTitle
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + itemColor.getColor()
                        + letter;
            }

            private char @NotNull [] updateFramesWhenRequired() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                if (this.colorState < currentFrames.length)
                    return currentFrames;

                this.colorState = 0;
                if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                    this.currentFrameIndex++;
                    currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                }
                return currentFrames;
            }

            private boolean cancelIfListIsEmpty() {
                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
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
                this.currentTitle = "";

                if (this.currentFrameIndex + 1 < this.framesCopy.size())
                    return;

                this.currentFrameIndex = 0;
            }
        }, this.delay, this.period);
    }

    /**
     * This function returns the task that is currently running.
     *
     * @return The task that is being run.
     */
    protected @NotNull BukkitTask getTask() {
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

    public static class Builder {

        private IntelligentTitleAnimator preset;

        private List<String> frames = new ArrayList<>();
        private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
        private int period = 20;
        private int delay = 0;
        private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
        private boolean loop;
        private Object identifier;

        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         * <p>
         * When copying the animator, the identification is not copied if present!
         */
        public @NotNull Builder copy(@NotNull IntelligentTitleAnimator preset) {
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
            if (VersionUtils.isBelowAnd13() && ((type == IntelligentItemAnimatorType.FULL_WORD) || type == IntelligentItemAnimatorType.FLASH))
                throw new IllegalArgumentException("The " + type.name() + " animation makes no sense under inclusive with version 13.");

            this.type = type;
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
         * @param color The color you want the frames to have.
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public @NotNull Builder colors(@NotNull List<Character> frames, IntelligentItemColor @NotNull ... color) {
            Preconditions.checkArgument(frames.size() == color.length, StringConstants.INVALID_COLOR_FRAME);

            for (int i = 0; i < frames.size(); i++)
                color(frames.get(i), color[i]);

            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames The frames that should receive the color.
         * @param color The color you want the frames to have.
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
         * @param color The color you want the frames to have.
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
         * @param setting The time setting
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
         * @param setting The time setting
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
         */
        public @NotNull Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link IntelligentTitleAnimator#animate(Player)}
         *
         * @param contents The contents of the inventory.
         * @return The animation class
         * @throws IllegalArgumentException if frameColor is empty, if frames is empty or if no color has been assigned to a frame.
         */
        public IntelligentTitleAnimator build(@NotNull InventoryContents contents) throws IllegalArgumentException {
            if (this.preset != null) {
                this.frames = this.preset.frames;
                this.frameColor = this.preset.frameColor;
                this.type = this.preset.type;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.loop = this.preset.loop;
            }

            checkIfValid();

            IntelligentTitleAnimator animator = new IntelligentTitleAnimator();
            animator.delay = this.delay;
            animator.frameColor = this.frameColor;
            animator.frames = this.frames;
            animator.loop = this.loop;
            animator.period = this.period;
            animator.type = this.type;
            animator.identifier = this.identifier;
            animator.inventory = contents.pagination().inventory();
            animator.title = contents.pagination().inventory().getTitle();
            return animator;
        }

        private void checkIfValid() {
            if (VersionUtils.isBelowAnd13()) {
                if (!this.frameColor.isEmpty())
                    throw new IllegalStateException("Anything less than inclusive with version 13 does not yet support titles with color. Please remove code with #color() or #colors()");

                if (!this.frames.isEmpty())
                    throw new IllegalArgumentException("Anything less than inclusive with version 13 does not yet support titles with color. Accordingly, the code can be removed with #frame or #frames.");

                return;
            }

            if (this.frameColor.isEmpty())
                throw new IllegalArgumentException("You must specify at least one frame with #color() or #colors()");

            if (this.frames.isEmpty())
                throw new IllegalArgumentException("No frames have been defined yet!");

            for (String frame : this.frames) {
                for (char c : frame.toCharArray()) {
                    if (frameColor.containsKey(c)) continue;
                    throw new IllegalArgumentException("You created the frame " + frame + ", but the letter " + c + " was not assigned a color.");
                }
            }
        }
    }
}
