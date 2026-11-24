package fromthemist.mixin;

import fromthemist.DeathRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Death is certain by the time die() runs (totems have had their chance), and nothing has been dropped yet. */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	@Inject(method = "die", at = @At("HEAD"))
	private void fromthemist$beforeDeath(final DamageSource source, final CallbackInfo info) {
		DeathRules.onDeath((ServerPlayer) (Object) this);
	}
}
