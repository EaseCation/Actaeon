package me.onebone.actaeon.hook;

import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import me.onebone.actaeon.entity.MovingEntity;
import me.onebone.actaeon.target.EntityTarget;
import me.onebone.actaeon.util.Utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by CreeperFace on 19.7.2017.
 */
public class WanderHook extends MovingEntityHook {

    private static final double MIN_DISTANCE = Math.pow(0.5, 2);

    private static final boolean USE_PATH_FINDER_ON_COLLISION = true;

    private Vector3 target;

//    private double distance;
//    private Vector2 checkPos;
//    private int startTick;

    private double radius;
    private int chance;

    private boolean collision = false;
    private int startCollision = Integer.MAX_VALUE;

    private int time;

    public WanderHook(MovingEntity entity) {
        this(entity, 60);
    }

    public WanderHook(MovingEntity entity, int chance) {
        this(entity, chance, 10);
    }

    public WanderHook(MovingEntity entity, int chance, double radius) {
        super(entity);
        this.radius = radius;
        this.chance = chance;
        setCompatibility(FLAG_MOVEMENT);
    }

    @Override
    public boolean shouldExecute() {
        if (ThreadLocalRandom.current().nextInt(chance) != 0)
            return false;

        target = findVector();

//        this.checkPos = new Vector2(target.x, target.z);
        return true;
    }

    @Override
    public void onUpdate(int tick) {
        this.time--;
//        if(tick - startTick >= 20 && tick % 20 == 0) {
//            Vector2 currentPos = new Vector2(this.entity.x, this.entity.z);
//
//            distance = currentPos.distanceSquared(checkPos);
//            checkPos = currentPos;
//        }
//        Vector3 v = this.target.subtract(this.entity).normalize();
        if (this.entity.isCollidedHorizontally) {
//            double yaw = Utils.getYawBetween(this.entity, this.target);

//            MainLogger.getLogger().info("origin: "+this.target);

            if (collision) {
                if (tick - startCollision < 20) {
                    return;
                }
            } else {
                startCollision = tick;
                collision = true;
            }

            if (new Vector2(this.entity.lastX, this.entity.lastZ).distance(this.entity.x, this.entity.z) > 0.0125) {
                return;
            }

            Vector3 direction = this.target.subtract(this.entity);
            Vector3 moveVector = direction.normalize();
//            Vector3 diff = Utils.divideVectors(direction, moveVector);

//            if(this.entity.isCollidedX && this.entity.isCollidedZ) {
//                moveVector.x = -moveVector.x;
//                moveVector.z = -moveVector.z;
//            } else if(this.entity.isCollidedX) {
//                moveVector.x = -moveVector.x;
//            } else if(this.entity.isCollidedZ) {
//                moveVector.z = -moveVector.z;
//            }
//            boolean overAngle = false;
//            boolean negative = false;

            if (this.entity.isCollidedX && Math.abs(this.entity.x - this.entity.lastX) < 0.0125) {
//                if (moveVector.x > 0.5) {
//                    overAngle = true;
//                } else if (moveVector.x < -0.5) {
//                    overAngle = true;
//                    negative = true;
//                }

                moveVector.x = -moveVector.x;
//                MainLogger.getLogger().info("collided X");
            } else if (this.entity.isCollidedZ && Math.abs(this.entity.z - this.entity.lastZ) < 0.0125) {
//                if (moveVector.z > 0.5) {
//                    overAngle = true;
//                } else if (moveVector.z < -0.5) {
//                    overAngle = true;
//                    negative = true;
//                }

                moveVector.z = -moveVector.z;
//                MainLogger.getLogger().info("collided Z");
            }

            double newYawDiff = Math.toDegrees(Utils.getAngleBetween(direction, moveVector));

//            if (Double.isNaN(newYawDiff)) {
//                MainLogger.getLogger().info("dir: " + direction + "  vec: " + moveVector);
//            }

//            if(overAngle) {
//                newYawDiff = 180 - newYawDiff;
//            }

            newYawDiff *= 0.1;

//            if(negative) {
//                newYawDiff = -newYawDiff;
//            }

//            if(newYawDiff > 10) {
//                newYawDiff = 10;
//            } else if(newYawDiff < -10) {
//                newYawDiff = -10;
//            }

//            MainLogger.getLogger().info("diff: "+newYawDiff);

            if (newYawDiff != 0) {
                this.target = this.entity.add(Utils.vectorFromYaw(Utils.getYawFrom(direction) + newYawDiff).multiply(6));
                this.target.y = this.entity.y;

//                TracePrinter.print(this.entity.level, this.entity, this.target, 6);
                this.entity.setDirectTarget(EntityTarget.builder().target(this.target).identifier("wander").build());
//            MainLogger.getLogger().info("yaw diff: "+newYawDiff+"  distance to target = "+this.entity.distance(this.target));
//            MainLogger.getLogger().info("after: "+this.target);
            }
        } else {
            collision = false;
        }

//        boolean collide = this.entity.isCollidedHorizontally;
    }

    @Override
    public boolean canContinue() {
        return this.time > 0 && this.entity.getRoute().hasRoute() && !this.entity.getRoute().hasArrived()/* && distance >= MIN_DISTANCE*/;
    }

    @Override
    public void startExecuting() {
        this.entity.setDirectTarget(EntityTarget.builder().target(this.target).identifier("wander").build());
        this.time = (ThreadLocalRandom.current().nextInt(8) + 7) * 20;
//        startTick = this.entity.getServer().getTick();
//        distance = Double.MAX_VALUE;
    }

    private Vector3 findVector() {
        Random random = ThreadLocalRandom.current();
        Vector3 base = this.entity.getRealTarget() != null ? this.entity.getTarget() : this.getEntity().getPosition();

        double r = random.nextDouble() * 360;
        double x = this.radius * Math.cos(Math.toRadians(r));
        double z = this.radius * Math.sin(Math.toRadians(r));
        double y = base.getY();

        return new Vector3(base.getX() + x, y, base.getZ() + z);
    }

    @Override
    public void reset() {
        this.entity.resetMovementPath();
    }
}
