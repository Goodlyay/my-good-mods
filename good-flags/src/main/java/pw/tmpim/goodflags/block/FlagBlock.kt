package pw.tmpim.goodflags.block

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.material.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.modificationstation.stationapi.api.block.HasCustomBlockItemFactory
import net.modificationstation.stationapi.api.template.block.TemplateBlockWithEntity
import pw.tmpim.goodflags.GoodFlags.namespace
import pw.tmpim.goodflags.block.FlagSpec.FLAG_THICKNESS
import pw.tmpim.goodflags.block.FlagSpec.FLAG_WALL_FLOAT
import pw.tmpim.goodflags.item.FlagBlockItem
import pw.tmpim.goodflags.net.FlagNetworkingS2C
import pw.tmpim.goodutils.block.OnPlaceItemStack
import pw.tmpim.goodutils.net.sendToPlayer

@HasCustomBlockItemFactory(FlagBlockItem::class)
class FlagBlock : TemplateBlockWithEntity(namespace.id("flag"), Material.WOOD), OnPlaceItemStack {
  init {
    textureId = LOG.textureId
    setTranslationKey(namespace, "flag")
    setHardness(0F)
    setResistance(1.0F)
    setSoundGroup(WOOD_SOUND_GROUP)
    //applyBoundingBox(this)
  }

  companion object {
    fun applyBoundingBox(block: Block) {
      block.setBoundingBox(
        0.5f - 0.0625f, 0.0f, 0.5f - 0.0625f,
        0.5f + 0.0625f, 1.0f, 0.5f + 0.0625f
      )
    }
  }

  override fun createBlockEntity(): BlockEntity = FlagBlockEntity()

  override fun isFullCube(): Boolean = false
  override fun isOpaque(): Boolean = false

  @Environment(EnvType.CLIENT)
  override fun getRenderType(): Int = -1

  override fun getCollisionShape(world: World, x: Int, y: Int, z: Int): Box? = null

  override fun onUse(world: World, x: Int, y: Int, z: Int, player: PlayerEntity): Boolean {
    val entity = world.getBlockEntity(x, y, z)
    if (entity is FlagBlockEntity) {
      if (!world.isRemote) {
        // request to open the screen via a packet; will short-circuit in singleplayer
        FlagNetworkingS2C.createFlagScreenOpenPacket(x, y, z).sendToPlayer(player)
      }

      return true
    }

    return false
  }

  override fun canPlaceAt(world: World, x: Int, y: Int, z: Int): Boolean {
    return true
    return _canPlaceAt(world, x, y, z);
  }
  fun _canPlaceAt(world: World, x: Int, y: Int, z: Int): Boolean {
    if (world.shouldSuffocate(x - 1, y, z)) {
      return true
    } else if (world.shouldSuffocate(x + 1, y, z)) {
      return true
    } else if (world.shouldSuffocate(x, y, z - 1)) {
      return true
    } else if (world.shouldSuffocate(x, y, z + 1)) {
      return true
    } else {
      return false
    }
  }

  //Runs before onPlaced with direction: Int
  override fun onPlaced(world: World, x: Int, y: Int, z: Int) {
    var meta = 0
    //GUESSES which direction it should be, blindly picks any wall to face away from
    if        (world.shouldSuffocate(x - 1, y, z)) {
      meta = 1;
    } else if (world.shouldSuffocate(x + 1, y, z)) {
      meta = 3;
    } else if (world.shouldSuffocate(x, y, z - 1)) {
      meta = 2;
    } else if (world.shouldSuffocate(x, y, z + 1)) {
      meta = 0
    }

    world.setBlockMeta(x, y, z, meta)
    //println("onPlaced: Set rotation $meta")
    super.onPlaced(world, x, y, z) //creates flag block entity
  }
  //Runs last; Final say over placement rules(?)
  override fun onPlaced(world: World?, x: Int, y: Int, z: Int, direction: Int) {
    var rot = world!!.getBlockMeta(x, y, z)
    //CONFIRMS the direction it should face now that we know which blockFace this is being placed on
    when (direction) {
      2 if world.shouldSuffocate(x, y, z + 1) -> { rot = 0; }
      3 if world.shouldSuffocate(x, y, z - 1) -> { rot = 2; }
      4 if world.shouldSuffocate(x + 1, y, z) -> { rot = 3; }
      5 if world.shouldSuffocate(x - 1, y, z) -> { rot = 1; }
      else -> { }
    }

    world?.setBlockMeta(x, y, z, rot)
    this.breakIfCannotPlaceAt(world, x, y, z)
  }

