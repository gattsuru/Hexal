package ram.talia.hexal.common.casting.actions.spells

import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.ParticleSpray
import at.petrak.hexcasting.api.spell.RenderedSpell
import at.petrak.hexcasting.api.spell.SpellDatum
import at.petrak.hexcasting.api.spell.SpellOperator
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import com.mojang.datafixers.util.Either
import net.minecraft.core.BlockPos
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.*

object OpSmelt : SpellOperator {
    const val COST_PER_SMELT = 0.75 * ManaConstants.DUST_UNIT

    override val argc = 1

    fun numToSmelt(toSmelt: Either<Vec3, ItemEntity>): Int {
        return toSmelt.map({ 1 }, { item -> item.item.count })
    }

    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): Triple<RenderedSpell, Int, List<ParticleSpray>>? {
        val toSmelt = when (val _toSmelt = args[0].payload) {
            is Vec3 -> Either.left(Vec3.atCenterOf(BlockPos(_toSmelt)))
            is ItemEntity -> Either.right(_toSmelt)
            else -> throw MishapInvalidIota(args[0], 0, "hexal.mishap.invalid_value.vecitem".asTranslatedComponent)
        }

        toSmelt.map({ vec -> ctx.assertVecInRange(vec) }, { item -> ctx.assertEntityInRange(item) })

        val pos = toSmelt.map({ vec -> vec }, { item -> item.position() })

        return Triple(
            Spell(toSmelt),
            (COST_PER_SMELT * numToSmelt(toSmelt)).toInt(),
            listOf(ParticleSpray.burst(pos, 1.0))
        )
    }

    private data class Spell(val vOrI: Either<Vec3, ItemEntity>) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            vOrI.map({vec -> // runs this code if the player passed a BlockPos
                val pos = BlockPos(vec)
                // if (!ctx.canEditBlockAt(pos)) return@map
                val blockState = ctx.world.getBlockState(pos)
                // if (!IXplatAbstractions.INSTANCE.isBreakingAllowed(ctx.world, pos, blockstate, ctx.caster)) return@map

                // Stealing code from Ars Nouveau
                val optional: Optional<SmeltingRecipe> = ctx.world.recipeManager.getRecipeFor(
                    RecipeType.SMELTING, SimpleContainer(ItemStack(blockState.getBlock().asItem(), 1)),
                    ctx.world
                )

                if (!optional.isPresent) return@map

                val itemStack = optional.get().resultItem

                if (itemStack.isEmpty) return@map

                if (itemStack.item is BlockItem) {
                    ctx.world.setBlockAndUpdate(pos, (itemStack.item as BlockItem).block.defaultBlockState())
                } else {
                    ctx.world.destroyBlock(pos, false, ctx.caster)
                    ctx.world.addFreshEntity(ItemEntity(ctx.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), itemStack.copy()))
                    // Send a block update, also copied from Ars Nouveau (this is all copied from Ars Nouveau
                    if (!ctx.world.isOutsideBuildHeight(pos))
                        ctx.world.sendBlockUpdated(pos, ctx.world.getBlockState(pos), ctx.world.getBlockState(pos), 3) // don't know how this works
                }

            }, {itemEntity -> // runs this code if the player passed an ItemEntity
                val optional: Optional<SmeltingRecipe> = ctx.world.recipeManager.getRecipeFor(
                    RecipeType.SMELTING, SimpleContainer(ItemStack(itemEntity.item.item, 1)),   // cursed .item.item to map from ItemEntity to ItemLike to ItemStack
                    ctx.world
                )

                if (!optional.isPresent) return@map

                val result = optional.get().resultItem.copy()

                if (result.isEmpty) return@map

                result.count = itemEntity.item.count

                itemEntity.remove(Entity.RemovalReason.DISCARDED)
                ctx.world.addFreshEntity(ItemEntity(ctx.world, itemEntity.x, itemEntity.y, itemEntity.z, result.copy()))
            })
        }
    }
}