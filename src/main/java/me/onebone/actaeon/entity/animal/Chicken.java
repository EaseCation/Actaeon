package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.entity.Fallable;
import me.onebone.actaeon.target.AreaPlayerHoldTargetFinder;

public class Chicken extends Animal implements EntityAgeable, Fallable{
	public static final int NETWORK_ID = 10;

	public Chicken(FullChunk chunk, CompoundTag nbt) {
		super(chunk, nbt);
		this.setTargetFinder(new AreaPlayerHoldTargetFinder(this, 500, Item.get(Item.WHEAT), 100));
	}

	@Override
	public float getWidth(){
		return 0.4f;
	}

	@Override
	protected float getGravity() {
		return 0.05f;
	}

	@Override
	public float getHeight(){
		if (isBaby()){
			return 0.51f;
		}
		return 0.7f;
	}

	@Override
	public float getEyeHeight(){
		if (isBaby()){
			return 0.51f;
		}
		return 0.7f;
	}

	@Override
	public Item[] getDrops(){
		return new Item[]{Item.get(Item.RAW_CHICKEN), Item.get(Item.FEATHER)};
	}

	@Override
	public boolean entityBaseTick(int tickDiff){
		return super.entityBaseTick(tickDiff);
	}

	@Override
	public int getNetworkId(){
		return NETWORK_ID;
	}

	@Override
	protected void initEntity(){
		super.initEntity();
		setMaxHealth(4);
	}

	@Override
	public boolean isBaby(){
		return false;
	}
}
