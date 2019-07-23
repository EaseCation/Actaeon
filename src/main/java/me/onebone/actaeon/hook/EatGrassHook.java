package me.onebone.actaeon.hook;

import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.event.entity.EntityBlockChangeEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.network.protocol.EntityEventPacket;
import me.onebone.actaeon.entity.animal.Animal;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by CreeperFace on 15.7.2017.
 */
public class EatGrassHook extends MovingEntityHook {

    private int timer = 40;
    private Animal animal;

    public EatGrassHook(Animal entity) {
        super(entity);
        this.animal = entity;
        setCompatibility(FLAG_MOVEMENT | FLAG_ROTATION | FLAG_BUSY);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.animal.onGround || ThreadLocalRandom.current().nextInt(this.animal.isBaby() ? 50 : 100) != 0) {
            return false;
        } else {
            Block block = this.entity.getLevelBlock();

            return block instanceof BlockTallGrass || block.down() instanceof BlockGrass;
        }
    }

    @Override
    public void startExecuting() {
        this.timer = 40;

        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.getEntity().getId();
        pk.event = EntityEventPacket.EAT_GRASS_ANIMATION;
        Server.broadcastPacket(this.getEntity().getViewers().values(), pk);

        this.entity.getRoute().forceStop();
        this.entity.routeLeading = false;
    }

    @Override
    public void reset() {
        timer = 0;
        this.entity.routeLeading = true;
    }

    @Override
    public boolean canContinue() {
        return timer > 0;
    }

    @Override
    public void onUpdate(int tick) {
        this.timer = Math.max(0, this.timer - 1);

        if (this.timer == 4) {
            Block block = this.entity.floor().getLevelBlock();

            if (block instanceof BlockTallGrass) {
                if (this.entity.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
                    EntityBlockChangeEvent e = new EntityBlockChangeEvent(this.entity, block, new BlockAir());
                    this.entity.getServer().getPluginManager().callEvent(e);

                    if (!e.isCancelled()) {
                        this.entity.level.useBreakOn(block);
                    }
                }

                this.animal.eatGrass();
            } else {
                block = block.down();

                if (block.getId() == Block.GRASS) {
                    if (this.entity.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {

                        EntityBlockChangeEvent e = new EntityBlockChangeEvent(this.entity, block, new BlockDirt());
                        this.entity.getServer().getPluginManager().callEvent(e);

                        if (!e.isCancelled()) {
                            this.entity.level.addParticle(new DestroyBlockParticle(block, block));
                            this.entity.level.setBlock(block, e.getTo(), true, false);
                        }
                    }

                    this.animal.eatGrass();
                }
            }
        }
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }
}
