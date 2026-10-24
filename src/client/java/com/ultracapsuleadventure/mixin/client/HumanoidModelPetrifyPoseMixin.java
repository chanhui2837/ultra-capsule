package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Freeze the completed humanoid pose per ENTITY UUID, not per RenderState instance.
 * Render states are reused, so an identity-keyed RenderState cache recaptured the
 * animation pose on subsequent frames and caused visible trembling.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelPetrifyPoseMixin {
    @Shadow public ModelPart head;
    @Shadow public ModelPart hat;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Unique private static final Map<UUID, float[]> UltraCapsuleAdventure$FROZEN_POSES = new HashMap<>();

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void UltraCapsuleAdventure$freezeActualPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof PetrifiedStateAccess access)) return;
        UUID uuid = access.UltraCapsuleAdventure$getRenderEntityUuid();
        if (uuid == null) return;
        synchronized (UltraCapsuleAdventure$FROZEN_POSES) {
            if (!access.UltraCapsuleAdventure$isPetrifiedState()) {
                UltraCapsuleAdventure$FROZEN_POSES.remove(uuid);
                return;
            }
            ModelPart[] parts = { head, hat, body, rightArm, leftArm, rightLeg, leftLeg };
            float[] pose = UltraCapsuleAdventure$FROZEN_POSES.get(uuid);
            if (pose == null) {
                pose = new float[parts.length * 6];
                for (int i = 0; i < parts.length; i++) {
                    ModelPart part = parts[i]; int j = i * 6;
                    pose[j] = part.x; pose[j+1] = part.y; pose[j+2] = part.z;
                    pose[j+3] = part.xRot; pose[j+4] = part.yRot; pose[j+5] = part.zRot;
                }
                UltraCapsuleAdventure$FROZEN_POSES.put(uuid, pose);
                // Keep the first pose: this preserves the exact attack frame at petrification.
            } else {
                for (int i = 0; i < parts.length; i++) {
                    ModelPart part = parts[i]; int j = i * 6;
                    part.x = pose[j]; part.y = pose[j+1]; part.z = pose[j+2];
                    part.xRot = pose[j+3]; part.yRot = pose[j+4]; part.zRot = pose[j+5];
                }
            }
            // Bound cache size in case entities unload without an unpetrified render.
            if (UltraCapsuleAdventure$FROZEN_POSES.size() > 2048) UltraCapsuleAdventure$FROZEN_POSES.clear();
        }
    }
}
