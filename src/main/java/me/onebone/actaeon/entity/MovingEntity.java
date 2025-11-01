package me.onebone.actaeon.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.attribute.Attribute;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityDamageEvent.DamageModifier;
import cn.nukkit.inventory.ArmorInventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.item.Items;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Mth;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import cn.nukkit.potion.Effect;
import me.onebone.actaeon.hook.MovingEntityHook;
import me.onebone.actaeon.inventory.EntityEquipmentInventory;
import me.onebone.actaeon.route.Node;
import me.onebone.actaeon.route.Router;
import me.onebone.actaeon.target.TargetFinder;
import me.onebone.actaeon.task.MovingEntityTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

abstract public class MovingEntity extends EntityCreature implements IMovingEntity, InventoryHolder {

	public static final String TAG_MAINHAND = "Mainhand";
	public static final String TAG_OFFHAND = "Offhand";
	public static final String TAG_ARMOR = "Armor";

	private boolean isKnockback = false;
	private final Router router;
	private TargetFinder targetFinder = null;
	private Vector3 target = null;
	private Entity hate = null;
	private String targetSetter = "";
	public boolean routeLeading = true;
	private final Map<String, MovingEntityHook> hooks = new HashMap<>();
	private MovingEntityTask task = null;
	protected boolean lookAtFront = true;
	protected boolean autoCollide = true;
	protected float collidePlayerToMove = 0f;

	private EntityEquipmentInventory equipmentInventory;
	private ArmorInventory armorInventory;

	public MovingEntity(FullChunk chunk, CompoundTag nbt){
		super(chunk, nbt);

		this.router = new Router(this);
		this.setImmobile(false);
	}

	@Override
	protected void initEntity() {
		super.initEntity();

		// this.setDataFlag(DATA_FLAG_NO_AI, true, false);
		this.setDataFlag(DATA_FLAG_CAN_WALK, true, false);
		this.setDataFlag(DATA_FLAG_GRAVITY, true, false);

		this.equipmentInventory = new EntityEquipmentInventory(this);
		this.armorInventory = new ArmorInventory(this);

		if (this.namedTag.contains(TAG_MAINHAND)) {
			this.equipmentInventory.setItemInHand(NBTIO.getItemHelper(this.namedTag.getCompound(TAG_MAINHAND)), true);
		}

		if (this.namedTag.contains(TAG_OFFHAND)) {
			this.equipmentInventory.setItemInOffhand(NBTIO.getItemHelper(this.namedTag.getCompound(TAG_OFFHAND)), true);
		}

		if (this.namedTag.contains(TAG_ARMOR)) {
			ListTag<CompoundTag> armorList = this.namedTag.getList(TAG_ARMOR, CompoundTag.class);
			for (CompoundTag armorTag : armorList.getAll()) {
				this.armorInventory.setItem(armorTag.getByte("Slot"), NBTIO.getItemHelper(armorTag));
			}
		}
	}

	@Override
	public EntityLiving getEntity() {
		return this;
	}

	public Map<String, MovingEntityHook> getHooks() {
		return hooks;
	}

	public void addHook(String key, MovingEntityHook hook) {
		this.hooks.put(key, hook);
	}

	@Override
	protected float getGravity() {
		return 0.092f;
	}

	@Override
	protected double getStepHeight() {
		return 0.6; // Enable step climbing like vanilla players
	}

	public Entity getHate() {
		return hate;
	}

	public void setHate(Entity hate) {
		this.hate = hate;
	}

	public void jump() {
		if (this.onGround){
			this.motionY = getJumpPower() + getJumpBoostPower();
		}
	}

	protected float getJumpPower() {
//		return 0.42f;
		return 0.35f;
	}

	public float getJumpBoostPower() {
		Effect jumpBoost = getEffect(Effect.JUMP_BOOST);
		if (jumpBoost == null) {
			return 0;
		}
		return 0.1f * (jumpBoost.getAmplifier() + 1);
	}

	@Override
	public boolean onUpdate(int currentTick) {
		super.onUpdate(currentTick);
		return true;
	}

