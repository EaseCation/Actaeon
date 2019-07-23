package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import me.onebone.actaeon.entity.EntityAgeable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

abstract public class Animal extends EntityAgeable {

    protected Player inLovePlayer;
    protected int inLoveTicks;

    public Animal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.inLoveTicks = this.namedTag.getInt("InLove");
    }

    @Override
    public boolean isBaby() {
        return this.getDataFlag(DATA_FLAGS, Entity.DATA_FLAG_BABY);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.inLoveTicks > 0) {
            --this.inLoveTicks;

            if (this.inLoveTicks % 10 == 0) {
                Random rand = ThreadLocalRandom.current();

                this.level.addParticle(new HeartParticle(new Vector3(this.x + (rand.nextFloat() * this.getWidth() * 2) - getWidth(), this.y + 0.5 + (rand.nextFloat() * getHeight()), this.z + (rand.nextFloat() * getWidth() * 2) - this.getWidth())));
            }
        }

        return hasUpdate;
    }

    @Override
    public void spawnTo(Player player) {
        AddEntityPacket pk = new AddEntityPacket();
        pk.type = this.getNetworkId();
        pk.entityUniqueId = this.getId();
        pk.entityRuntimeId = this.getId();
        pk.x = (float) this.x;
        pk.y = (float) this.y;
        pk.z = (float) this.z;
        pk.speedX = (float) this.motionX;
        pk.speedY = (float) this.motionY;
        pk.speedZ = (float) this.motionZ;
        pk.metadata = this.dataProperties;
        pk.yaw = (float) this.yaw;
        pk.pitch = (float) this.pitch;
        player.dataPacket(pk);

        UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
        pk0.entityId = this.getId();
        pk0.entries = new Attribute[]{
                Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
        };
        player.dataPacket(pk0);

        super.spawnTo(player);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (super.attack(source)) {
            this.inLoveTicks = 0;
            return true;
        }

        return false;
    }

    public boolean isBreedingItem(Item item) {
        return item.getId() == Item.WHEAT;
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (this.isBreedingItem(item) && this.getGrowingAge() == 0 && this.inLoveTicks <= 0) {
            this.setInLove(player);
            return true;
        }

        if (this.isBaby() && this.isBreedingItem(item)) {
            this.ageUp((int) ((float) (-this.getGrowingAge() / 20) * 0.1F), true);
            return true;
        }

        return super.onInteract(player, item);
    }

    public void setInLove(Player player) {
        this.inLoveTicks = 600;
        this.inLovePlayer = player;
    }

    public boolean isInLove() {
        return this.inLoveTicks > 0;
    }

    public void resetInLove() {
        this.inLoveTicks = 0;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putInt("InLove", this.inLoveTicks);
    }

    public void eatGrass() {

    }
}
