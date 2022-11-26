package me.onebone.actaeon.route;

import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import me.onebone.actaeon.entity.IMovingEntity;
import net.easecation.eccommons.promise.AsyncPromise;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class Router implements Iterator<Node> {

	private static final BitSet canPassThrough;

	static {
		canPassThrough = new BitSet();
		canPassThrough.set(0);
		Item.getCreativeItems().stream().
				filter(item -> item.getBlock().getId() > 0 && item.getBlock().canPassThrough()).
				forEach(item -> canPassThrough.set(item.getId()));
	}

	// =====================

	private IRouteFinder routeFinder = new SimpleRouteFinder();  //默认使用Simple寻路算法
	private int current = 0;
	// 目的地
	protected Position destination = null;
	private boolean arrived = false;
	protected List<Node> nodes = new ArrayList<>();

	protected IMovingEntity entity;
	public long nextRouteFind = System.currentTimeMillis();

	/**
	 * 正在执行中的任务，如果已经执行完了，那么应该为null
	 */
	private AsyncPromise<List<Node>> promise = null;

	public Router(IMovingEntity entity) {
		if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
		this.entity = entity;
	}

	public void setDestination(Position destination) {
		this.setDestination(destination, false);
	}

	public void setDestination(Position destination, boolean immediate) {
		this.destination = destination;
		if (immediate) {
			this.nextRouteFind = System.currentTimeMillis();
		}
	}

	public void onTick() {
		if (!this.isSearching() && System.currentTimeMillis() >= this.nextRouteFind) {
			if (destination == null) return;
			// TODO 判断是否需要重新寻路
			this.nextRouteFind = System.currentTimeMillis() + routeFinder.getRouteFindCooldown();

			this.promise = routeFinder.search(entity.getEntity(), entity.getPosition(), destination);

			promise.whenSuccess(result -> {
				this.arrived = false;
				this.current = 0;
				this.nodes = result;
			});
			promise.whenCompleted(() -> this.promise = null);
		}
	}

	public IMovingEntity getEntity() {
		return this.entity;
	}

	public boolean isSearching() {
		return this.promise != null;
	}

	public IRouteFinder getRouteFinder() {
		return routeFinder;
	}

	public Router setRouteFinder(IRouteFinder routeFinder) {
		this.routeFinder = routeFinder;
		return this;
	}

	/**
	 * @return true if it has next node to go
	 */
	@Override
	public boolean hasNext() {
		if (nodes.size() == 0) throw new IllegalStateException("There is no path found");

		return !this.arrived && nodes.size() > this.current + 1;
	}

	/**
	 * Move to next node
	 * @return true if succeed
	 */
	@Override
	public Node next() {
		if (nodes.size() == 0) {
			throw new IllegalStateException("There is no path found");
		}

		if (this.hasNext()) {
			this.current++;
			return this.nodes.get(this.current);
		}
		return null;
	}

	/**
	 * Returns if the entity has reached the node
	 * @return true if reached
	 */
	public boolean hasReachedNode(Vector3 vec) {
		Vector3 cur = this.get().getVector3();

		/*return NukkitMath.floorDouble(vec.x) ==  NukkitMath.floorDouble(cur.x)
				&& NukkitMath.floorDouble(vec.y) == NukkitMath.floorDouble(cur.y)
				&& NukkitMath.floorDouble(vec.z) == NukkitMath.floorDouble(cur.z);*/
		return vec.x == cur.x
				//&& vec.y == cur.y
				&& vec.z == cur.z;
	}

	/**
	 * Gets node of current
	 * @return current node
	 */
	public Node get() {
		if (nodes.size() == 0) throw new IllegalStateException("There is no path found.");

		if (this.arrived) return null;
		//new ArrayList<>(this.nodes).forEach(n -> Server.broadcastPacket(level.getPlayers().values().stream().toArray(Player[]::new), new EntityFlameParticle(n.getVector3()).encode()[0]));
		if (this.current >= this.nodes.size()) {
			return null;
		}
		return nodes.get(current);
	}

	public void arrived() {
		this.current = 0;
		this.arrived = true;
	}

	public boolean hasRoute() {
		return this.nodes.size() > 0;
	}

	public static boolean canPassThrough(int blockId) {
		return canPassThrough.get(blockId);
	}
}