	@Override
	public boolean entityBaseTick(int tickDiff){
		if(this.closed){
			return false;
		}

		new ArrayList<>(this.hooks.values()).forEach(hook -> hook.onUpdate(Server.getInstance().getTick()));
		if (this.task != null) this.task.onUpdate(Server.getInstance().getTick());

		boolean hasUpdate = super.entityBaseTick(tickDiff);

		if (this.isKnockback) {                   // knockback 이 true 인 경우는 맞은 직후

		} else if(this.routeLeading && this.onGround) {
			this.motionX = this.motionZ = 0;
		}

		this.motionX *= (1 - this.getDrag());
		this.motionZ *= (1 - this.getDrag());
		if (this.motionX < 0.001 && this.motionX > -0.001) this.motionX = 0;
		if (this.motionZ < 0.001 && this.motionZ > -0.001) this.motionZ = 0;

		if (this.targetFinder != null) this.targetFinder.onUpdate();

		// 如果没在寻路，但是设置了目标，达到了下次寻路计划的时间，就开始寻路
		if (this.routeLeading) {
			this.router.onTick();
			hasUpdate = true;
		}
		/*if (this.routeLeading && this.onGround
				&& this.hasSetTarget() &&
				!this.route.isSearching()
				&& System.currentTimeMillis() >= this.route.nextRouteFind
				&& (this.route.getDestination() == null || this.route.getDestination().distance(this.getTarget()) > 2))
		{ // 대상이 이동함

			if (RouteFinderSearchAsyncTask.getTaskSize() < 50) Server.getInstance().getScheduler().scheduleAsyncTask(new RouteFinderSearchAsyncTask(this.route, this.level, this, this.getTarget(), this.boundingBox));

			*//*if(this.route.isSearching()) this.route.research();
			else this.route.search();*//*

			hasUpdate = true;
		}*/

		if (!isRiding() && !this.isImmobile()) {
			// 如果未在寻路，并且有寻路路径，则控制实体前往下一个节点
			if (this.routeLeading && (!this.isKnockback || this.getGravity() == 0) && !this.router.isSearching() && this.router.hasRoute() && !router.needStop()) { // entity has route to go
				hasUpdate = true;

				// 获取下一寻路的节点
				Node node = this.router.get();
				if (node != null) {
					Vector3 vec = node.getVector3();
					double diffX = Math.pow(vec.x - this.x, 2);
					double diffZ = Math.pow(vec.z - this.z, 2);
					double diffY = this.getGravity() == 0 ? Math.pow(vec.y - this.y, 2) : 0;

					double totalDiff = diffX + diffZ + diffY; // 这里得到所有diff的和

					// 已经达到了节点
					if (totalDiff == 0) {
						// 那么将节点调至下一个节点，如果没有下一个节点了，则到达目的地
						if (this.router.hasNext()) {
							this.router.next();
							//Server.getInstance().getLogger().warning(vec.toString());
						} else {
							this.router.arrived();
						}
					} else {
						if (totalDiff > 0) {
							double ratioX = diffX / totalDiff; // 使用各自的diff除以所有diff的和
							double ratioZ = diffZ / totalDiff;
							double ratioY = diffY / totalDiff;

							int negX = vec.x - this.x < 0 ? -1 : 1;
							int negZ = vec.z - this.z < 0 ? -1 : 1;
							int negY = vec.y - this.y < 0 ? -1 : 1;

							float movementSpeed = this.getMovementSpeed();
							Effect speed = getEffect(Effect.SPEED);
							if (speed != null) {
								movementSpeed += this.getMovementSpeed() * 0.2f * (speed.getAmplifier() + 1);
							}
							Effect slowness = getEffect(Effect.SLOWNESS);
							if (slowness != null) {
								movementSpeed -= this.getMovementSpeed() * 0.15f * (slowness.getAmplifier() + 1);
							}

							this.motionX = Math.min(Math.abs(vec.x - this.x), ratioX * movementSpeed) * negX;
							this.motionZ = Math.min(Math.abs(vec.z - this.z), ratioZ * movementSpeed) * negZ;

							if (this.getGravity() == 0) {
								this.motionY = Math.min(Math.abs(vec.y - this.y), ratioY * movementSpeed) * negY;
							}
						}
						if (this.lookAtFront) {
							double angle = Mth.atan2(vec.z - this.z, vec.x - this.x);
							this.setRotation((angle * 180) / Math.PI - 90, 0);
						}
					}
				}
			}

			for (Entity entity : this.getLevel().getCollidingEntities(this.boundingBox, this)) {
				if (entity.canCollide() /*&& entity.canCollideWith(this)*/) {
					if (entity instanceof EntityHuman) {
						this.onCollideWithPlayer((EntityHuman) entity);
						// 如果 collidePlayerToMove > 0，则应用运动向量来推动玩家
						if (collidePlayerToMove > 0f) {
							Vector3 playerMotion = entity.subtract(this).normalize().multiply(collidePlayerToMove);
							playerMotion.y = -0.1f; // 向下施加一点重力
							entity.setMotion(playerMotion);
						}
					}
					if (autoCollide) {
						double collisionFactor = this.getEntityCollisionFactor(entity);
						Vector3 motion = this.subtract(entity).normalize();
						this.motionX += motion.x * collisionFactor;
						this.motionZ += motion.z * collisionFactor;
					}
				}
			}

			if ((this.motionX != 0 || this.motionZ != 0) && this.isCollidedHorizontally){
				this.jump();
			}

			Effect levitation = getEffect(Effect.LEVITATION);
			if (levitation != null) {
				this.motionY = 0.045f * (levitation.getAmplifier() + 1);
				resetFallDistance();
			}

			this.move(this.motionX, this.motionY, this.motionZ);

			this.checkGround();
			if (!this.onGround) {
				float gravity = this.getGravity();

				if (gravity > 0 && hasEffect(Effect.SLOW_FALLING)) {
					gravity = Math.min(gravity, 0.01f);
					resetFallDistance();
				}

				this.motionY -= gravity;
				//Server.getInstance().getLogger().warning(this.getId() + ": 不在地面, 掉落 motionY=" + this.motionY);
				hasUpdate = true;
			} else {
				this.isKnockback = false;
			}
		}

		return hasUpdate;
	}

