package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import com.google.common.collect.Sets;
import me.onebone.actaeon.hook.*;

import java.util.Random;
import java.util.Set;

public class Cow extends Animal implements EntityAgeable {

    public static final int NETWORK_ID = 11;
    private static final Set<Item> FOLLOW_ITEMS = Sets.newHashSet(Item.get(Item.WHEAT));

    public Cow(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.addHook(1, new AnimalMateHook(this));
        this.addHook(2, new FollowItemAI(this, 10, FOLLOW_ITEMS));
        this.addHook(3, new FollowParentHook(this));
        this.addHook(4, new WanderHook(this));
        this.addHook(5, new WatchClosestHook(this, Player.class, 6));
        this.addHook(6, new LookIdleHook(this));

        setMaxHealth(10);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.65f;
        }
        return 1.3f;
    }

    @Override
    public float getEyeHeight() {
        if (isBaby()) {
            return 0.65f;
        }
        return 1.2f;
    }

    @Override
    public Item[] getDrops() {
        if (!isBaby()) {
            Random random = new Random();
            Item leather = Item.get(Item.LEATHER, 0, random.nextInt(2));
            Item meat = Item.get(Item.RAW_BEEF, 0, random.nextInt(3) + 1);
            EntityDamageEvent cause = this.getLastDamageCause();
            if (cause.getCause() == EntityDamageEvent.DamageCause.FIRE) {
                meat = Item.get(Item.STEAK, 0, random.nextInt(3) + 1);
            }
            this.getLevel().dropExpOrb(this, random.nextInt(3) + 1);
            return new Item[]{leather, meat};
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        return super.entityBaseTick(tickDiff);
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (item.getId() == Item.BUCKET && item.getDamage() == 0) {
            PlayerInventory inv = player.getInventory();

            item.count--;

            if (item.count <= 0) {
                inv.setItem(inv.getHeldItemIndex(), Item.get(Item.BUCKET, 1));
            } else {
                Item milk = Item.get(Item.BUCKET, 1);

                if (inv.canAddItem(milk)) {
                    inv.setItem(inv.getHeldItemIndex(), item);
                    inv.addItem();
                }
            }

            return true;
        }

        return super.onInteract(player, item);
    }
}
