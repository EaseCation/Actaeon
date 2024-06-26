package me.onebone.actaeon.target;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import me.onebone.actaeon.entity.IMovingEntity;

public class AreaPlayerHoldTargetFinder extends TargetFinder {

	private final Item item;
    private final int radius;
    private boolean first = true;

	public AreaPlayerHoldTargetFinder(IMovingEntity entity, long interval, Item item, int radius) {
		super(entity, interval);
		this.item = item;
        this.radius = radius;
	}

	protected void find() {
        Player near = null;
        double nearest = this.radius * this.radius;

        for (Player player: this.getEntity().getLevel().getPlayers().values()) {
            if (this.getEntity().distanceSquared(player) < nearest){
                if (player.getInventory().getItemInHand().equals(this.item, false, false)){
                    near = player;
                    nearest = this.getEntity().distance(player);
                }
            }
        }

        if (near != null) {
            this.getEntity().setTarget(near, this.getEntity().getName());
            this.getEntity().setHate(near);
        } else {
            //this.getEntity().getRoute().forceStop();
            this.getEntity().setTarget(null, this.getEntity().getName(), first);
        }
        first = false;
	}
}
