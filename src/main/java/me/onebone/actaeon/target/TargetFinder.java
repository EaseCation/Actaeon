package me.onebone.actaeon.target;

import me.onebone.actaeon.entity.MovingEntity;

public abstract class TargetFinder {
    protected MovingEntity entity = null;
    protected long nextFind = 0;
    protected long interval;

    public TargetFinder(MovingEntity entity, long interval) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        this.entity = entity;
        this.interval = interval;
    }

    public MovingEntity getEntity() {
        return this.entity;
    }

    public void onUpdate() {
        if (System.currentTimeMillis() >= nextFind) {
            this.find();
            this.nextFind = System.currentTimeMillis() + this.interval;
        }
    }

    protected abstract void find();

}
