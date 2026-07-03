package me.onebone.actaeon.entity.monster.evoker;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityFullNames;
import cn.nukkit.entity.EntityID;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

/**
 * net.easecation.ecgrave.entity
 * ===============
 * author: boybook
 * EaseCation Network Project
 * codefuncore
 * ===============
 */
public class EntityEvocationFang extends Entity {

    public static final int NETWORK_ID = EntityID.EVOCATION_FANG;

    private final Entity owner;
    private final float damage;
    private float knockBack = Float.NaN;

    private int limitedLifeTicks;

    public EntityEvocationFang(FullChunk chunk, CompoundTag nbt, Entity owner) {
        super(chunk, nbt);
        this.owner = owner;
        if (owner != null) {
            this.setDataProperty(new LongEntityData(Entity.DATA_OWNER_EID, owner.getId()), false);
        }
        limitedLifeTicks = 22;
        this.setDataProperty(new IntEntityData(Entity.DATA_LIMITED_LIFE, limitedLifeTicks), false);
        if (nbt.contains("Damage")) {
            this.damage = nbt.getFloat("Damage");
        } else {
            this.damage = 6f;
        }
        this.fireProof = true;
    }

    public EntityEvocationFang setKnockBack(float knockBack) {
        this.knockBack = knockBack;
        return this;
    }

    @Override
    public float getWidth() {
        return 1;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    public float getEyeHeight() {
        return 0.4f;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public void spawnTo(Player player) {
        if (this.hasSpawned.containsKey(player.getLoaderId())) return;
        AddEntityPacket pk = new AddEntityPacket();
        pk.type = this.getNetworkId();
        pk.entityUniqueId = this.getId();
        pk.entityRuntimeId = this.getId();
        pk.x = (float) this.x;
        pk.y = (float) this.y;
        pk.z = (float) this.z;
        pk.metadata = this.dataProperties;
        Pair<Int2IntMap, Int2FloatMap> propertyValues = getProperties().getValues();
        if (propertyValues != null) {
            pk.intProperties = propertyValues.left();
            pk.floatProperties = propertyValues.right();
        }
        player.dataPacket(pk);
        super.spawnTo(player);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return false; //无敌
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean hasUpdate = super.onUpdate(currentTick);

        if ((limitedLifeTicks-- & 1) == 1) {
            setDataProperty(new IntEntityData(Entity.DATA_LIMITED_LIFE, limitedLifeTicks));

            if (limitedLifeTicks == 20) {
                Entity[] entities = this.getLevel().getNearbyEntities(this.getBoundingBox().grow(0.2, 0, 0.2), this);
                for (Entity entity : entities) {
                    if (!(entity instanceof EntityLiving)) {
                        continue;
                    }

                    if (entity == this.owner) {
                        continue;
                    }

                    if (!entity.isAlive()) {
                        continue;
                    }

                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        if (player.isCreativeLike()) {
                            continue;
                        }
                    }

                    EntityDamageByChildEntityEvent event = new EntityDamageByChildEntityEvent(this.owner, this, entity, DamageCause.MAGIC, this.damage);
                    if (!Float.isNaN(knockBack)) {
                        event.setKnockbackBase(knockBack);
                    }
                    entity.attack(event);
                }

                broadcastEntityEvent(EntityEventPacket.ARM_SWING);

                if (owner != null) {
                    level.addLevelSoundEvent(getEyePosition(), LevelSoundEventPacket.SOUND_THROW, EntityFullNames.EVOCATION_FANG);
                }
                level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_FANG, EntityFullNames.EVOCATION_FANG);
            } else if (limitedLifeTicks <= 0) {
                limitedLifeTicks = -1;
                setDataProperty(new IntEntityData(Entity.DATA_LIMITED_LIFE, limitedLifeTicks));
                close();
                return false;
            }
        }

        return hasUpdate;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }
}
