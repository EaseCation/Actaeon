package me.onebone.actaeon.task;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.network.protocol.EntityEventPacket;
import me.onebone.actaeon.entity.MovingEntity;

/**
 * AttackTask
 * ===============
 * author: boybook
 * ===============
 */
public class AttackTask extends MovingEntityTask {

    private Entity target;
    private float damage;
    private double viewAngle;
    private boolean effectual;

    public AttackTask(MovingEntity entity, Entity target, float damage, double viewAngle, boolean effectual) {
        super(entity);
        this.target = target;
        this.damage = damage;
        this.viewAngle = viewAngle;
        this.effectual = effectual;
    }

    @Override
    public void onUpdate(int tick) {
        double angle = Math.atan2(this.target.z - this.entity.z, this.target.x - this.entity.x);
        double yaw = ((angle * 180) / Math.PI) - 90;
        double min = this.entity.yaw - this.viewAngle / 2;
        double max = this.entity.yaw + this.viewAngle / 2;
        boolean valid;
        if (min < 0) {
            valid = yaw > 360 + min || yaw < max;
        } else if (max > 360) {
            valid = yaw < max - 360 || yaw > min;
        } else {
            valid = yaw < max && yaw > min;
        }
        if (valid && this.effectual) {
            if (this.target.noDamageTicks <= 0) {
                this.target.attack(new EntityDamageByEntityEvent(this.getEntity(), this.target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, this.damage));
                this.target.noDamageTicks = 10;
            }
        }

        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.entity.getId();
        pk.event = 4;
        Server.broadcastPacket(this.getEntity().getViewers().values(), pk);
        this.entity.updateBotTask(null);
    }

    @Override
    public void forceStop() {
    }
}
