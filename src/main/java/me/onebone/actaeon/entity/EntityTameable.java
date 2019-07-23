package me.onebone.actaeon.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityOwnable;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.entity.animal.Animal;
import me.onebone.actaeon.hook.SitHook;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author CreeperFace
 */
public abstract class EntityTameable extends Animal implements EntityOwnable {

    protected String owner;
    protected Player ownerInstance;

    protected SitHook sitHook;

    public EntityTameable(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.owner = namedTag.getString("Owner");
        this.setSitting(namedTag.getBoolean("Sitting"));

        if (owner != null && !owner.isEmpty()) {
            this.setTamed(true);
        }

        if (sitHook != null) {

        }
    }

    public boolean isTamed() {
        return getDataFlag(DATA_FLAGS, DATA_FLAG_TAMED);
    }

    public void setTamed(boolean tamed) {
        setTamed(tamed, null);
    }

    public void setTamed(boolean tamed, Entity owner) {
        setDataFlag(DATA_FLAGS, DATA_FLAG_TAMED, tamed);

        if (owner != null) {
            setDataProperty(new LongEntityData(DATA_OWNER_EID, owner.getId()));
        }
    }

    public boolean isSitting() {
        return getDataFlag(DATA_FLAGS, DATA_FLAG_SITTING);
    }

    public void setSitting(boolean sitting) {
        setDataFlag(DATA_FLAGS, DATA_FLAG_SITTING, sitting);
    }

    @Override
    public Player getOwner() {
        if (ownerInstance == null || ownerInstance.closed) {
            ownerInstance = this.getServer().getPlayerExact(this.owner);
        }

        return ownerInstance;
    }

    @Override
    public String getOwnerName() {
        return owner;
    }

    @Override
    public void setOwnerName(String owner) {
        this.owner = owner;
    }

    public void setOwner(Player p) {
        this.ownerInstance = p;
        this.owner = p.getName();

        setDataProperty(new LongEntityData(DATA_OWNER_EID, p.getId()));
    }

    public boolean isOwner(Player player) {
        return owner.equalsIgnoreCase(player.getName());
    }

    public boolean hasOwner() {
        return owner != null;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Sitting", isSitting());
        this.namedTag.putString("Owner", owner);
    }

    protected void addTameParticle(boolean success) {
        Particle particle = success ? new HeartParticle(this) : new SmokeParticle(this);

        Random rand = ThreadLocalRandom.current();
        for (int i = 0; i < 7; ++i) {

            this.level.addParticle((Particle) particle.setComponents(this.x + (rand.nextFloat() * this.getWidth() * 2) - this.getWidth(), this.y + 0.5 + (rand.nextFloat() * this.getHeight()), this.z + (rand.nextFloat() * this.getWidth() * 2) - this.getWidth()));
        }
    }
}
