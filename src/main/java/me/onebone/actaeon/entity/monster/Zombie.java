package me.onebone.actaeon.entity.monster;

import cn.nukkit.Difficulty;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityID;
import cn.nukkit.entity.mob.EntityZombie;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.entity.Fallable;
import me.onebone.actaeon.hook.AttackHook;
import me.onebone.actaeon.target.AreaHaterTargetFinder;

import java.util.concurrent.ThreadLocalRandom;

public class Zombie extends Monster implements EntityAgeable, Fallable {
	public static final int NETWORK_ID = EntityID.ZOMBIE;

	public Zombie(FullChunk chunk, CompoundTag nbt) {
		super(chunk, nbt);
		this.setTargetFinder(new AreaHaterTargetFinder(this, 500, 20000));
		registerHooks();
	}

	protected void registerHooks() {
		this.addHook("attack", new AttackHook(this, this.getAttackDistance(), this::getDamage, 1000, 10, 180));
	}

	@Override
	public float getWidth(){
		return 0.6f;
	}

	@Override
	protected float getGravity() {
		return 0.05f;
	}

	@Override
	public float getHeight(){
		return 1.9f;
	}

	@Override
	public float getEyeHeight(){
		return 1.62f;
	}

	@Override
	protected double getStepHeight() {
		return 1;
	}

	@Override
	public float getRidingOffset() {
		if (getDataFlag(DATA_FLAG_BABY)) {
			return super.getRidingOffset();
		}
		return -0.5f;
	}

	@Override
	public Vector3f getMountedOffset(Entity entity) {
		return new Vector3f(0, 1.1f + entity.getRidingOffset(), -0.35f);
	}

	@Override
	public Item[] getDrops(){
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return new Item[]{
				Item.get(Item.ROTTEN_FLESH, 0, random.nextInt(3)),
		};
	}

	public double getAttackDistance() {
		return 1;
	}

	@Override
	public int getNetworkId(){
		return NETWORK_ID;
	}

	@Override
	protected void initEntity(){
		super.initEntity();

		dataProperties.putShort(DATA_ZOMBIE_TYPE, EntityZombie.ZOMBIE_TYPE_DEFAULT);

		setMaxHealth(20);
	}

	@Override
	protected void onAttackSuccess(EntityDamageByEntityEvent source) {
		if (!isOnFire()) {
			return;
		}
		int difficulty = level.getDifficulty();
		if (difficulty < Difficulty.NORMAL.ordinal()) {
			return;
		}
		if (!getEquipmentInventory().getItemInHand().isNull()) {
			return;
		}
		source.getEntity().setOnFire(2);
	}

	@Override
	public int getBaseArmorValue() {
		return 2;
	}
}
