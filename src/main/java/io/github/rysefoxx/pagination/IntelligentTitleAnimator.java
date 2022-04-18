package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.content.IntelligentItemColor;
import io.github.rysefoxx.opener.InventoryOpenerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulcode(eazypaulCode#0001) |
 * @apiNote The title animation is currently only available for Chest or EnderChest. Other inventory types like BREWING_STAND will not work!
 * @since 4/12/2022
 */
public class IntelligentTitleAnimator {

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

    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
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
         * @apiNote When copying the animator, the identification is not copied if present!
         */
        public Builder copy(@NotNull IntelligentTitleAnimator preset) {
            this.preset = preset;
            return this;
        }

        /**
         * Keeps the animation running until the player closes the inventory.
         *
         * @return The Builder to perform further editing.
         */
        public Builder loop() {
            this.loop = true;
            return this;
        }

        /**
         * Decides how the name of the item should be animated.
         *
         * @param type The animation type
         * @return The Builder to perform further editing.
         */
        public Builder type(@NotNull IntelligentItemAnimatorType type) {
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
        public Builder color(char frame, @NotNull IntelligentItemColor color) {
            this.frameColor.put(frame, color);
            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames
         * @param color
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public Builder colors(@NotNull List<Character> frames, IntelligentItemColor @NotNull ... color) {
            Preconditions.checkArgument(frames.size() == color.length, "Frames must have the same length as color.");

            for (int i = 0; i < frames.size(); i++) {
                color(frames.get(i), color[i]);
            }
            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames
         * @param color
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public Builder colors(Character @NotNull [] frames, IntelligentItemColor @NotNull ... color) {
            Preconditions.checkArgument(frames.length == color.length, "Frames must have the same length as color.");

            for (int i = 0; i < frames.length; i++) {
                color(frames[i], color[i]);
            }
            return this;
        }

        /**
         * Several frames are assigned individual colors.
         *
         * @param frames
         * @param color
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameters are not equal.
         */
        public Builder colors(Character @NotNull [] frames, @NotNull List<IntelligentItemColor> color) {
            Preconditions.checkArgument(frames.length == color.size(), "Frames must have the same length as color.");

            for (int i = 0; i < frames.length; i++) {
                color(frames[i], color.get(i));
            }
            return this;
        }

        /**
         * Adds another frame.
         *
         * @param frame
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
         */
        public Builder frame(@NotNull String frame) throws IllegalArgumentException {
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
         * @param frames
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
         */
        public Builder frames(@NotNull String @NotNull ... frames) {
            for (String frame : frames) {
                frame(frame);
            }
            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no color has been assigned to the frame yet. e.g {@link IntelligentItemNameAnimator.Builder#colors(List, IntelligentItemColor...)}
         */
        public Builder frames(@NotNull List<String> frames) {
            frames.forEach(this::frame);
            return this;
        }

        /**
         * Sets the speed of the animation in the scheduler.
         *
         * @param time
         * @param setting
         * @return The Builder to perform further editing.
         */
        public Builder period(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.period = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Specifies the delay before the animation starts.
         *
         * @param time
         * @param setting
         * @return The Builder to perform further editing.
         */
        public Builder delay(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.delay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Gives the Animation an identification
         *
         * @param identifier The ID through which you can get the animation
         * @return The Builder to perform further editing
         */
        public Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link IntelligentTitleAnimator#animate(JavaPlugin, Player)}
         *
         * @return The animation class
         * @throws IllegalArgumentException if frameColor is empty, if frames is empty or if no color has been assigned to a frame.
         */
        public IntelligentTitleAnimator build(@NotNull InventoryContents contents) throws IllegalArgumentException {
            InventoryOpenerType type = contents.pagination().inventory().getInventoryOpenerType();

            if (type != InventoryOpenerType.CHEST
                    && type != InventoryOpenerType.ENDER_CHEST
                    && type != InventoryOpenerType.DROPPER
                    && type != InventoryOpenerType.DISPENSER) {
                throw new IllegalStateException("The title animation is currently only available for Chest or EnderChest.");
            }
            if (this.preset != null) {
                this.frames = this.preset.frames;
                this.frameColor = this.preset.frameColor;
                this.type = this.preset.type;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.loop = this.preset.loop;
            }

            if (this.frameColor.isEmpty()) {
                throw new IllegalArgumentException("No colors have been defined yet!");
            }
            if (this.frames.isEmpty()) {
                throw new IllegalArgumentException("You need to set a possible pattern.");
            }

            for (String frame : this.frames) {
                for (char c : frame.toCharArray()) {
                    if (frameColor.containsKey(c)) continue;
                    throw new IllegalArgumentException("You created the frame " + frame + ", but the letter " + c + " was not assigned a color.");
                }
            }

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
    }

    /**
     * This starts the animation for the item.
     *
     * @param plugin Your main class that extends the JavaPlugin.
     * @param player the player
     */
    public void animate(@NotNull JavaPlugin plugin, @NotNull Player player) {
        this.inventory.addTitleAnimator(this);
        animateByType(plugin, player);
    }

    private void animateByType(@NotNull JavaPlugin plugin, @NotNull Player player) {
        if (this.type == IntelligentItemAnimatorType.FULL_WORD) {
            animateByFullWord(plugin, player);
            return;
        }
        if (this.type == IntelligentItemAnimatorType.WORD_BY_WORD) {
            animateWordByWord(plugin, player);
            return;
        }
        if (type == IntelligentItemAnimatorType.FLASH) {
            animateWithFlash(plugin, player);
        }
    }

    private void animateWithFlash(@NotNull JavaPlugin plugin, @NotNull Player player) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(title).toCharArray();
            final List<String> framesCopy = frames;
            final String fixedTitle = ChatColor.stripColor(title);

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            String currentTitle = "";

            @Override
            public void run() {
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorState = 0;
                    this.subStringIndex = 0;
                    this.currentTitle = "";
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
                    return;
                }

                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();
                if (this.colorState >= currentFrames.length) {
                    this.colorState = 0;
                    if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                        this.currentFrameIndex++;
                        currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                    }
                }

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                this.currentTitle = itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + fixedTitle;

                this.colorState++;
                this.subStringIndex++;
                inventory.updateTitle(player, this.currentTitle);
            }
        }, this.delay, this.period);
    }

    private void animateByFullWord(@NotNull JavaPlugin plugin, @NotNull Player player) {
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
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorIndex = 0;
                    this.subStringIndex = 0;
                    this.previous.clear();
                    this.currentTitle = title;
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;

                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
                    return;
                }

                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();
                if (this.colorIndex >= currentFrames.length) {
                    this.colorIndex = 0;
                    if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                        this.currentFrameIndex++;
                        currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                    }
                }
                char singleFrame = currentFrames[this.colorIndex];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                String letter = String.valueOf(this.letters[this.subStringIndex]);
                String rest = this.currentTitleFixed.substring(this.subStringIndex + 1);
                boolean addColor = !letter.isBlank();

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
                        .append(letter)
                        .append(ChatColor.WHITE).append(rest);
                this.currentTitle = newString.toString();

                this.previous.add(itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + letter);

                this.subStringIndex++;

                if (!addColor) return;

                this.colorIndex++;
                inventory.updateTitle(player, this.currentTitle);
            }
        }, this.delay, this.period);
    }

    private void animateWordByWord(@NotNull JavaPlugin plugin, @NotNull Player player) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final char[] letters = ChatColor.stripColor(title).toCharArray();
            final List<String> framesCopy = frames;

            int colorState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            String currentTitle = "";

            @Override
            public void run() {
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorState = 0;
                    this.subStringIndex = 0;
                    this.currentTitle = "";
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeTitleAnimator(IntelligentTitleAnimator.this);
                    return;
                }

                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();
                if (this.colorState >= currentFrames.length) {
                    this.colorState = 0;
                    if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                        this.currentFrameIndex++;
                        currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                    }
                }
                String letter = String.valueOf(this.letters[this.subStringIndex]);
                boolean addColor = !letter.isBlank();

                char singleFrame = currentFrames[this.colorState];
                IntelligentItemColor itemColor = frameColor.get(singleFrame);

                this.currentTitle = this.currentTitle + itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + letter;

                this.subStringIndex++;

                if (!addColor) return;

                this.colorState++;
                inventory.updateTitle(player, this.currentTitle);
            }
        }, this.delay, this.period);
    }

    protected @NotNull BukkitTask getTask() {
        return this.task;
    }

    protected @NotNull Object getIdentifier() {
        return this.identifier;
    }
}
