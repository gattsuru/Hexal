package ram.talia.hexal.common.casting.actions

import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.ConstManaOperator
import at.petrak.hexcasting.api.spell.SpellDatum
import at.petrak.hexcasting.api.spell.asSpellResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.mishaps.MishapNoSpellCircle
import ram.talia.hexal.api.spell.casting.MixinCastingContextInterface

object OpWispMedia : ConstManaOperator {
	override val argc = 0

	override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>> {
		val mCast = ctx as? MixinCastingContextInterface

		if (mCast == null || mCast.wisp == null)
			throw MishapNoSpellCircle()

		return (mCast.wisp.media.toFloat() / ManaConstants.DUST_UNIT).asSpellResult
	}
}