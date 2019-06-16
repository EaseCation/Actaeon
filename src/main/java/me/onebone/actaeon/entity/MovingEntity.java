package me.onebone.actaeon.entity;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import lombok.Getter;
import me.onebone.actaeon.hook.HookManager;
import me.onebone.actaeon.hook.MovingEntityHook;
import me.onebone.actaeon.route.AdvancedRouteFinder;
import me.onebone.actaeon.route.Node;
import me.onebone.actaeon.route.RouteFinder;
import me.onebone.actaeon.route.SimpleRouteFinder;
import me.onebone.actaeon.target.EntityTarget;
import me.onebone.actaeon.target.TargetFinder;
import me.onebone.actaeon.task.MovingEntityTask;
import me.onebone.actaeon.util.ActaeonTimings;
import me.onebone.actaeon.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

abstract public class MovingEntity extends EntityCreature {

    private boolean isKnockback = false;

    private Map<Class<? extends RouteFinder>, RouteFinder> routeFinders = new HashMap<>();

    @Getter
    private RouteFinder route;

    private TargetFinder targetFinder = null;

    private EntityTarget target = null;

    private Entity hate = null;

    public boolean routeLeading = true;

    private HookManager hookManager = new HookManager();

    private MovingEntityTask task = null;

    public boolean isCollidedX = false;
    public boolean isCollidedZ = false;

    public double headYaw;
    public double lastHeadYaw;

    @Getter
    private EntityLookManager lookManager = new EntityLookManager(this);

    public MovingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        registerRouteFinder(new AdvancedRouteFinder(this));
        registerRouteFinder(new SimpleRouteFinder(this));

