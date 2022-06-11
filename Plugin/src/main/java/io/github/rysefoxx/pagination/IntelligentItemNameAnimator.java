package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemAnimatorType;
import io.github.rysefoxx.content.IntelligentItemColor;
import io.github.rysefoxx.enums.TimeSetting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulcode(eazypaulCode#0001) |
 * @since 4/12/2022
 */
public class IntelligentItemNameAnimator {

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
    private static Plugin plugin;

    public static Builder builder(Plugin plugin) {
        IntelligentItemNameAnimator.plugin = plugin;
        return new Builder();
    }

    /**
     * @throws UnsupportedOperationException if called
     * @deprecated Use {@link #builder(Plugin)} instead.
     */
    @Deprecated
    public static Builder builder() {
        throw new UnsupportedOperationException("Use builder(Plugin) instead.");
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
        public Builder item(IntelligentItem intelligentItem) {
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
        public Builder copy(IntelligentItemNameAnimator preset) {
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
        public Builder type(IntelligentItemAnimatorType type) {
            this.type = type;
            return this;
        }

        /**
         * This tells us in which slot the animation should take place.
         *
         * @param slot
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot > 53
         */
        public Builder slot(@Nonnegative int slot) throws IllegalArgumentException {
            if (slot > 53) {
                throw new IllegalArgumentException("The slot must not be larger than 53.");
            }
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
        public Builder color(char frame, IntelligentItemColor color) {
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
        public Builder colors(List<Character> frames, IntelligentItemColor... color) throws IllegalArgumentException {
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
        public Builder colors(Character[] frames, IntelligentItemColor... color) {
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
        public Builder colors(Character[] frames, List<IntelligentItemColor> color) {
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
        public Builder frame(String frame) throws IllegalArgumentException {
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
        public Builder frames(String... frames) {
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
        public Builder frames(List<String> frames) {
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
        public Builder period(@Nonnegative int time, TimeSetting setting) {
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
        public Builder delay(@Nonnegative int time, TimeSetting setting) {
            this.delay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Gives the Animation an identification
         *
         * @param identifier The ID through which you can get the animation
         * @return The Builder to perform further editing
         * @apiNote When copying the animator, the identification is not copied if present!
         */
        public Builder identifier(Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link IntelligentItemNameAnimator#animate(Plugin)}
         *
         * @return The animation class
         * @throws IllegalArgumentException if no slot was specified, if frameColor is empty, if frames is empty or if no color has been assigned to a frame.
         * @throws NullPointerException     if item is null.
         */
        public IntelligentItemNameAnimator build(InventoryContents contents) throws IllegalArgumentException, NullPointerException {
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

            if (this.slot == -1) {
                throw new IllegalArgumentException("Please specify a slot where the item is located.");
            }
            if (this.frameColor.isEmpty()) {
                throw new IllegalArgumentException("Please specify a color for each frame.");
            }
            if (this.intelligentItem == null) {
                throw new NullPointerException("An IntelligentItem must be passed.");
            }
            if (this.frames.isEmpty()) {
                throw new IllegalArgumentException("Please specify at least one frame.");
            }

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

    /**
     * This starts the animation for the item.
     */
    public void animate() {
        this.inventory.addItemAnimator(this);
        animateByType();
    }

    /**
     * This starts the animation for the item.
     *
     * @param plugin Your main class that extends the Plugin.
     * @deprecated Use {@link IntelligentItemNameAnimator#animate()} instead.
     */
    @Deprecated
    public void animate(Plugin plugin) {
        animate();
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
            String currentName = "";

            @Override
            public void run() {
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorState = 0;
                    this.subStringIndex = 0;
                    this.currentName = "";
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
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

                this.currentName = itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + fixedDisplayName;

                this.colorState++;
                this.subStringIndex++;
                updateDisplayName(contents, this.currentName);
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
            String currentName = ChatColor.stripColor(displayName);

            @Override
            public void run() {
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorState = 0;
                    this.subStringIndex = 0;
                    this.previous.clear();
                    this.currentName = ChatColor.stripColor(displayName);
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
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

                String letter = String.valueOf(this.letters[this.subStringIndex]);
                String rest = this.currentNameFixed.substring(this.subStringIndex + 1);
                boolean addColor = !letter.isEmpty();

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
                this.currentName = newString.toString();

                this.previous.add(itemColor.getColor()
                        + (itemColor.isBold() ? "§l" : "")
                        + (itemColor.isUnderline() ? "§n" : "")
                        + (itemColor.isItalic() ? "§o" : "")
                        + (itemColor.isObfuscated() ? "§k" : "")
                        + (itemColor.isStrikeThrough() ? "§m" : "")
                        + letter);

                this.subStringIndex++;

                if (!addColor) return;

                this.colorState++;
                Bukkit.broadcastMessage(this.currentName);
                updateDisplayName(contents, this.currentName);
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
                if (this.subStringIndex >= this.letters.length) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.colorState = 0;
                    this.subStringIndex = 0;
                    this.currentName = "";
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeItemAnimator(IntelligentItemNameAnimator.this);
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
                boolean addColor = !letter.isEmpty();

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
        }, this.delay, this.period);
    }

    private void updateDisplayName(InventoryContents contents, String currentName) {
        ItemStack itemStack = new ItemStack(intelligentItem.getItemStack());

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(currentName);
        itemStack.setItemMeta(itemMeta);

        contents.update(slot, itemStack);
    }

    protected BukkitTask getTask() {
        return this.task;
    }

    protected Object getIdentifier() {
        return this.identifier;
    }
}
