package me.onebone.actaeon.entity.animal;

import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.hook.EatGrassHook;
import me.onebone.actaeon.hook.WanderHook;
import me.onebone.actaeon.target.AreaPlayerHoldTargetFinder;

import java.util.Random;

public class Sheep extends Animal {
    public static final int NETWORK_ID = 13;

    public Sheep(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setTargetFinder(new AreaPlayerHoldTargetFinder(this, 500, Item.get(Item.WHEAT), 10));
        this.addHook("eatGrass", new EatGrassHook(this));
        this.addHook("wander", new WanderHook(this));
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
        return new Item[]{Item.get(Item.WOOL), Item.get(Item.RAW_MUTTON, 0, new Random().nextInt(2) + 1)};
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        return super.entityBaseTick(tickDiff);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(8);
    }
}
