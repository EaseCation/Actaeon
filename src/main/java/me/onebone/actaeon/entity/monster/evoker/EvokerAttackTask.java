package me.onebone.actaeon.entity.monster.evoker;

import cn.nukkit.block.Block;
import cn.nukkit.block.SupportType;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityFullNames;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.level.Level;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Mth;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.utils.BlockColor;
import me.onebone.actaeon.entity.IMovingEntity;
import me.onebone.actaeon.task.MovingEntityTask;

/**
 * AttackTask
 * ===============
 * author: boybook
 * ===============
 */
public class EvokerAttackTask extends MovingEntityTask {
    private final float prevSpeed;
    private final double x;
    private final double z;
    private final double minY;
    private final double maxY;

    private final Vector3 target;
    private final float damage;
    private float knockBack = Float.NaN;

    private boolean lineMode;
    private int index = 1;

    public EvokerAttackTask(IMovingEntity entity, Entity target, float damage) {
        super(entity);
        prevSpeed = entity.getEntity().getMovementSpeed();
        x = entity.getX();
        z = entity.getZ();
        minY = Math.min(target.getY(), entity.getY());
        maxY = Math.max(target.getY(), entity.getY()) + 1;
        this.target = target.getPosition();
        this.damage = damage;
        onStart(target);
    }

    protected void onStart(Entity target) {
        entity.getEntity().setMovementSpeed(0);
        entity.getEntity().setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, target.getId()));
        this.getEntity().getEntity().setDataFlag(Entity.DATA_FLAG_EVOKER_SPELL, true);
        this.getEntity().getEntity().setDataProperty(new IntEntityData(Entity.DATA_SPELL_CASTING_COLOR, BlockColor.getARGB(0x66, 0x4d, 0x59)));
        //如果目标离唤魔者的距离少于3格，唤魔者会以自身为中心召唤两圈尖牙。
        this.lineMode = this.getEntity().getEntity().distanceSquared(this.target) > 3 * 3;
        entity.getEntity().getLevel().addLevelSoundEvent(entity.getEntity().getEyePosition(), LevelSoundEventPacket.SOUND_MOB_WARNING, EntityFullNames.EVOCATION_ILLAGER);
        entity.getEntity().getLevel().addLevelSoundEvent(entity.getEntity(), LevelSoundEventPacket.SOUND_CAST_SPELL, EntityFullNames.EVOCATION_ILLAGER);
        if (!lineMode) {
            entity.getEntity().getLevel().addLevelSoundEvent(entity.getEntity(), LevelSoundEventPacket.SOUND_PREPARE_ATTACK, EntityFullNames.EVOCATION_ILLAGER);
        }
    }

    public EvokerAttackTask setKnockBack(float knockBack) {
        this.knockBack = knockBack;
        return this;
    }

    public EvokerAttackTask setLineMode(boolean lineMode) {
        this.lineMode = lineMode;
        return this;
    }

    public Vector3 getTarget() {
        return target;
    }

    @Override
    public void onUpdate(int tick) {
        if (this.lineMode) {
            if (index > 20 && index <= 20 + 16) {
                // 16个尖牙，延伸20格，所以每一格的距离是20/16=1.25
                Vector2 v2 = new Vector2(this.target.x - this.entity.getX(), this.target.z - this.entity.getZ());
                v2 = v2.normalize().multiply(1.25);

                int num = index - 20;
                double x = this.x + v2.multiply(num).getX();
                double z = this.z + v2.multiply(num).getY();
                summonFang(x, z, minY, maxY);
            }
        } else {
            if (index == 20 + 1) {
                entity.getEntity().getLevel().addLevelSoundEvent(entity.getEntity(), LevelSoundEventPacket.SOUND_PREPARE_ATTACK, EntityFullNames.EVOCATION_ILLAGER);
                // 内圈5个尖牙，距离1，环绕成一圈
                for (int i = 0; i < 5; i++) {
                    double angle = 2 * Math.PI / 5 * i;
                    double x = this.x + Mth.cos(angle);
                    double z = this.z + Mth.sin(angle);
                    summonFang(x, z, minY, maxY);
                }
            } else if (index == 23 + 1) {
                // 外圈为8个尖牙，距离2，环绕成一圈
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI / 8 * i;
                    double x = this.x + 2 * Mth.cos(angle);
                    double z = this.z + 2 * Mth.sin(angle);
                    summonFang(x, z, minY, maxY);
                }
            }
        }
        if (index++ > 40) {
            this.entity.updateBotTask(null);
        }
    }

    private boolean summonFang(double x, double z, double minY, double maxY) {
        Level level = this.getEntity().getLevel();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int minBlockY = Mth.floor(minY) - 1;
        int maxBlockY = Mth.floor(maxY);
        double y = maxBlockY;

        boolean success = false;
        for (int blockY = maxBlockY; blockY >= minBlockY; blockY--) {
            Block block = level.getBlock(blockX, blockY, blockZ, false);
            if (block.isSolid()) {
                continue;
            }
            Block below = level.getBlock(blockX, blockY - 1, blockZ, false);
            if (!SupportType.hasFullSupport(below, BlockFace.UP)) {
                continue;
            }
            y = blockY;
            if (!block.isAir()) {
                AxisAlignedBB[] aabbs = block.getCollisionShape();
                if (aabbs != null) {
                    for (AxisAlignedBB aabb : aabbs) {
                        y = Math.max(y, aabb.getMaxY());
                    }
                }
            }
            success = true;
            break;
        }
        if (!success) {
            return false;
        }

        CompoundTag nbt = Entity.getDefaultNBT(blockX, y, blockZ)
                .putFloat("Damage", damage);
        EntityEvocationFang fang = new EntityEvocationFang(level.getChunk(blockX >> 4, blockZ >> 4), nbt, getEntity().getEntity());
        fang.setKnockBack(knockBack);
        fang.spawnToAll();
        return true;
    }

    @Override
    public void forceStop() {
        entity.getEntity().setMovementSpeed(prevSpeed == 0 ? 0.1f : prevSpeed);
        this.getEntity().getEntity().setDataFlag(Entity.DATA_FLAG_EVOKER_SPELL, false);
        entity.getEntity().setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, 0));
    }

}