	public double getRange() {
		return 100.0;
	}

	public void setTarget(Vector3 vec, String identifier) {
		this.setTarget(vec, identifier, false);
	}

	public void setTarget(Vector3 vec, String identifier, boolean immediate) {
		if (identifier == null) return;

		if (vec == null || immediate || !this.hasSetTarget() || identifier.equals(this.targetSetter)) {
			this.target = vec;
			this.targetSetter = identifier;
		}

		// 如果设置了新的目标，则按需重新开始寻路ding
		// 这边可以直接把某个实体设为Target，会被无缝传入到寻路中，自动更新寻路目标坐标
		if (vec != null) {
			this.router.setDestination(vec instanceof Position ? (Position) vec : Position.fromObject(vec, this.level), immediate || !this.router.hasRoute());
		} else {
			this.router.setDestination(null, true);
		}
	}

    public Vector3 getRealTarget() {
        return this.target;
    }

	public Vector3 getTarget() {
		if (this.target == null) return null;
		return new Vector3(this.target.x, this.target.y, this.target.z);
	}


	/**
	 * Returns whether the entity has set its target
	 * The entity may not follow the target if there is following target and set target is different
	 * If following distance of target is too far to follow or cannot reach, set target will be the next following target
	 */
	public boolean hasSetTarget(){
		double range = this.getRange();
		return this.target != null && this.distanceSquared(this.target) < range * range;
	}

