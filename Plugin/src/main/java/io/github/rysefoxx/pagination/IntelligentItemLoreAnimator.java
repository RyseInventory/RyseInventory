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
import java.util.Map;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulcode(eazypaulCode#0001) |
 * @since 4/12/2022
 */
public class IntelligentItemLoreAnimator {

    private IntelligentItem intelligentItem;
    private HashMap<Integer, String> loreData = new HashMap<>();
    private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
    private final List<BukkitTask> task = new ArrayList<>();
    private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
    private int period = 20;
    private int delay = 0;
    private int slot = -1;

    private boolean loop;
    private List<String> lore;
    private RyseInventory inventory;
    private InventoryContents contents;
    private ItemStack itemStack;
    private Object identifier;
    private static Plugin plugin;

    public static Builder builder(Plugin plugin) {
        IntelligentItemLoreAnimator.plugin = plugin;
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
        private HashMap<Character, IntelligentItemColor> frameColor = new HashMap<>();
        private HashMap<Integer, String> loreData = new HashMap<>();
        private IntelligentItemAnimatorType type = IntelligentItemAnimatorType.WORD_BY_WORD;
        private int period = 20;
        private int delay = 0;
        private int slot = -1;

        private IntelligentItemLoreAnimator preset;
        private IntelligentItem intelligentItem;
        private List<String> lore;
        private boolean loop;
        private Object identifier;

        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         * @apiNote When copying the animator, the identification is not copied if present!
         */
        public Builder copy(IntelligentItemLoreAnimator preset) {
            this.preset = preset;
            return this;
        }

        /**
         * A frame is added to the index. The frame is used to change the color of the lore.
         *
         * @param index e.g 0 for the first line in the lore.
         * @param frame e.g AABB (A and B must then each be defined with {@link #color(char, IntelligentItemColor)}).
         * @return The Builder to perform further editing
         */
        public Builder lore(@Nonnegative int index, String frame) {
            this.loreData.put(index, frame);
            return this;
        }

        /**
         * Gives the Animation an identification
         *
         * @param identifier The ID through which you can get the animation
         * @return The Builder to perform further editing
         */
        public Builder identifier(Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This tells which item is to be animated.
         *
         * @param intelligentItem
         * @return The Builder to perform further editing.
         */
        public Builder item(IntelligentItem intelligentItem) {
            this.intelligentItem = intelligentItem;
            ItemStack itemStack = this.intelligentItem.getItemStack();

            List<String> lore = itemStack.hasItemMeta() ? itemStack.getItemMeta().getLore() : new ArrayList<>();

            if (lore == null) {
                throw new NullPointerException("The given item has no lore.");
            }

            if (lore.isEmpty()) {
                throw new IllegalArgumentException("The passed item has an empty lore.");
            }

            this.lore = lore;
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
        public Builder slot(@Nonnegative int slot) {
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
         * This creates the animation class but does not start it yet! {@link IntelligentItemLoreAnimator#animate()}
         *
         * @return The animation class
         * @throws IllegalArgumentException if no slot was specified, if lore is empty, if frameColor is empty or if loreData is empty.
         * @throws NullPointerException     if item is null or if lore is null.
         */
        public IntelligentItemLoreAnimator build(InventoryContents contents) throws IllegalArgumentException, NullPointerException {
            if (this.preset != null) {
                this.intelligentItem = this.preset.intelligentItem;
                this.lore = this.preset.lore;
                this.loreData = this.preset.loreData;
                this.type = this.preset.type;
                this.frameColor = this.preset.frameColor;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.slot = this.preset.slot;
                this.loop = this.preset.loop;
            }
            if (this.intelligentItem == null) {
                throw new NullPointerException("An IntelligentItem must be passed.");
            }
            if (this.lore == null) {
                throw new NullPointerException("The lore of the item must not be null.");
            }
            if (this.lore.isEmpty()) {
                throw new IllegalArgumentException("The passed item has an empty lore.");
            }
            if (this.slot == -1) {
                throw new IllegalArgumentException("Please specify a slot where the item is located.");
            }
            if (this.frameColor.isEmpty()) {
                throw new IllegalArgumentException("Please specify a color for each frame.");
            }
            if (this.loreData.isEmpty()) {
                throw new IllegalArgumentException("No lore data has been defined yet!");
            }

            for (Map.Entry<Integer, String> entry : this.loreData.entrySet()) {
                for (char c : entry.getValue().toCharArray()) {
                    if (frameColor.containsKey(c)) continue;
                    throw new IllegalArgumentException("The pattern contains a character that is not defined: " + c + ". Please define a color for this character.");
                }
                if ((entry.getKey() + 1) <= this.lore.size()) continue;
                throw new IllegalArgumentException("You passed the index " + entry.getKey() + ", but the lore only has a size of " + this.lore.size() + ".");
            }

            IntelligentItemLoreAnimator animator = new IntelligentItemLoreAnimator();
            animator.delay = this.delay;
            animator.lore = this.lore;
            animator.frameColor = this.frameColor;
            animator.loop = this.loop;
            animator.period = this.period;
            animator.intelligentItem = this.intelligentItem;
            animator.slot = this.slot;
            animator.type = this.type;
            animator.loreData = this.loreData;
            animator.identifier = this.identifier;
            animator.itemStack = this.intelligentItem.getItemStack();
            animator.contents = contents;
            animator.inventory = contents.pagination().inventory();
            return animator;
        }
    }

    /**
     * This starts the animation for the item.
     */
    public void animate() {
        this.inventory.addLoreAnimator(this);
        animateByType();
    }

    /**
     * This starts the animation for the item.
     *
     * @param plugin Your main class the Plugin extended.
     * @deprecated Use {@link #animate()} instead.
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
        if (type == IntelligentItemAnimatorType.FLASH) {
            animateWithFlash();
        }
    }

    private void animateWithFlash() {
        for (Map.Entry<Integer, String> entry : this.loreData.entrySet()) {
            BukkitTask task;
            task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                final HashMap<Integer, String> framesCopy = loreData;

                int subStringIndex = 0;
                int colorState = 0;
                int currentFrameIndex = 0;
                String currentLore = "";

                @Override
                public void run() {
                    int loreIndex = entry.getKey();
                    String frame = framesCopy.get(loreIndex);
                    char[] currentFrames = frame.toCharArray();

                    if (this.subStringIndex >= currentFrames.length) {
                        if (!loop)
                            this.framesCopy.remove(loreIndex);
                        this.colorState = 0;
                        this.subStringIndex = 0;
                        this.currentLore = "";
                        if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                            this.currentFrameIndex = 0;
                    }

                    if (this.framesCopy.isEmpty()) {
                        inventory.removeLoreAnimator(IntelligentItemLoreAnimator.this);
                        return;
                    }

                    char singleFrame = currentFrames[this.colorState];
                    IntelligentItemColor itemColor = frameColor.get(singleFrame);

                    this.currentLore = itemColor.getColor()
                            + (itemColor.isBold() ? "§l" : "")
                            + (itemColor.isUnderline() ? "§n" : "")
                            + (itemColor.isItalic() ? "§o" : "")
                            + (itemColor.isObfuscated() ? "§k" : "")
                            + (itemColor.isStrikeThrough() ? "§m" : "")
                            + ChatColor.stripColor(lore.get(loreIndex));

                    this.colorState++;
                    this.subStringIndex++;
                    updateLore(contents, this.currentLore, loreIndex);
                }
            }, this.delay, this.period);
            this.task.add(task);
        }


    }

    private void animateByFullWord() {
        for (Map.Entry<Integer, String> entry : this.loreData.entrySet()) {
            BukkitTask task;
            task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                final HashMap<Integer, String> framesCopy = loreData;
                final List<String> previous = new ArrayList<>();

                int colorState = 0;
                int subStringIndex = 0;
                int currentFrameIndex = 0;
                String currentLore = "";

                @Override
                public void run() {

                    int loreIndex = entry.getKey();
                    String frame = framesCopy.get(loreIndex);
                    char[] currentFrames = frame.toCharArray();
                    currentLore = lore.get(loreIndex);
                    String currentLoreFixed = lore.get(loreIndex);
                    char[] letters = this.currentLore.toCharArray();


                    if (this.subStringIndex >= letters.length) {
                        if (!loop)
                            this.framesCopy.remove(0);
                        this.colorState = 0;
                        this.subStringIndex = 0;
                        this.previous.clear();
                        this.currentLore = lore.get(loreIndex);
                        if (this.currentFrameIndex + 1 >= this.framesCopy.size()) {
                            this.currentFrameIndex = 0;
                        }
                    }

                    if (this.framesCopy.isEmpty()) {
                        inventory.removeLoreAnimator(IntelligentItemLoreAnimator.this);
                        return;
                    }

                    if (this.colorState >= currentFrames.length) {
                        this.colorState = 0;
                    }
                    char singleFrame = currentFrames[this.colorState];
                    IntelligentItemColor itemColor = frameColor.get(singleFrame);

                    String letter = String.valueOf(letters[this.subStringIndex]);
                    String rest = currentLoreFixed.substring(this.subStringIndex + 1);
                    boolean addColor = !letter.equals(" ");

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
                    this.currentLore = newString.toString();

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
                    updateLore(contents, this.currentLore, loreIndex);
                }
            }, this.delay, this.period);
            this.task.add(task);
        }
    }

    private void animateWordByWord() {
        for (Map.Entry<Integer, String> entry : this.loreData.entrySet()) {
            BukkitTask task;
            task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                final HashMap<Integer, String> framesCopy = loreData;

                int colorState = 0;
                int subStringIndex = 0;
                int currentFrameIndex = 0;
                String currentLore = "";

                @Override
                public void run() {
                    int loreIndex = entry.getKey();
                    String frame = framesCopy.get(loreIndex);
                    char[] currentFrames = frame.toCharArray();
                    String savedLore = ChatColor.stripColor(lore.get(loreIndex));
                    char[] letters = !this.currentLore.isEmpty() ? ChatColor.stripColor(lore.get(loreIndex)).toCharArray() : savedLore.toCharArray();

                    if (this.subStringIndex >= letters.length) {
                        if (!loop)
                            this.framesCopy.remove(0);
                        this.colorState = 0;
                        this.subStringIndex = 0;
                        this.currentLore = "";
                        if (this.currentFrameIndex + 1 >= this.framesCopy.size()) {
                            this.currentFrameIndex = 0;
                        }
                    }

                    if (this.framesCopy.isEmpty()) {
                        inventory.removeLoreAnimator(IntelligentItemLoreAnimator.this);
                        return;
                    }

                    if (this.colorState >= currentFrames.length) {
                        this.colorState = 0;
                        if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                            this.currentFrameIndex++;
                            currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                        }
                    }
                    String letter = String.valueOf(letters[this.subStringIndex]);
                    boolean addColor = !letter.equals(" ");

                    char singleFrame = currentFrames[this.colorState];
                    IntelligentItemColor itemColor = frameColor.get(singleFrame);

                    this.currentLore = this.currentLore + itemColor.getColor()
                            + (itemColor.isBold() ? "§l" : "")
                            + (itemColor.isUnderline() ? "§n" : "")
                            + (itemColor.isItalic() ? "§o" : "")
                            + (itemColor.isObfuscated() ? "§k" : "")
                            + (itemColor.isStrikeThrough() ? "§m" : "")
                            + letter;

                    this.subStringIndex++;

                    if (!addColor) return;

                    this.colorState++;
                    updateLore(contents, this.currentLore, loreIndex);
                }
            }, this.delay, this.period);
            this.task.add(task);
        }
    }

    private void updateLore(InventoryContents contents, String lore, @Nonnegative int index) {
        ItemStack itemStack = this.itemStack;

        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> currentLore = itemMeta.getLore() == null ? new ArrayList<>() : itemMeta.getLore();
        currentLore.set(index, lore);

        itemMeta.setLore(currentLore);
        itemStack.setItemMeta(itemMeta);

        contents.update(slot, itemStack);
        this.itemStack = itemStack;
    }

    protected List<BukkitTask> getTasks() {
        return this.task;
    }

    protected Object getIdentifier() {
        return this.identifier;
    }
}
