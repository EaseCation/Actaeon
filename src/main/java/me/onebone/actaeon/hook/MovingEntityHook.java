package me.onebone.actaeon.hook;

import cn.nukkit.entity.Entity;
import me.onebone.actaeon.entity.MovingEntity;

/**
 * ECPlayerBotHook
 * ===============
 * author: boybook
 * EaseCation Network Project
 * codefuncore
 * ===============
 */
public abstract class MovingEntityHook {

    protected final MovingEntity entity;

    public boolean executing = false;

    public MovingEntityHook(MovingEntity entity) {
        this.entity = entity;
    }

    public MovingEntity getEntity() {
        return entity;
    }

    public void onUpdate(int tick) {
    }

    public void onDamage(Entity damager) {
    }

    public boolean shouldExecute() {
        return true;
    }
}
