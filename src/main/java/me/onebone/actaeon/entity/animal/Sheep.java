package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.DyeColor;
import com.google.common.collect.Sets;
import me.onebone.actaeon.entity.EntityAgeable;
import me.onebone.actaeon.event.PlayerSheepShearEvent;
import me.onebone.actaeon.hook.*;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Sheep extends Animal {

    public static final int NETWORK_ID = 13;
    private static final Set<Item> FOLLOW_ITEMS = Sets.newHashSet(Item.get(Item.WHEAT));

    public Sheep(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setMaxHealth(8);

        if (!namedTag.contains("Color")) {
            namedTag.putByte("Color", getRandomSheepColor().getWoolData());
        }

        this.setDataProperty(new ByteEntityData(DATA_COLOUR, namedTag.getByte("Color")));

        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED, namedTag.getBoolean("Sheared"));

        this.addHook(1, new AnimalMateHook(this));
        this.addHook(2, new FollowItemAI(this, 10, FOLLOW_ITEMS));
        this.addHook(3, new FollowParentHook(this));
        this.addHook(4, new EatGrassHook(this));
        this.addHook(5, new WanderHook(this));
        this.addHook(6, new WatchClosestHook(this, Player.class, 6));
        this.addHook(7, new LookIdleHook(this));
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        namedTag.putByte("Color", getColor().getWoolData());
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.9f; // No have information
        }
        return 1.3f;
    }

    @Override
    public float getEyeHeight() {
        if (isBaby()) {
            return 0.95f * 0.9f; // No have information
        }
        return 0.95f * getHeight();
    }

    @Override
    public String getName() {
        return this.getNameTag();
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(Item.WOOL, getColor().getWoolData(), 1)};
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        return super.entityBaseTick(tickDiff);
    }

    public void setColor(DyeColor color) {
        setColor(color.getWoolData());
    }

    public void setColor(int color) {
        this.setDataProperty(new ByteEntityData(DATA_COLOUR, color));
        this.namedTag.putByte("Color", color);
    }

    public DyeColor getColor() {
        return DyeColor.getByWoolData(getDataPropertyByte(DATA_COLOR));
    }

    public void setSheared(boolean sheared) {
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED, sheared);
        this.namedTag.putBoolean("Sheared", sheared);
    }

    public boolean isSheared() {
        return getDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED);
    }

    public static DyeColor getRandomSheepColor() {
        Random random = new Random();

        int i = random.nextInt(100);
        return i < 5 ? DyeColor.BLACK : (i < 10 ? DyeColor.GRAY : (i < 15 ? DyeColor.LIGHT_GRAY : (i < 18 ? DyeColor.BROWN : (random.nextInt(500) == 0 ? DyeColor.PINK : DyeColor.WHITE))));
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (item.getId() == Item.SHEARS && !isSheared()) {
            PlayerSheepShearEvent ev = new PlayerSheepShearEvent(player, this);
            this.server.getPluginManager().callEvent(ev);

            if (ev.isCancelled()) {
                return false;
            }

            this.setSheared(true);

            Random rand = ThreadLocalRandom.current();
            int count = 1 + rand.nextInt(3);
            while (count-- > 0) {
                this.level.dropItem(this.add(0, 1), Item.get(Item.WOOL, getColor().getWoolData()), new Vector3(
                        (rand.nextFloat() - rand.nextFloat()) * 0.1,
                        rand.nextFloat() * 0.05,
                        (rand.nextFloat() - rand.nextFloat()) * 0.1
                ));
            }

            return true;
        } else if (item.getId() == Item.DYE) {
            DyeColor color = DyeColor.getByDyeData(item.getDamage());

            if (color != null) {
                this.setColor(color);
                return true;
            }
        }

        return super.onInteract(player, item);
    }

    public void eatGrass() {
        this.setSheared(false);

        if (this.isBaby()) {
            addGrowth(60);
        }
    }

    @Override
    public EntityAgeable createBaby(EntityAgeable mother) {
        Entity baby = super.createBaby(mother);

        if (!(baby instanceof Sheep) || !(mother instanceof Sheep)) {
            return null;
        }

        ((Sheep) baby).setColor(getColorFromParents(this, (Sheep) mother));

        return (Sheep) baby;
    }

    private DyeColor getColorFromParents(Sheep father, Sheep mother) {
//        List<DyeColor> colors = Arrays.asList(father.getColor(), mother.getColor());
//
//        for (Recipe recipe : getServer().getCraftingManager().recipes.values()) {
//            if (!(recipe instanceof ShapelessRecipe)) {
//                continue;
//            }
//
//            ShapelessRecipe rec = (ShapelessRecipe) recipe;
//            List<Item> ingredients = rec.getIngredientList();
//
//            if (ingredients.size() != 2) {
//                continue;
//            }
//
//
//        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            return father.getColor();
        } else {
            return mother.getColor();
        }
    }
}
