package me.onebone.actaeon.hook;

import me.onebone.actaeon.entity.MovingEntity;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author CreeperFace
 */
public class LookIdleHook extends MovingEntityHook {

    private double x;
    private double z;

    private double time;

    public LookIdleHook(MovingEntity entity) {
        super(entity);

        this.setCompatibility(FLAG_MOVEMENT | FLAG_ROTATION);
    }

    @Override
    public void onUpdate(int tick) {
        this.time--;

        this.entity.getLookManager().setLookPosition(this.entity.add(x, this.entity.getEyeHeight(), z), this.entity.getHorizontalLookSpeed(), this.entity.getVerticalLookSpeed());
    }

    @Override
    public boolean shouldExecute() {
        return ThreadLocalRandom.current().nextFloat() < 0.02;
    }

    @Override
    public boolean canContinue() {
        return time >= 0;
    }

    @Override
    public void startExecuting() {
        Random random = ThreadLocalRandom.current();

        double rng = (Math.PI * 2) * random.nextDouble();
        this.x = Math.cos(rng);
        this.z = Math.sin(rng);
        this.time = 20 + random.nextInt(20);
    }
}
