package pw.tmpim.goodflags.data

import emmathemartian.datagen.DataGenContext
import emmathemartian.datagen.provider.CraftingRecipeProvider
import emmathemartian.datagen.util.DataIngredient
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import pw.tmpim.goodflags.GoodFlags

class GoodFlagsCraftingRecipeProvider(ctx: DataGenContext) : CraftingRecipeProvider(ctx) {
  override fun run(ctx: DataGenContext) {
    shaped()
      .pattern("LLL")
      .pattern("LWL")
      .pattern("LPL")
      .define('L', DataIngredient.of(Item.LEATHER))
      .define('W', DataIngredient.of(Block.WOOL.asItem(), 1, -1))
      .define('P', DataIngredient.of(GoodFlags.paletteItem))
      .result(ItemStack(GoodFlags.flagBlock))
      .save("flag", this, ctx)
    shapeless()
      .ingredient(DataIngredient.of(Item.DYE, 1, 0)) //Ink
      .ingredient(DataIngredient.of(Item.DYE, 1, 1)) //Red
      .ingredient(DataIngredient.of(Item.DYE, 1, 2)) //Cactus green
      .ingredient(DataIngredient.of(Item.DYE, 1, 4)) //Lapis
      .ingredient(DataIngredient.of(Item.DYE, 1, 15)) //Bonemeal
      .ingredient(DataIngredient.of(Block.WOODEN_PRESSURE_PLATE.asItem(), 1))
      .result(ItemStack(GoodFlags.paletteItem))
      .save("palette", this, ctx)
  }
}
