package me.onebone.actaeon.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import me.onebone.actaeon.hook.MovingEntityHook;
import me.onebone.actaeon.route.Router;
import me.onebone.actaeon.target.TargetFinder;
import me.onebone.actaeon.task.MovingEntityTask;

import java.util.Map;

public interface IMovingEntity {

    EntityLiving getEntity();

    Level getLevel();

    Position getPosition();

    double getX();
    double getY();
    double getZ();

    double getYaw();

    double getPitch();

    double distanceSquared(Vector3 pos);

    double distance(Vector3 pos);

    String getName();

    void jump();

    void setTargetFinder(TargetFinder targetFinder);

    TargetFinder getTargetFinder();

    default void setTarget(Vector3 vec, String identifier) {
        this.setTarget(vec, identifier, false);
    }

    void setTarget(Vector3 vec, String identifier, boolean immediate);

    Vector3 getRealTarget();

    Vector3 getTarget();

    void setHate(Entity hate);

    Entity getHate();

    Router getRouter();

    boolean isLookAtFront();

    void setLookAtFront(boolean lookAtFront);

    boolean isAutoCollide();

    void setAutoCollide(boolean autoCollide);

    Map<String, MovingEntityHook> getHooks();

    void addHook(String key, MovingEntityHook hook);

    MovingEntityTask getTask();

    void updateBotTask(MovingEntityTask task);

    default int getDifficulty() {
        return getLevel().getDifficulty();
    }

    Location add(Vector3 vec);

    Location add(double x, double y, double z);
}
