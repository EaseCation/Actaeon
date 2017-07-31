package me.onebone.actaeon;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import me.onebone.actaeon.entity.MovingEntity;
import me.onebone.actaeon.entity.animal.Chicken;
import me.onebone.actaeon.entity.animal.Cow;
import me.onebone.actaeon.entity.animal.Pig;
import me.onebone.actaeon.entity.animal.Sheep;
import me.onebone.actaeon.entity.monster.Zombie;

public class Actaeon extends PluginBase implements Listener {

    private static Actaeon instance;

    public static Actaeon getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        if (instance == null) instance = this;
    }

    public void onEnable() {
        this.saveDefaultConfig();

        this.registerEntity("Sheep", Sheep.class);
        this.registerEntity("Cow", Cow.class);
        this.registerEntity("Chicken", Chicken.class);
        this.registerEntity("Pig", Pig.class);
        this.registerEntity("Zombie", Zombie.class);

        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, Zombie.NETWORK_ID));

        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 10));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 11));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 12));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 13));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 14));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 18));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 22));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 23));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 24));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 25));
        Item.addCreativeItem(Item.get(Item.SPAWN_EGG, 28));
        //this.getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerEntity(String name, Class<? extends MovingEntity> clazz) {
        Entity.registerEntity(name, clazz, true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getBlock();
        event.getPlayer().sendMessage("XYZ=" + block.getX() + "," + block.getY() + "," + block.getZ() + " ID=" + block.getId() + " META=" + block.getDamage());
    }
}