        this.setRouteFinder(AdvancedRouteFinder.class);
        //this.route = new SimpleRouteFinder(this);
        this.setImmobile(false);
//        this.setDataFlag(DATA_FLAGS, 46, this.onGround); //collision
    }

    public void setBaby(boolean isBaby) {
        this.setDataFlag(DATA_FLAGS, Entity.DATA_FLAG_BABY, isBaby);
    }

    public void addHook(int priority, MovingEntityHook hook) {
        this.hookManager.addHook(priority, hook);
    }

    public void removeHook(MovingEntityHook hook) {
        this.hookManager.removeHook(hook);
    }

    @Override
    protected float getGravity() {
        return 0.08f;
    }

    public Entity getHate() {
        return hate;
    }

    public void setHate(Entity hate) {
        this.hate = hate;
    }

    public void jump() {
        if (this.onGround) {
            this.motionY = Math.sqrt(2 * getJumpHeight() * getGravity());
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        super.onUpdate(currentTick);
        return true;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.closed) {
            return false;
        }

        ActaeonTimings.HOOK_TICK.startTiming();
        this.hookManager.onUpdate(getServer().getTick());
        ActaeonTimings.HOOK_TICK.stopTiming();

        if (this.task != null) this.task.onUpdate(Server.getInstance().getTick());

        this.lookManager.onUpdate();

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        ActaeonTimings.MOVE_TICK.startTiming();

        try {
//            if (this.isKnockback) {                   // knockback 이 true 인 경우는 맞은 직후
//
//            } else if (this.routeLeading && this.onGround) {
//                this.motionX = this.motionZ = 0;
//            }

            if (this.targetFinder != null) this.targetFinder.onUpdate();

            if (this.route.isSearching() && System.currentTimeMillis() - this.route.stopRouteFindUntil > 1000) {
                this.route.forceStop();
            }

            double swim = 0;

            for (Block block : getCollisionBlocks()) {
                if (block instanceof BlockLiquid) {
                    float f = ((BlockLiquid) block).getFluidHeightPercent();
                    double minY = block.getFloorY();
                    double maxY = minY + 1 - f;

                    swim = Math.max(swim, this.boundingBox.getMaxY() >= maxY ? maxY - this.boundingBox.getMinY() : this.boundingBox.getMaxY() - minY);
                    break;
                }
            }

            if (this.routeLeading && (this.onGround || swim > 0) && this.hasSetTarget() && !this.route.isSearching() && System.currentTimeMillis() >= this.route.stopRouteFindUntil && (this.route.getDestination() == null || this.route.getDestination().distance(this.getTarget()) > maxTargetDistance())) { // 대상이 이동함
                //Server.getInstance().getScheduler().scheduleAsyncTask(new RouteFinderSearchAsyncTask(this.route, this.level, this, this.getTarget(), this.boundingBox));
                this.route.setPositions(this.level, this.clone(), getTarget().clone(), this.boundingBox.clone());

                this.route.search();

			/*if(this.route.isSearching()) this.route.research();
            else this.route.search();*/

                hasUpdate = true;
            }

            if (!this.isImmobile()) {
                double moveMotionX = 0;
                double moveMotionZ = 0;

                if (this.routeLeading && !this.isKnockback && !this.route.isSearching() && this.route.isSuccess() && this.route.hasRoute()) { // entity has route to go
                    hasUpdate = true;
                    //MainLogger.getLogger().info("MOVE");

                    Node node = this.route.get();
                    if (node != null) {
                        //level.addParticle(new cn.nukkit.level.particle.RedstoneParticle(node.getVector3(), 2));
                        Vector3 vec = node.getVector3();
                        double diffX = Math.pow(vec.x - this.x, 2);
                        double diffZ = Math.pow(vec.z - this.z, 2);

                        if (diffX + diffZ == 0) {
                            if (!this.route.next()) {
                                this.route.arrived();
                                //Server.getInstance().getLogger().warning(vec.toString());
                            }
                        } else {
//                            Vector3 motion = vec.subtract(this).normalize().multiply(this.getMovementSpeed());
//
//                            this.motionX = motion.x;
//                            this.motionZ = motion.z;
                            int negX = vec.x - this.x < 0 ? -1 : 1;
                            int negZ = vec.z - this.z < 0 ? -1 : 1;

                            moveMotionX = Math.min(Math.abs(vec.x - this.x), diffX / (diffX + diffZ) * this.getMovementSpeed()) * negX;
                            moveMotionZ = Math.min(Math.abs(vec.z - this.z), diffZ / (diffX + diffZ) * this.getMovementSpeed()) * negZ;


//                            if(this.getServer().getTick() % 5 == 0) {
//                                TracePrinter.print(this.level, this, vec, 7);
//                            }

                            if (!this.target.isWatchTarget()) {
                                this.setRotation(Utils.getYawBetween(this, vec), 0);
                            }

                        /*if(getRealTarget() instanceof Entity)
                            lookAt(getTarget());*/
                        }
                    }
                }

                for (Entity entity : this.getLevel().getCollidingEntities(this.boundingBox)) {
                    if (this.canCollide() && this.canCollideWith(entity)) {
                    /*Vector3 motion = this.subtract(entity);
                    this.motionX += motion.x / 2;
                    this.motionZ += motion.z / 2;*/
                        applyEntityCollision(entity);
                    }
                }

                if (swim != 0) {
                    if (ThreadLocalRandom.current().nextFloat() < 0.8) {
                        this.motionY = Math.min(0.15, this.motionY + (0.2 * swim));
                    }

                    this.motionX *= 0.3;
                    this.motionZ *= 0.3;
                }

//                double beforeY = this.y;
//                double beforeMotY = this.motionY;

                this.move(this.motionX + moveMotionX, this.motionY, this.motionZ + moveMotionZ);

//                if(beforeY - this.y > 0.3) {
////                    MainLogger.getLogger().info("motion: "+motionY+"motionY diff: "+Math.abs(beforeMotY - this.motionY)+"  Y diff: "+Math.abs(this.y - beforeY)+"   onGround: "+this.onGround);
//                }
//                MainLogger.getLogger().info("moving to: "+new Vector3(this.motionX + moveMotionX, this.motionY, this.motionZ + moveMotionZ));
//                MainLogger.getLogger().info("motX: "+moveMotionX+"  motZ: "+moveMotionZ);

                if ((moveMotionX != 0 || moveMotionZ != 0 || this.motionX != 0 || this.motionZ != 0) && this.isCollidedHorizontally) {
                    AxisAlignedBB bb = this.boundingBox.clone().offset(this.motionX + moveMotionX, this.motionY, this.motionZ + moveMotionZ);
                    Block[] blocks = this.level.getCollisionBlocks(bb);

                    boolean jump = false;
                    boolean step = true;

                    for (Block b : blocks) {
                        AxisAlignedBB blockBB = b.getBoundingBox();
                        if (blockBB == null || b.canPassThrough())
                            continue;

                        double diffY = blockBB.getMaxY() - this.boundingBox.getMinY();
                        if (diffY < getJumpHeight()) {
                            jump = true;
                        }

                        if (step)
                            step = diffY <= getStepHeight();
                    }

                    if (jump && !step) {
                        this.jump();
                    }
                }

//                this.checkGround();
                if (!this.onGround && swim == 0) {
//                    double beforeMot = this.motionY;
                    this.motionY -= this.getGravity();
//                    Server.getInstance().getLogger().warning("motionY: " + this.motionY+" before: "+beforeMot+"  diff: "+Math.abs(beforeMot - this.motionY));
                    hasUpdate = true;
                } else {
                    this.isKnockback = false;
                }

                double friction = 1 - this.getDrag();

                if (this.onGround && (Math.abs(this.motionX) > 0.00001 || Math.abs(this.motionZ) > 0.00001))
                    friction *= this.getLevel().getBlock(this.temporalVector.setComponents((int) Math.floor(this.x), (int) Math.floor(this.y - 1), (int) Math.floor(this.z) - 1)).getFrictionFactor();

                this.motionX *= friction;
                this.motionY *= 1 - getDrag();
                this.motionZ *= friction;
            }
        } finally {
            ActaeonTimings.MOVE_TICK.stopTiming();
        }


        return hasUpdate;
    }

    public double getRange() {
        return 100.0;
    }

    public void setTarget(EntityTarget target) {
        this.setTarget(target, false);
    }

    public void setTarget(EntityTarget target, boolean forceSearch) {
        if (target.getIdentifier() == null) return;
        if (this.target != null && Objects.equals(this.target.getTarget(), target.getTarget()))
            return;

        if (forceSearch || !this.hasSetTarget() || target.getIdentifier().equals(this.target.getIdentifier())) {
            this.target = target;
        }

        if (this.hasSetTarget() && (forceSearch || !this.route.hasRoute())) {
            this.route.forceStop();

            if (!(this.route instanceof AdvancedRouteFinder)) {
                this.setRouteFinder(AdvancedRouteFinder.class);
            }

            this.route.setPositions(this.level, this.clone(), getTarget().clone(), this.boundingBox.clone());

            this.route.search();
			/*if(this.route.isSearching()) this.route.research();
			else this.route.search();*/
        }
    }

    public void setDirectTarget(EntityTarget target) {
        this.target = target;

        this.setRouteFinder(SimpleRouteFinder.class);

        this.route.setPositions(this.level, this.clone(), getTarget(), this.boundingBox.clone());
        this.route.search();
    }

    public Vector3 getRealTarget() {
        if (this.target == null) {
            return null;
        }

        return this.target.getTarget();
    }

    public Vector3 getTarget() {
        if (this.target == null) {
            return null;
        }

        Vector3 v = this.target.getTarget();

        if (v == null) {
            return null;
        }

        return new Vector3(v.x, v.y, v.z);
    }

    /**
     * Returns whether the entity has following target
     * Entity will try to move to position where target exists
     */
    public boolean hasFollowingTarget() {
        return this.route.getDestination() != null && this.target != null && this.distanceSquared(this.target.getTarget()) < this.getRange();
    }

    /**
     * Returns whether the entity has set its target
     * The entity may not follow the target if there is following target and set target is different
     * If following distance of target is too far to follow or cannot reach, set target will be the next following target
     */
    public boolean hasSetTarget() {
        return this.target != null && this.target.getTarget() != null && this.distanceSquared(this.target.getTarget()) < this.getRange();
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        this.isCollidedVertically = movY != dy;

        this.isCollidedX = movX != dx;
        this.isCollidedZ = movZ != dz;

        this.isCollidedHorizontally = this.isCollidedX || this.isCollidedZ;

        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);

