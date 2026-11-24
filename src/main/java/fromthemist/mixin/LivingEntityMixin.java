package fromthemist.mixin;

import fromthemist.rpg.Rpg;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lets skills and talents scale damage dealt and taken, in one place, before armour is applied. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float fromthemist$scaleDamage(final float damage, final ServerLevel level, final DamageSource source) {
		return Rpg.modifyDamage((LivingEntity) (Object) this, source, damage);
	}
}
