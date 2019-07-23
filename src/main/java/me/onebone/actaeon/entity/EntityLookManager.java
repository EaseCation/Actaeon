package me.onebone.actaeon.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.math.Vector3;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author CreeperFace
 */
@RequiredArgsConstructor
public class EntityLookManager {

    @Getter
    private final MovingEntity entity;

    @Getter
    private double stepYaw;
    @Getter
    private double stepPitch;

    @Getter
    private Vector3 target;

    @Getter
    private boolean looking;

    public void setLookPosition(Entity entity, double stepYaw, double stepPitch) {
        Vector3 pos = entity.clone();

        if (entity instanceof EntityLiving) {
            pos.y += entity.getEyeHeight();
        } else {
            pos.y = (entity.getBoundingBox().getMinY() + entity.getBoundingBox().getMaxY()) / 2;
        }

        setLookPosition(pos, stepYaw, stepPitch);
    }

    public void setLookPosition(Vector3 pos, double stepYaw, double stepPitch) {
        this.target = pos.clone();
        this.stepYaw = stepYaw;
        this.stepPitch = stepPitch;
        this.looking = true;
    }

    public void onUpdate() {
        this.entity.pitch = 0;

        if (this.isLooking()) {
            this.looking = false;
//            Vector3 from = Utils.vectorFromRotation(this.entity.yaw, this.entity.pitch);
//
//            Vector3 vec = this.target.subtract(this.entity);
            Vector3 diff = this.target.subtract(this.entity.add(0, this.entity.getEyeHeight()));

            double deltaYaw = (Math.atan2(diff.z, diff.x) * (180 / Math.PI)) - 90 - this.entity.yaw;
            double deltaPitch = (-(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)) * (180 / Math.PI))) - this.entity.pitch;

//            double deltaYaw = Utils.getYawBetween(from, vec);
//            double deltaPitch = Utils.getPitchBetween(from, vec);

            deltaYaw = correctValue(deltaYaw, this.stepYaw);
            deltaPitch = correctValue(deltaPitch, this.stepPitch);

            this.entity.yaw += deltaYaw;
            this.entity.pitch += deltaPitch;

//            TracePrinter.print(this.entity.level, this.entity, this.target, 5);
        }
//        else {
//            this.entity.yaw = correctValue(0 - this.entity.yaw, 10);
//        }
    }

    private double correctValue(double value, double step) {
        if (value > step) {
            value = step;
        } else if (value < -step) {
            value = -step;
        }

        return value;
    }
}