//        boolean wasOnGround = this.onGround;

        this.onGround = (movY != dy && movY < 0);

        // onGround 는 onUpdate 에서 확인
    }

//    private void checkGround() {
//        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(0, -1, 0) : this.boundingBox.addCoord(0, -1, 0), false);
//
//        double maxY = 0;
//        for (AxisAlignedBB bb : list) {
//            if (bb.maxY > maxY) {
//                maxY = bb.maxY;
//            }
//        }
//
//        this.onGround = (maxY == this.boundingBox.minY);
//    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
        pk0.entityId = this.getId();
        pk0.entries = new Attribute[]{
                Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
        };
        this.getLevel().addChunkPacket(this.chunk.getX(), this.chunk.getZ(), pk0);
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        super.setMaxHealth(maxHealth);
        if (this.getHealth() > maxHealth) this.health = maxHealth;
        UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
        pk0.entityId = this.getId();
        pk0.entries = new Attribute[]{
                Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
        };
        this.getLevel().addChunkPacket(this.chunk.getX(), this.chunk.getZ(), pk0);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_NO_AI);
    }

    @Override
    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        this.isKnockback = true;

        super.knockBack(attacker, damage, x, z, base / 2);
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }

    public TargetFinder getTargetFinder() {
        return targetFinder;
    }

    public void setTargetFinder(TargetFinder targetFinder) {
        this.targetFinder = targetFinder;
    }

    public void updateBotTask(MovingEntityTask task) {
        if (this.task != null) this.task.forceStop();
        this.task = task;
        if (task != null) this.task.onUpdate(Server.getInstance().getTick());
    }

    public MovingEntityTask getTask() {
        return task;
    }

    @Deprecated
    public void lookAt(Vector3 pos) {
        Vector3 diff = pos.subtract(this.add(0, getEyeHeight()));

        double length = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        double newPitch = Math.atan2(diff.z, diff.x) * (180 / Math.PI) - 90;
        double newYaw = -(Math.atan2(diff.y, length) * (180 / Math.PI));

        this.setRotation(newYaw, newPitch);
    }

    public double getJumpHeight() {
        return 1.25;
    }

    /*public boolean isBaby() {
        return false;
    }*/

    public double maxTargetDistance() {
        if (this.target != null) {
            return this.target.getMaxDistance();
        }

        return 2;
    }

    public int getAge() {
        return age;
    }

    public void setRouteFinder(Class<? extends RouteFinder> finder) {
        if (this.route != null) {
            this.route.forceStop();
        }

        this.route = this.routeFinders.get(finder);
    }

    public void resetMovementPath() {
        this.getRoute().forceStop();
        this.setTarget(EntityTarget.builder().identifier(this.getName()).build(), true);
    }

    public void registerRouteFinder(RouteFinder finder) {
        this.routeFinders.put(finder.getClass(), finder);
    }

    @Override
    public float getMovementSpeed() {
        return super.getMovementSpeed() * this.target.getSpeed();
    }

    public int getHorizontalLookSpeed() {
        return 10;
    }

    public int getVerticalLookSpeed() {
        return 40;
    }

    @Override
    public void updateMovement() {
        double diffPosition = (this.x - this.lastX) * (this.x - this.lastX) + (this.y - this.lastY) * (this.y - this.lastY) + (this.z - this.lastZ) * (this.z - this.lastZ);
        double diffRotation = (this.headYaw - lastHeadYaw) * (this.headYaw - lastHeadYaw) + (this.yaw - this.lastYaw) * (this.yaw - this.lastYaw) + (this.pitch - this.lastPitch) * (this.pitch - this.lastPitch);

        double diffMotion = (this.motionX - this.lastMotionX) * (this.motionX - this.lastMotionX) + (this.motionY - this.lastMotionY) * (this.motionY - this.lastMotionY) + (this.motionZ - this.lastMotionZ) * (this.motionZ - this.lastMotionZ);

        if (diffPosition > 0.0001 || diffRotation > 1.0) { //0.2 ** 2, 1.5 ** 2
            this.lastX = this.x;
            this.lastY = this.y;
            this.lastZ = this.z;

            this.lastYaw = this.yaw;
            this.lastPitch = this.pitch;

            this.addMovement(this.x, this.y + this.getBaseOffset(), this.z, this.yaw, this.pitch, this.headYaw);
        }

        if (diffMotion > 0.0025 || (diffMotion > 0.0001 && this.getMotion().lengthSquared() <= 0.0001)) { //0.05 ** 2
            this.lastMotionX = this.motionX;
            this.lastMotionY = this.motionY;
            this.lastMotionZ = this.motionZ;

            this.addMotion(this.motionX, this.motionY, this.motionZ);
        }
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
    }
}