	@Override
	protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
		this.isCollidedVertically = movY != dy;
		this.isCollidedHorizontally = (movX != dx || movZ != dz);
		this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);

		// this.onGround = (movY != dy && movY < 0);
		// onGround 는 onUpdate 에서 확인
	}

	private void checkGround(){
		AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(0, -1, 0) : this.boundingBox.addCoord(0, -1, 0), false);

		double maxY = Level.MIN_Y;
		for (AxisAlignedBB bb : list) {
			if (bb.getMaxY() > maxY) {
				maxY = bb.getMaxY();
			}
		}

		this.onGround = (maxY == this.boundingBox.getMinY());
	}

	@Override
	public boolean setHealth(float health) {
		if (!super.setHealth(health)) {
			return false;
		}
		UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
		pk0.entityId = this.getId();
		pk0.entries = new Attribute[]{
				Attribute.getAttribute(Attribute.HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
		};
		Server.broadcastPacket(this.getViewers().values(), pk0);
		return true;
	}

	@Override
	public void setMaxHealth(int maxHealth) {
		super.setMaxHealth(maxHealth);
		if (this.getHealth() > maxHealth) this.health = maxHealth;
		UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
		pk0.entityId = this.getId();
		pk0.entries = new Attribute[]{
				Attribute.getAttribute(Attribute.HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
		};
		Server.broadcastPacket(this.getViewers().values(), pk0);
	}

	@Override
	public void spawnTo(Player player) {
		super.spawnTo(player);
		this.equipmentInventory.sendContents(player);
		this.armorInventory.sendContents(player);
	}

	@Override
	public void saveNBT() {
		super.saveNBT();

		if (this.equipmentInventory != null) {
			this.namedTag.put(TAG_MAINHAND, NBTIO.putItemHelper(this.equipmentInventory.getItemInHand()));
			this.namedTag.put(TAG_OFFHAND, NBTIO.putItemHelper(this.equipmentInventory.getItemInOffhand()));
		}

		if (this.armorInventory != null) {
			ListTag<CompoundTag> armorTag = new ListTag<>(TAG_ARMOR);
			for (int i = 0; i < 4; i++) {
				armorTag.add(NBTIO.putItemHelper(this.armorInventory.getItem(i), i));
			}
			this.namedTag.putList(armorTag);
		}
	}

	@Override
	public boolean attack(EntityDamageEvent source) {
		if (this.isClosed() || !this.isAlive()) {
			return false;
		}

		int armorDamage = 0;

		if (source.getCause() != EntityDamageEvent.DamageCause.VOID && source.getCause() != DamageCause.SUICIDE && source.getCause() != EntityDamageEvent.DamageCause.CUSTOM && source.getCause() != EntityDamageEvent.DamageCause.SONIC_BOOM && source.getCause() != EntityDamageEvent.DamageCause.HUNGER) {
			int breach = 0;
			if (source instanceof EntityDamageByEntityEvent damageByEntityEvent) {
				Enchantment[] enchantments = damageByEntityEvent.getWeaponEnchantments();
				if (enchantments != null) {
					for (Enchantment enchantment : enchantments) {
						if (enchantment.getId() == Enchantment.BREACH) {
							breach = enchantment.getLevel();
							break;
						}
					}
				}
			}

			int armorPoints = getBaseArmorValue();
			int toughness = 0;
			int epf = 0;

			ArmorInventory armorInventory = this.getArmorInventory();
			for (Item armor : armorInventory.getContents().values()) {
				armorPoints += armor.getArmorPoints();
				toughness += armor.getToughness();
				epf += calculateEnchantmentProtectionFactor(armor, source);
			}

			if (source.canBeReducedByArmor()) {
				armorDamage = Math.max(1, (int) source.getDamage() / 4);

				armorPoints = Mth.clamp(armorPoints, 0, 20);
				float damage = source.getFinalDamage();
				float armorFraction;
				if (level.isNewArmorMechanics()) {
					armorFraction = Mth.clamp(armorPoints - damage / (2 + toughness / 4f), armorPoints * 0.2f, 20) / 25;
				} else {
					armorFraction = armorPoints / 25f;
				}
				if (breach > 0) {
					armorFraction = Math.max(armorFraction - breach * 0.15f, 0);
				}
				source.setDamage(-damage * armorFraction, DamageModifier.ARMOR);
			}

			source.setDamage(-source.getFinalDamage() * (Mth.clamp(epf, 0, 20) / 25f), DamageModifier.ARMOR_ENCHANTMENTS);

			source.setDamage(-Math.min(this.getAbsorption(), source.getFinalDamage()), EntityDamageEvent.DamageModifier.ABSORPTION);
		}

		this.hooks.forEach((name, hook) -> hook.onDamage(source));

		if (super.attack(source)) {
			if (source.getCause() == DamageCause.SUICIDE) {
				return true;
			}

			Entity damager = null;

			if (source instanceof EntityDamageByEntityEvent) {
				damager = ((EntityDamageByEntityEvent) source).getDamager();
			}

			for (int slot = 0; slot < 4; slot++) {
				Item armor = damageArmor(armorInventory.getItem(slot), armorDamage, source.getCause(), damager);
				armorInventory.setItem(slot, armor, armor.getId() != BlockID.AIR);
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setOnFire(int seconds) {
		int level = 0;
		for (Item armor : this.getArmorInventory().getContents().values()) {
            int fireProtection = armor.getEnchantmentLevel(Enchantment.FIRE_PROTECTION);
            if (fireProtection <= 0) {
                continue;
            }
			level += fireProtection;
		}

		int ticks = (int) (seconds * 20 * (level * -0.15 + 1));

		if (ticks > 0 && (hasEffect(Effect.FIRE_RESISTANCE) || !isAlive())) {
			extinguish();
			return;
		}

		if (ticks > fireTicks) {
			fireTicks = ticks;
		}
	}

	protected int calculateEnchantmentProtectionFactor(Item item, EntityDamageEvent source) {
		if (!item.hasEnchantments()) {
			return 0;
		}

		int epf = 0;

		for (Enchantment ench : item.getEnchantments()) {
			epf += ench.getProtectionFactor(source);
		}

		return epf;
	}

	protected Item damageArmor(Item armor, int armorDamage, DamageCause cause, Entity damager) {
		if (armor.hasEnchantments()) {
			if (damager != null) {
				for (Enchantment enchantment : armor.getEnchantments()) {
					enchantment.doPostAttack(armor, damager, this, cause);
				}

				if (armor.getDamage() > armor.getMaxDurability()) {
					level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_BREAK);
					return Items.air();
				}
			}
		}

		if (armorDamage == 0) {
			return armor;
		}

		switch (cause) {
			case VOID:
			case MAGIC:
			case WITHER:
			case HUNGER:
			case DROWNING:
            case DEHYDRATION:
			case SUFFOCATION:
			case SUICIDE:
			case FIRE_TICK:
			case FREEZE:
			case TEMPERATURE:
			case FALL:
			case STALAGMITE:
			case FLY_INTO_WALL:
			case SONIC_BOOM:
				return armor;
		}

		if (!armor.isArmor() || armor.getId() == Item.ELYTRA) {
			return armor;
		}

		if (armor.isFireResistant() && (cause == DamageCause.FIRE || cause == DamageCause.LAVA || cause == DamageCause.MAGMA || cause == DamageCause.CAMPFIRE || cause == DamageCause.SOUL_CAMPFIRE)) {
			return armor;
		}

		int itemDamaged = armor.hurtAndBreak(armorDamage);
		if (itemDamaged == 0) {
			return armor;
		}

		if (itemDamaged < 0) {
			getLevel().addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_BREAK);
			return Item.get(BlockID.AIR, 0, 0);
		}

		return armor;
	}

	public ArmorInventory getArmorInventory() {
		return armorInventory;
	}

	public EntityEquipmentInventory getEquipmentInventory() {
		return equipmentInventory;
	}

	public EntityEquipmentInventory getInventory() {
		return equipmentInventory;
	}

	@Override
	public void knockBack(Entity attacker, double damage, double x, double z, double base){
		this.isKnockback = true;
		super.knockBack(attacker, damage, x, z, base);
	}

	@Override
	public void knockBack(Entity attacker, double damage, double x, double z, double baseH, double baseV) {
		this.isKnockback = true;
		super.knockBack(attacker, damage, x, z, baseH, baseV);
	}

	@Override
	protected float getKnockbackResistance() {
		float total = 0;
		for (Item armor : armorInventory.getContents().values()) {
			total += armor.getKnockbackResistance();
		}
		return total;
	}

    public Router getRouter() {
        return router;
    }

    public void setTargetFinder(TargetFinder targetFinder) {
        this.targetFinder = targetFinder;
    }

	@Override
	public TargetFinder getTargetFinder() {
		return targetFinder;
	}

	public void updateBotTask(MovingEntityTask task) {
		if (this.task != null) this.task.forceStop();
		this.task = task;
		if (task != null) this.task.onUpdate(Server.getInstance().getTick());
	}

	public MovingEntityTask getTask() {
		return task;
	}

	public boolean isLookAtFront() {
		return lookAtFront;
	}

	public void setLookAtFront(boolean lookAtFront) {
		this.lookAtFront = lookAtFront;
	}

	public boolean isAutoCollide() {
		return autoCollide;
	}

	public void setAutoCollide(boolean autoCollide) {
		this.autoCollide = autoCollide;
	}

	public void playAnimation(String animation) {
		this.getViewers().values().forEach(p -> p.playAnimation(animation, this.getId()));
	}

	/**
	 * 实体碰撞因数，数值越小被碰撞的影响越小
	 */
	public double getEntityCollisionFactor(Entity entity) {
		return 0.2f;
	}

	@Override
	public boolean canDisableShield() {
		return equipmentInventory.getItemInHand().isAxe();
	}
}
