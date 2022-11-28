package me.onebone.actaeon.hook;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import me.onebone.actaeon.entity.IMovingEntity;
import me.onebone.actaeon.task.MovingEntityTask;

import java.util.Arrays;

/**
 * ECPlayerBotAttackHook
 * ===============
 * author: boybook
 * EaseCation Network Project
 * codefuncore
 * ===============
 */
public class ShootArrowHook extends MovingEntityHook {

    public interface ShootArrowTaskSupplier {
        MovingEntityTask get(IMovingEntity entity, Entity target, int ticks, double pitch);
    }

    private long lastAttack = 0;
    private final double minDistance;
    private final double maxDistance;
    private long coolDown;
    private final int ticks;
    private final double pitch;
    private final ShootArrowTaskSupplier shootArrowTaskSupplier;

    public ShootArrowHook(IMovingEntity entity, ShootArrowTaskSupplier shootArrowTaskSupplier) {
        this(entity, shootArrowTaskSupplier, 10, 30, 2500);
    }

    public ShootArrowHook(IMovingEntity entity, ShootArrowTaskSupplier shootArrowTaskSupplier, double minDistance, double maxDistance, long coolDown) {
        this(entity, shootArrowTaskSupplier, minDistance, maxDistance, coolDown, 40);
    }

    public ShootArrowHook(IMovingEntity entity, ShootArrowTaskSupplier shootArrowTaskSupplier, double minDistance, double maxDistance, long coolDown, int ticks) {
        this(entity, shootArrowTaskSupplier, minDistance, maxDistance, coolDown, ticks, 5);
    }

    public ShootArrowHook(IMovingEntity entity, ShootArrowTaskSupplier shootArrowTaskSupplier, double minDistance, double maxDistance, long coolDown, int ticks, double pitch) {
        super(entity);
        this.shootArrowTaskSupplier = shootArrowTaskSupplier;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.coolDown = coolDown;
        this.ticks = ticks;
        this.pitch = pitch;
    }

    public long getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(long coolDown) {
        this.coolDown = coolDown;
    }

    public long getLastAttack() {
        return lastAttack;
    }

    @Override
    public void onUpdate(int tick) {
        if (this.entity.getHate() != null) {
            Entity hate = this.entity.getHate();
            double distance = this.entity.distance(hate);
            if (distance >= this.minDistance && distance <= this.maxDistance) {
                if (System.currentTimeMillis() - this.lastAttack > this.coolDown) {
                    try {
                        if (this.ticks == 0 || (this.entity.getEntity() instanceof EntityLiving && Arrays.stream(((EntityLiving) this.entity.getEntity()).getLineOfSight((int) this.entity.distance(hate.getPosition().add(0, hate.getEyeHeight(), 0)))).noneMatch(Block::isSolid))) {  //可以直接看到目标
                            if (this.entity.getTask() == null) {
                                this.entity.updateBotTask(this.shootArrowTaskSupplier.get(this.entity, hate, this.ticks, this.pitch));
                            }
                            this.lastAttack = System.currentTimeMillis();
                        }
                    } catch (Exception e) {
                        this.lastAttack = System.currentTimeMillis();
                    }
                }
            }
        }
    }
}