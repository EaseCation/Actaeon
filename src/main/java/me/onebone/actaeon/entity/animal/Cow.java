package me.onebone.actaeon.entity.animal;

import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityID;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.target.AreaPlayerHoldTargetFinder;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Cow extends Animal implements EntityAgeable{
	public static final int NETWORK_ID = EntityID.COW;

	public Cow(FullChunk chunk, CompoundTag nbt){
		super(chunk, nbt);
		this.setTargetFinder(new AreaPlayerHoldTargetFinder(this, 500, Item.get(Item.WHEAT), 100));
	}

	@Override
	public int getNetworkId(){
		return NETWORK_ID;
	}

	@Override
	public float getWidth(){
		return 0.9f;
	}

	@Override
	public float getHeight(){
		if (isBaby()) {
			return 0.65f;
		}
		return 1.3f;
	}

	@Override
	public float getEyeHeight(){
		if (isBaby()){
			return 0.65f;
		}
		return 1.2f;
	}

	@Override
	public boolean isBaby(){
		return false;
	}

	@Override
	public Item[] getDrops(){
		Random random = ThreadLocalRandom.current();
		Item leather = Item.get(Item.LEATHER, 0, random.nextInt(3));
		Item meat = Item.get(Item.BEEF, 0, random.nextInt(3) + 1);
		EntityDamageEvent cause = this.getLastDamageCause();
		if (cause.getCause() == EntityDamageEvent.DamageCause.FIRE) {
			meat = Item.get(Item.COOKED_BEEF, 0, random.nextInt(3) + 1);
		}
		return new Item[]{leather, meat};
	}

	@Override
	public boolean entityBaseTick(int tickDiff){

		return super.entityBaseTick(tickDiff);
	}

	@Override
	protected void initEntity(){
		super.initEntity();
		setMaxHealth(10);
	}
}
