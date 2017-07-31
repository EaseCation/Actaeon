package me.onebone.actaeon.hook;

import cn.nukkit.math.Vector3;
import me.onebone.actaeon.entity.MovingEntity;
import me.onebone.actaeon.utils.Utils;

/**
 * Created by CreeperFace on 19.7.2017.
 */
public class WanderHook extends MovingEntityHook {

    private boolean update = false;
    private int chance;

    public WanderHook(MovingEntity entity) {
        this(entity, 120);
    }

    public WanderHook(MovingEntity entity, int chance) {
        super(entity);
        this.chance = chance;
    }

    @Override
    public void onUpdate(int tick) {
        if (!this.entity.routeLeading || this.entity.getHate() != null || this.entity.level.rand.nextInt(this.chance) != 0) {
            return;
        }

        Vector3 randomPos = Utils.randomVector(this.entity, 10, 0, 10).add(0, 7);

        this.entity.setTarget(randomPos, "randomPos");
    }
}
