package com.williambl.boatsandbeeps.upgrade

import net.minecraft.util.math.Vec3d

enum class BoatUpgradeSlot(val position: Vec3d) {
    BOW(Vec3d(1.0, 0.0, 0.0)),
    FRONT(Vec3d(0.2, 0.0, 0.0)),
    PORT(Vec3d(0.0, 0.0, 0.2)),
    STARBOARD(Vec3d(0.0, 0.0, -0.2)),
    BACK(Vec3d(-0.6, 0.0, 0.0)),
    AFT(Vec3d(-1.0, 0.0, 0.0))
}