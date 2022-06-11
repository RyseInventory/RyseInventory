package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.enums.TimeSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx(Rysefoxx # 6772) |
 * @since 4/12/2022
 */
public class IntelligentMaterialAnimator {

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
    private static Plugin plugin;

    public static Builder builder(Plugin plugin) {
        IntelligentMaterialAnimator.plugin = plugin;
        return new Builder();
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
         * @param intelligentItem
         * @return The Builder to perform further editing.
         */
        public Builder item(IntelligentItem intelligentItem) {
            this.intelligentItem = intelligentItem;
            return this;
        }


        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         */
        public Builder copy(IntelligentMaterialAnimator preset) {
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
         * Assigns a material to a frame.
         *
         * @param frame    The frame that should receive the material.
         * @param material The material you want the frame to have.
         * @return The Builder to perform further editing.
         */
        public Builder material(char frame, Material material) {
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
        public Builder materials(List<Character> frames, Material... materials) throws IllegalArgumentException {
            Preconditions.checkArgument(frames.size() == materials.length, "Frames must have the same length as materials.");

            for (int i = 0; i < frames.size(); i++) {
                material(frames.get(i), materials[i]);
            }
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
        public Builder materials(Character[] frames, Material... materials) {
            Preconditions.checkArgument(frames.length == materials.length, "Frames must have the same length as materials.");

            for (int i = 0; i < frames.length; i++) {
                material(frames[i], materials[i]);
            }
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
        public Builder materials(Character[] frames, List<Material> materials) {
            Preconditions.checkArgument(frames.length == materials.size(), "Frames must have the same length as materials.");

            for (int i = 0; i < frames.length; i++) {
                material(frames[i], materials.get(i));
            }
            return this;
        }

        /**
         * Adds another frame.
         *
         * @param frame
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link IntelligentMaterialAnimator.Builder#material(char, Material)}
         */
        public Builder frame(String frame) throws IllegalArgumentException {
            this.frames.add(frame);
            return this;
        }

        /**
         * Adds several frames.
         *
         * @param frames
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link IntelligentMaterialAnimator.Builder#material(char, Material)}
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
         * @throws IllegalArgumentException If no material has been assigned to the frame yet. e.g {@link IntelligentMaterialAnimator.Builder#material(char, Material)}
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
         * This creates the animation class but does not start it yet! {@link IntelligentMaterialAnimator#animate()}
         *
         * @return The animation class
         * @throws IllegalArgumentException if no slot was specified, if frameMaterial is empty, if frames is empty or if no material has been assigned to a frame.
         * @throws NullPointerException     if item is null.
         */
        public IntelligentMaterialAnimator build(InventoryContents contents) throws IllegalArgumentException, NullPointerException {
            if (this.preset != null) {
                this.intelligentItem = this.preset.intelligentItem;
                this.frames = this.preset.frames;
                this.frameMaterial = this.preset.frameMaterial;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.slot = this.preset.slot;
                this.loop = this.preset.loop;
            }

            if (this.slot == -1) {
                throw new IllegalArgumentException("Please specify a slot where the item is located.");
            }
            if (this.frameMaterial.isEmpty()) {
                throw new IllegalArgumentException("Please specify a material for each frame.");
            }
            if (this.intelligentItem == null) {
                throw new NullPointerException("Please specify an item to animate.");
            }
            if (this.frames.isEmpty()) {
                throw new IllegalArgumentException("No frames have been defined yet!");
            }

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

    /**
     * This starts the animation for the item.
     */
    public void animate() {
        this.inventory.addMaterialAnimator(this);
        animateItem();
    }

    private void animateItem() {
        int finalLength = getFrameLength();

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final List<String> framesCopy = frames;

            int materialState = 0;
            int subStringIndex = 0;
            int currentFrameIndex = 0;
            Material currentMaterial;
            final ItemStack itemStack = new ItemStack(intelligentItem.getItemStack());

            @Override
            public void run() {
                char[] currentFrames = framesCopy.get(this.currentFrameIndex).toCharArray();

                if (this.subStringIndex >= finalLength) {
                    if (!loop)
                        this.framesCopy.remove(0);
                    this.materialState = 0;
                    this.subStringIndex = 0;

                    if (!this.framesCopy.isEmpty()) {
                        this.currentMaterial = frameMaterial.get(currentFrames[this.materialState]);
                    }
                    if (this.currentFrameIndex + 1 >= this.framesCopy.size())
                        this.currentFrameIndex = 0;
                }

                if (this.framesCopy.isEmpty()) {
                    inventory.removeMaterialAnimator(IntelligentMaterialAnimator.this);
                    return;
                }

                if (this.materialState >= currentFrames.length) {
                    this.materialState = 0;
                    if (this.framesCopy.size() > 1 && (this.currentFrameIndex + 1 != this.framesCopy.size())) {
                        this.currentFrameIndex++;
                        currentFrames = this.framesCopy.get(this.currentFrameIndex).toCharArray();
                    }
                }

                char singleFrame = currentFrames[this.materialState];

                this.currentMaterial = frameMaterial.get(singleFrame);
                this.materialState++;
                this.subStringIndex++;

                this.itemStack.setType(this.currentMaterial);
                contents.update(slot, this.itemStack);
            }
        }, this.delay, this.period);
    }

    protected BukkitTask getTask() {
        return this.task;
    }

    protected Object getIdentifier() {
        return this.identifier;
    }

    private int getFrameLength() {
        int length = 0;
        for (String frame : frames) {
            length += frame.length();
        }
        return length;
    }
}