  override fun raycast(world: World, x: Int, y: Int, z: Int, startPos: Vec3d?, endPos: Vec3d?): HitResult? {
    val rot = world.getBlockMeta(x, y, z) and 7
    var thick = (FLAG_THICKNESS + FLAG_WALL_FLOAT).toFloat() //thickness of frame including float-off-wall portion
    var half = FLAG_WALL_FLOAT.toFloat()

    if (rot == 0) { //Faces North
      this.setBoundingBox(
        0.0f, 0.0f, 1.0f-thick,
        1.0f, 1.0f, 1.0f-half)
    } else if (rot == 1) { //Faces East
      this.setBoundingBox(
        half, 0.0f, 0.0f,
        thick, 1.0f, 1.0f)
    } else if (rot == 2) { //Faces South
      this.setBoundingBox(
        0.0f, 0.0f, half,
        1.0f, 1.0f, thick)
    } else if (rot == 3) { //Faces West
      this.setBoundingBox(
        1.0f-thick, 0.0f, 0.0f,
        1.0f-half, 1.0f, 1.0f)
    } else {
      this.setBoundingBox(
        0.0f, 0.0f, 0.0f,
        1.0f, 1.0f, 1.0f)
    }

    return super.raycast(world, x, y, z, startPos, endPos)
  }

  override fun neighborUpdate(world: World, x: Int, y: Int, z: Int, id: Int) {
    //println("neighbor update at $x, $y, $z");
    breakIfCannotPlaceAt(world, x, y, z);
  }
  fun breakIfCannotPlaceAt(world: World, x: Int, y: Int, z: Int) {
    val meta = world.getBlockMeta(x, y, z)

    if (meta == 1 && world.shouldSuffocate(x - 1, y, z)) {
      return
    } else if ( meta == 3 && world.shouldSuffocate(x + 1, y, z)) {
      return
    } else if (meta == 2 && world.shouldSuffocate(x, y, z - 1)) {
      return
    } else if ( meta == 0 && world.shouldSuffocate(x, y, z + 1)) {
      return
    }


    // Double check that we still exist before dropping a stack.
    if (world.getBlockId(x, y, z) == this.id) {
      if (!world.isRemote) {
        dropStacks(world, x, y, z, meta)
      }
      world.setBlock(x, y, z, 0)
    }
  }

  override fun dropStacks(world: World, x: Int, y: Int, z: Int, meta: Int, luck: Float) {
    // Don't drop anything, since we need to set NBT from the BE which is already
    // removed at this point. Our workaround is to instead drop stacks in onBreak.
    return
  }

  override fun onBreak(world: World, x: Int, y: Int, z: Int) {
    if (world.isRemote) return
    val entity = world.getBlockEntity(x, y, z)
    val stack = ItemStack(this)
    if (entity is FlagBlockEntity && entity.isPainted) {
      stack.stationNbt.putByteArray("Pixels", entity.pixels)
    }
    dropStack(world, x, y, z, stack)

    super.onBreak(world, x, y, z)
  }

  override fun getPistonBehavior() = 2 // unpushable

  override fun onPlaced(
    world: World,
    x: Int,
    y: Int,
    z: Int,
    itemStack: ItemStack
  ) {
    val stationNbt = itemStack.stationNbt
    if (stationNbt == null || !stationNbt.contains("Pixels")) return

    val entity = world.getBlockEntity(x, y, z)
    if (entity is FlagBlockEntity) {
      val pixels = stationNbt.getByteArray("Pixels")
      entity.setAllPixels(pixels)
    }
  }
}
