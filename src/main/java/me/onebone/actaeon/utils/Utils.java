package me.onebone.actaeon.utils;

import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;

/**
 * Created by CreeperFace on 19.7.2017.
 */
public class Utils {

    public static Vector3 randomVector(Vector3 from, int xRadius, int yRadius, int zRadius) {
        NukkitRandom random = new NukkitRandom();

        return from.add(random.nextRange(-xRadius, xRadius), random.nextRange(-yRadius, yRadius), random.nextRange(-zRadius, zRadius));
    }
}
