package me.onebone.actaeon.route;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

import java.util.Iterator;

public class WalkableIterator implements Iterator<Block> {
    private final Level level;
    private final int maxDistance;
    private final double width;

    private boolean end = false;

    private Block currentBlockObject;

    private double currentDistance;
    private Vector3 startPosition;
    private Vector3 currentPosition = null;
    private Vector3 direction = null;

    private AdvancedRouteFinder advancedRouteFinder;

    public WalkableIterator(AdvancedRouteFinder advancedRouteFinder, Level level, Vector3 start, Vector3 direction, double width, int maxDistance) {
        this.advancedRouteFinder = advancedRouteFinder;
        this.level = level;
        this.width = width;
        this.maxDistance = maxDistance == 0 ? 120 : maxDistance;
        this.currentDistance = 0;
        this.currentPosition = start.clone();
        this.startPosition = start.clone();
        this.direction = direction.normalize();
    }

    public Vector3 getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public Block next() {
        return this.currentBlockObject;
    }

    @Override
    public boolean hasNext() {
        this.scan();
        return this.currentBlockObject != null;
    }

    private void scan() {
        if (this.maxDistance != 0 && this.currentDistance > this.maxDistance) {
            this.end = true;
            return;
        }
        if (this.end) return;

        do {
            //if (this.currentDistance > 2 && new Random().nextInt(100) < 35) this.level.addParticle(new FlameParticle(this.currentPosition));
            Block block = this.level.getBlock(this.currentPosition);
            Vector3 next = this.currentPosition.add(this.direction);
            double walkable = this.advancedRouteFinder.isWalkableAt(block);
            if (walkable != 0 || (block.getBoundingBox() != null && block.getBoundingBox().calculateIntercept(this.currentPosition, next) != null)) {
                this.currentBlockObject = block;
                this.end = true;
            }
            this.currentPosition = next;
            this.currentDistance = this.currentPosition.distance(this.startPosition);
            if (this.maxDistance > 0 && this.currentDistance > this.maxDistance) this.end = true;
        } while (!this.end);
    }

}
