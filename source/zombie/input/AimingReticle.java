// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;
import zombie.iso.Vector2;
import zombie.util.list.PZArrayUtil;

public class AimingReticle {
    private static final Vector2[] aimingPositions = PZArrayUtil.newInstance(Vector2.class, 4, Vector2::new);

    public static int getX(int playerIndex) {
        return (int)(getXA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getY(int playerIndex) {
        return (int)(getYA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getXA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        return player != null && player.joypadBind >= 0 ? (int)aimingPositions[playerIndex].x : Mouse.getXA();
    }

    public static int getYA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        return player != null && player.joypadBind >= 0 ? (int)aimingPositions[playerIndex].y : Mouse.getYA();
    }

    private static float axisToScreenX(float aimingX) {
        float screenHalfWidth = getScreenHalfWidth();
        return screenHalfWidth * (1.0F + aimingX);
    }

    private static float axisToScreenY(float aimingY) {
        float screenHalfHeight = getScreenHalfHeight();
        return screenHalfHeight * (1.0F + aimingY);
    }

    private static float renormalizeAimingAxisValue(float aimingAxisVal) {
        float aimingAxisMin = 0.3F;
        float aimingAxisMax = 0.9F;
        float aimingAxisSign = PZMath.sign(aimingAxisVal);
        float renormalizedAxis = PZMath.clamp((aimingAxisVal * aimingAxisSign - 0.3F) / 0.59999996F, 0.0F, 1.0F) * aimingAxisSign;
        float aimingMax = 0.8F;
        float aimingMin = 0.02F;
        return PZMath.lerp(0.02F, 0.8F, renormalizedAxis * aimingAxisSign, LerpType.EaseOutQuad) * aimingAxisSign;
    }

    private static float getAimingScreenX(IsoPlayer player) {
        float aimingAxisRaw = JoypadManager.instance.getAimingAxisX(player.joypadBind);
        float aimingAxis = renormalizeAimingAxisValue(aimingAxisRaw);
        return axisToScreenX(aimingAxis);
    }

    private static float getAimingScreenY(IsoPlayer player) {
        float aimingAxisRaw = JoypadManager.instance.getAimingAxisY(player.joypadBind);
        float aimingAxis = renormalizeAimingAxisValue(aimingAxisRaw);
        return axisToScreenY(aimingAxis);
    }

    private static float getScreenHalfWidth() {
        float screenWidth = Core.getInstance().getScreenWidth();
        return screenWidth / 2.0F;
    }

    private static float getScreenHalfHeight() {
        float screenHeight = Core.getInstance().getScreenHeight();
        return screenHeight / 2.0F;
    }

    public static void update() {
        float screenHalfWidth = getScreenHalfWidth();
        float screenHalfHeight = getScreenHalfHeight();

        for (int i = 0; i < aimingPositions.length; i++) {
            IsoPlayer player = IsoPlayer.getPlayer(i);
            if (player != null && player.joypadBind > -1) {
                AimingMode aimingMode = player.getAimingMode();
                aimingPositions[i].x = aimingMode.lerpAxis(aimingPositions[i].x, getAimingScreenX(player), screenHalfWidth);
                aimingPositions[i].y = aimingMode.lerpAxis(aimingPositions[i].y, getAimingScreenY(player), screenHalfHeight);
            }
        }
    }
}
