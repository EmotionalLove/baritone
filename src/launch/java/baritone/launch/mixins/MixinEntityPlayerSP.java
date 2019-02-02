/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.type.EventState;
import baritone.behavior.LookBehavior;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Brady
 * @since 8/1/2018
 */
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendChatMessage(String msg, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(msg);
        BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this).getGameEventHandler().onSendChatMessage(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/entity/EntityPlayerSP.isRiding()Z",
                    shift = At.Shift.BY,
                    by = -3
            )
    )
    private void onPreUpdate(CallbackInfo ci) {
        BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this).getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
    }

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/entity/EntityPlayerSP.onUpdateWalkingPlayer()V",
                    shift = At.Shift.BY,
                    by = 2
            )
    )
    private void onPostUpdate(CallbackInfo ci) {
        BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this).getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "FIELD",
                    target = "net/minecraft/entity/player/PlayerCapabilities.allowFlying:Z"
            )
    )
    private boolean isAllowFlying(PlayerCapabilities capabilities) {
        IPathingBehavior pathingBehavior = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this).getPathingBehavior();
        return !pathingBehavior.isPathing() && capabilities.allowFlying;
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/settings/KeyBinding.isKeyDown()Z"
            )
    )
    private boolean isKeyDown(KeyBinding keyBinding) {
        SprintStateEvent event = new SprintStateEvent();
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this);
        baritone.getGameEventHandler().onPlayerSprintState(event);
        if (event.getState() != null) {
            return event.getState();
        }
        if (baritone != BaritoneAPI.getProvider().getPrimaryBaritone()) {
            // hitting control shouldn't make all bots sprint
            return false;
        }
        return keyBinding.isKeyDown();
    }

    @Inject(
            method = "updateRidden",
            at = @At(
                    value = "HEAD"
            )
    )
    private void updateRidden(CallbackInfo cb) {
        ((LookBehavior) BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this).getLookBehavior()).pig();
    }

    @Inject(
            method = "isCurrentViewEntity",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    private void isCurrentViewEntity(CallbackInfoReturnable<Boolean> cb) {
        // never called during frame I think
        cb.setReturnValue(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player() == (EntityPlayerSP) (Object) this);
    }

    @Inject(
            method = "isUser",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isUser(CallbackInfoReturnable<Boolean> callback) {
        Baritone baritone = (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone.getFreecamBehavior().enabled() && baritone.getFreecamBehavior().realPlayer() == (EntityPlayerSP) (Object) this) {
            callback.setReturnValue(false);
        }
    }
}
