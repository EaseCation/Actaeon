package me.onebone.actaeon.entity.animal;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityID;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlockID;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.target.AreaPlayerHoldTargetFinder;

import java.util.concurrent.ThreadLocalRandom;

public class Sheep extends Animal{
	public static final int NETWORK_ID = EntityID.SHEEP;

	public Sheep(FullChunk chunk, CompoundTag nbt){
		super(chunk, nbt);
		this.setTargetFinder(new AreaPlayerHoldTargetFinder(this, 500, Item.get(Item.WHEAT), 100));
	}

	@Override
	public float getWidth(){
		return 0.9f;
	}

	@Override
	public float getHeight(){
		return 1.3f;
	}

	@Override
	public float getEyeHeight(){
		if (isBaby()){
			return 0.95f * 0.9f; // No have information
		}
		return 0.95f * getHeight();
	}

	@Override
	protected double getStepHeight() {
		return 1;
	}

	@Override
	public Vector3f getMountedOffset(Entity entity) {
		if (getDataFlag(DATA_FLAG_SHEARED)) {
			return new Vector3f(0, 0.9f + entity.getRidingOffset(), 0);
		}
		return new Vector3f(0, 0.975f + entity.getRidingOffset(), 0);
	}

	@Override
	public String getName(){
		return this.getNameTag();
	}

	@Override
	public Item[] getDrops(){
		return new Item[]{
				Item.get(isOnFire() ? Item.COOKED_MUTTON : Item.MUTTON, 0, ThreadLocalRandom.current().nextInt(1, 3)),
				Item.get(ItemBlockID.WHITE_WOOL),
		};
	}

	@Override
	public int getNetworkId(){
		return NETWORK_ID;
	}

	@Override
	protected void initEntity(){
		super.initEntity();
		this.setMaxHealth(8);
	}
}
