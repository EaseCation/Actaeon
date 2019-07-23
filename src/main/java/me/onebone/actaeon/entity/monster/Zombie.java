package me.onebone.actaeon.entity.monster;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.entity.Fallable;
import me.onebone.actaeon.hook.LookIdleHook;
import me.onebone.actaeon.hook.WanderHook;
import me.onebone.actaeon.hook.WatchClosestHook;
import me.onebone.actaeon.target.AreaHaterTargetFinder;

public class Zombie extends Monster implements EntityAgeable, Fallable {

    public static final int NETWORK_ID = 32;

    public Zombie(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.setTargetFinder(new AreaHaterTargetFinder(this, Player.class, 500, 16));

        this.addHook(4, new WanderHook(this));
        this.addHook(5, new WatchClosestHook(this, Player.class, 6));
        this.addHook(6, new LookIdleHook(this));

        setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getLength() {
        return 0.6f;
    }

    @Override
    protected float getGravity() {
        return 0.05f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.8f;
        }
        return 1.8f;
    }

    @Override
    public float getEyeHeight() {
        if (isBaby()) {
            return 0.51f;
        }
        return 0.7f;
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(Item.ROTTEN_FLESH)};
    }

    public double getAttackDistance() {
        return 1;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public boolean isBaby() {
        return false;
    }
}
