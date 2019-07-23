package me.onebone.actaeon.hook;

import cn.nukkit.entity.Entity;
import me.onebone.actaeon.entity.MovingEntity;
import me.onebone.actaeon.util.Utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author CreeperFace
 */
public class WatchClosestHook extends MovingEntityHook {

    private Class<? extends Entity> watching;
    private double maxDistance;
    private float chance;

    private Entity closeEntity;

    private int time;

    public WatchClosestHook(MovingEntity entity, Class<? extends Entity> watching, double maxDistance) {
        this(entity, watching, maxDistance, 0.2f);
    }

    public WatchClosestHook(MovingEntity entity, Class<? extends Entity> watching, double maxDistance, float chance) {
        super(entity);

        this.watching = watching;
        this.maxDistance = maxDistance;
        this.chance = chance;

        this.setCompatibility(FLAG_ROTATION);
    }

    @Override
    public boolean shouldExecute() {
        if (ThreadLocalRandom.current().nextFloat() >= this.chance) {
            return false;
        }

        this.closeEntity = Utils.getClosestEntityTo(this.entity, maxDistance, this.watching);

        return this.closeEntity != null;
    }

    @Override
    public void onUpdate(int tick) {
        this.time--;

        this.entity.getLookManager().setLookPosition(this.closeEntity, this.entity.getHorizontalLookSpeed(), this.entity.getVerticalLookSpeed());
    }

    @Override
    public boolean canContinue() {
        return this.closeEntity.isAlive() && this.entity.distanceSquared(this.closeEntity) <= this.maxDistance * this.maxDistance && time > 0;
    }

    @Override
    public void startExecuting() {
        this.time = ThreadLocalRandom.current().nextInt(40) + 40;
    }

    @Override
    public void reset() {
        this.closeEntity = null;
    }
}
