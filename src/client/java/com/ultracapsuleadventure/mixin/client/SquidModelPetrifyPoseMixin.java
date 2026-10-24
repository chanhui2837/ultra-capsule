package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Freeze squid tentacle transforms after the first petrified pose is calculated. */
@Mixin(SquidModel.class)
public abstract class SquidModelPetrifyPoseMixin {
    @Shadow @Final private ModelPart[] tentacles;

    @Unique private static final Map<UUID, float[]> UltraCapsuleAdventure$FROZEN_SQUID_POSES = new HashMap<>();

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SquidRenderState;)V", at = @At("RETURN"))
    private void UltraCapsuleAdventure$freezeTentaclePose(SquidRenderState state, CallbackInfo ci) {
        if (!(state instanceof PetrifiedStateAccess access)) return;
        UUID uuid = access.UltraCapsuleAdventure$getRenderEntityUuid();
        if (uuid == null || tentacles == null) return;
        synchronized (UltraCapsuleAdventure$FROZEN_SQUID_POSES) {
            if (!access.UltraCapsuleAdventure$isPetrifiedState()) {
                UltraCapsuleAdventure$FROZEN_SQUID_POSES.remove(uuid);
                return;
            }
            float[] pose = UltraCapsuleAdventure$FROZEN_SQUID_POSES.get(uuid);
            if (pose == null || pose.length != tentacles.length * 6) {
                pose = new float[tentacles.length * 6];
                for (int i = 0; i < tentacles.length; i++) {
                    ModelPart part = tentacles[i]; int j = i * 6;
                    pose[j] = part.x; pose[j+1] = part.y; pose[j+2] = part.z;
                    pose[j+3] = part.xRot; pose[j+4] = part.yRot; pose[j+5] = part.zRot;
                }
                UltraCapsuleAdventure$FROZEN_SQUID_POSES.put(uuid, pose);
            } else {
                for (int i = 0; i < tentacles.length; i++) {
                    ModelPart part = tentacles[i]; int j = i * 6;
                    part.x = pose[j]; part.y = pose[j+1]; part.z = pose[j+2];
                    part.xRot = pose[j+3]; part.yRot = pose[j+4]; part.zRot = pose[j+5];
                }
            }
            if (UltraCapsuleAdventure$FROZEN_SQUID_POSES.size() > 2048) UltraCapsuleAdventure$FROZEN_SQUID_POSES.clear();
        }
    }
}
