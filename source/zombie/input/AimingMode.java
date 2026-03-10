// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.GameTime;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;

public enum AimingMode {
    REGULAR(0.035F, 0.025F),
    TARGET_FOUND(0.01F, 0.0075F),
    NOT_AIMING(1.0F, 1.0F);

    final float lerpRateOutwards;
    final float lerpRateInwards;

    private AimingMode(final float outwards, final float inwards) {
        this.lerpRateOutwards = outwards;
        this.lerpRateInwards = inwards;
    }

    float lerpAxis(float src, float target, float middle) {
        float absDiffSrc = PZMath.abs(middle - src);
        float absDiffTarget = PZMath.abs(middle - target);
        float aimingLerpRate = absDiffTarget > absDiffSrc ? this.lerpRateOutwards : this.lerpRateInwards;
        float aimingLerpRateClamped = PZMath.clamp(aimingLerpRate * GameTime.getInstance().getMultiplier(), 0.0F, 1.0F);
        return PZMath.lerp(src, target, aimingLerpRateClamped, LerpType.Linear);
    }
}
