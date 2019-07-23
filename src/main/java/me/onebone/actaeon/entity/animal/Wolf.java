package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.DyeColor;
import me.onebone.actaeon.entity.EntityTameable;
import me.onebone.actaeon.hook.*;

import java.util.concurrent.ThreadLocalRandom;

public class Wolf extends EntityTameable {

    public static final int NETWORK_ID = 14;
//    private static final Set<Item> FOLLOW_ITEMS = Sets.newHashSet(Item.get(Item.BONE));

    protected DyeColor collarColor;

    public Wolf(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        setMaxHealth(isTamed() ? 20 : 8);
        setMovementSpeed(0.23f);

        this.sitHook = new SitHook(this);

        this.addHook(1, this.sitHook);
        this.addHook(2, new AnimalMateHook(this));
//        this.addHook(3, new FollowItemAI(this, 10, FOLLOW_ITEMS));
        this.addHook(4, new WanderHook(this));
        this.addHook(5, new WatchClosestHook(this, Player.class, 6));
        this.addHook(6, new LookIdleHook(this));

        this.setAngry(namedTag.getBoolean("Angry"));
        this.setCollarColor(namedTag.getByte("CollarColor"));
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Angry", isAngry());
        this.namedTag.putByte("CollarColor", getCollarColor().getDyeData());
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 0.85f;
    }

    public boolean isAngry() {
        return getDataFlag(DATA_FLAGS, DATA_FLAG_ANGRY);
    }

    public void setAngry(boolean angry) {
        setDataFlag(DATA_FLAGS, DATA_FLAG_ANGRY, angry);
    }

    public void setCollarColor(DyeColor color) {
        setCollarColor(color.getWoolData());
    }

    public void setCollarColor(int color) {
        this.collarColor = DyeColor.getByWoolData(color);

//        MainLogger.getLogger().info("set collar color: "+color);
        this.setDataProperty(new ByteEntityData(Entity.DATA_COLOUR, color));
    }

    public DyeColor getCollarColor() {
        return collarColor;
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (!this.isTamed()) {
            if (item.getId() == Item.BONE && !isAngry()) {
                item.count--;

                if (ThreadLocalRandom.current().nextInt(3) == 0) {
                    setTamed(true, player);
                    setMaxHealth(20);
                    setHealth(20);
                    setSitting(true);
                    addTameParticle(true);
                    setCollarColor(DyeColor.RED);
                } else {
                    addTameParticle(false);
                }

                this.setOwner(player);
            }
        } else if (isOwner(player)) {
            if (item.getId() == Item.DYE) {
                DyeColor color = DyeColor.getByDyeData(item.getDamage());

                if (color != null && getCollarColor() != color) {
                    this.setCollarColor(color);
                    return true;
                }
            }

            if (isBreedingItem(item)) {
                //TODO:
            } else {
                boolean sitting = !isSitting();

                setSitting(sitting);
                this.sitHook.setSitting(sitting);
            }
        }

        return false;
    }

    @Override
    public boolean attack(EntityDamageEvent source) { //TODO: angry
        if (source instanceof EntityDamageByEntityEvent) {
            Entity attacker = ((EntityDamageByEntityEvent) source).getDamager();
            /*if (!this.getOwnerName().equalsIgnoreCase(attacker.getName()) || !this.hasOwner()) {
                this.setHate(attacker);
                this.addHook("attack", new AttackHook(this, 1, 4, 1000, 10, 180));
            }*/
            if (!isTamed()) {
                setAngry(true);
            } else {
                //TODO:
            }
        }
        return false;
    }

    @Override
    public boolean isBreedingItem(Item item) {
        return item.getId() == Item.BONE;
    }
}
