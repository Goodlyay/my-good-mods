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
import pw.tmpim.goodflags.block.FlagSpec.NBT_ARTIST
import pw.tmpim.goodflags.block.FlagSpec.NBT_PIXELS
import pw.tmpim.goodflags.item.FlagBlockItem
import pw.tmpim.goodflags.net.FlagNetworkingS2C
import pw.tmpim.goodutils.block.OnPlaceItemStack
import pw.tmpim.goodutils.net.sendToPlayer
import java.util.*

@HasCustomBlockItemFactory(FlagBlockItem::class)
class FlagBlock : TemplateBlockWithEntity(namespace.id("flag"), Material.WOOD), OnPlaceItemStack {
  init {
    textureId = LOG.textureId
    setTranslationKey(namespace, "flag")
    setHardness(0F)
    setResistance(1.0F)
    setSoundGroup(WOOD_SOUND_GROUP)
    this.setTickRandomly(true)
    //applyBoundingBox(this)
  }

  companion object {
    //Numbered names as a reference to what the literal values are
    const val EAST1 = 1;
    const val WEST2 = 2;
    const val SOUTH3 = 3;
    const val NORTH4 = 4;
    const val FLOOR5 = 5;

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

  private fun canPlaceOn(world: World, x: Int, y: Int, z: Int): Boolean {
    return world.shouldSuffocate(x, y, z) || world.getBlockId(x, y, z) == FENCE.id
  }

  override fun canPlaceAt(world: World, x: Int, y: Int, z: Int): Boolean {
    if (world.shouldSuffocate(x - 1, y, z)) {
      return true
    } else if (world.shouldSuffocate(x + 1, y, z)) {
      return true
    } else if (world.shouldSuffocate(x, y, z - 1)) {
      return true
    } else if (world.shouldSuffocate(x, y, z + 1)) {
      return true
    } else {
      return this.canPlaceOn(world, x, y - 1, z)
    }
  }

  override fun onPlaced(world: World, x: Int, y: Int, z: Int, direction: Int) {
    var meta = world.getBlockMeta(x, y, z)
    if (direction == 2 && world.shouldSuffocate(x, y, z + 1)) {
      meta = NORTH4
    }
    if (direction == 3 && world.shouldSuffocate(x, y, z - 1)) {
      meta = SOUTH3
    }
    if (direction == 4 && world.shouldSuffocate(x + 1, y, z)) {
      meta = WEST2
    }
    if (direction == 5 && world.shouldSuffocate(x - 1, y, z)) {
      meta = EAST1
    }
    world.setBlockMeta(x, y, z, meta)
    println("onPlaced... meta is ${world.getBlockMeta(x, y, z)}") //TEST WITH THIS BYE :)
  }

  override fun onTick(world: World, x: Int, y: Int, z: Int, random: Random?) {
    super.onTick(world, x, y, z, random)
    if (world.getBlockMeta(x, y, z) == 0) {
      this.onPlaced(world, x, y, z)
    }
  }

  override fun onPlaced(world: World, x: Int, y: Int, z: Int) {
    if (world.shouldSuffocate(x - 1, y, z)) {
      world.setBlockMeta(x, y, z, EAST1)
    } else if (world.shouldSuffocate(x + 1, y, z)) {
      world.setBlockMeta(x, y, z, WEST2)
    } else if (world.shouldSuffocate(x, y, z - 1)) {
      world.setBlockMeta(x, y, z, SOUTH3)
    } else if (world.shouldSuffocate(x, y, z + 1)) {
      world.setBlockMeta(x, y, z, NORTH4)
    }
    println("onPlaced... meta is ${world.getBlockMeta(x, y, z)}")

    super.onPlaced(world, x, y, z)
    this.breakIfCannotPlaceAt(world, x, y, z)
  }

  override fun neighborUpdate(world: World, x: Int, y: Int, z: Int, id: Int) {
    if (this.breakIfCannotPlaceAt(world, x, y, z)) {
      val meta = world.getBlockMeta(x, y, z)
      var DIE = false
      if (!world.shouldSuffocate(x - 1, y, z) && meta == EAST1) {
        DIE = true
      }
      if (!world.shouldSuffocate(x + 1, y, z) && meta == WEST2) {
        DIE = true
      }
      if (!world.shouldSuffocate(x, y, z - 1) && meta == SOUTH3) {
        DIE = true
      }
      if (!world.shouldSuffocate(x, y, z + 1) && meta == NORTH4) {
        DIE = true
      }
      if (!this.canPlaceOn(world, x, y - 1, z) && meta == 5) {
        DIE = true
      }
      if (DIE) {
        //this.dropStacks(world, x, y, z, world.getBlockMeta(x, y, z))
        world.setBlock(x, y, z, 0)
      }
    }
  }

  private fun breakIfCannotPlaceAt(world: World, x: Int, y: Int, z: Int): Boolean {
    if (!this.canPlaceAt(world, x, y, z)) {
      //this.dropStacks(world, x, y, z, world.getBlockMeta(x, y, z))
      world.setBlock(x, y, z, 0)
      return false
    } else {
      return true
    }
  }
  override fun raycast(world: World, x: Int, y: Int, z: Int, startPos: Vec3d?, endPos: Vec3d?): HitResult? {
    val rot = world.getBlockMeta(x, y, z) and 7
    var thick = (FLAG_THICKNESS + FLAG_WALL_FLOAT).toFloat() //thickness of frame including float-off-wall portion
    var half = FLAG_WALL_FLOAT.toFloat()

    if (rot == NORTH4) { //Faces North
      this.setBoundingBox(
        0.0f, 0.0f, 1.0f-thick,
        1.0f, 1.0f, 1.0f-half)
    } else if (rot == EAST1) { //Faces East
      this.setBoundingBox(
        half, 0.0f, 0.0f,
        thick, 1.0f, 1.0f)
    } else if (rot == SOUTH3) { //Faces South
      this.setBoundingBox(
        0.0f, 0.0f, half,
        1.0f, 1.0f, thick)
    } else if (rot == WEST2) { //Faces West
      this.setBoundingBox(
        1.0f-thick, 0.0f, 0.0f,
        1.0f-half, 1.0f, 1.0f)
    } else {
      this.setBoundingBox(
        0.0f, 0.0f, 0.0f,
        1.0f, 1.0f, 1.0f
      )
    }
    return super.raycast(world, x, y, z, startPos, endPos)
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
    if (entity == null) {
      println("dropSelf, but entity is NULL!")
    }
    if (entity is FlagBlockEntity && entity.isPainted) {
      stack.stationNbt.putByteArray(NBT_PIXELS, entity.pixels)
      stack.stationNbt.putString(NBT_ARTIST, entity.artist)
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
    if (stationNbt != null && stationNbt.contains("Pixels")) {
      println("onPlaced set data one")
      val entity = world.getBlockEntity(x, y, z)
      if (entity is FlagBlockEntity) {
        val pixels = stationNbt.getByteArray(NBT_PIXELS)
        val artist = stationNbt.getString(NBT_ARTIST)
        entity.setData(artist, pixels)
      }
    }

    //if (!world.isRemote) {
    //  println("onPlaced ItemStack: about to break if cannot place")
    //  this.breakIfCannotPlaceAt(world, x, y, z)
    //}
  }
}
