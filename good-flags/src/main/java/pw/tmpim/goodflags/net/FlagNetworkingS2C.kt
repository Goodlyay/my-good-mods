package pw.tmpim.goodflags.net

import net.glasslauncher.mods.networking.GlassPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.network.ClientNetworkHandler
import pw.tmpim.goodflags.GoodFlags.namespace
import pw.tmpim.goodflags.block.FlagBlockEntity
import pw.tmpim.goodflags.block.FlagSpec.FLAG_HEIGHT
import pw.tmpim.goodflags.block.FlagSpec.FLAG_WIDTH
import pw.tmpim.goodflags.block.FlagSpec.NBT_ARTIST
import pw.tmpim.goodflags.block.FlagSpec.NBT_PIXELS
import pw.tmpim.goodflags.client.FlagPaintScreen
import pw.tmpim.goodutils.net.GlassPacket


object FlagNetworkingS2C {
  /** Server -> Client: open flag GUI */
  val FLAG_SCREEN_OPEN_ID = namespace.id("flag_screen_open")
  /** Server -> Client: sync flag pixel data on chunk load */
  val FLAG_SYNC_ID = namespace.id("flag_sync")

  /**
   * Server -> client: request the client opens the flag screen for a given flag.
   * Called by FlagBlock.onUse().
   */
  fun createFlagScreenOpenPacket(x: Int, y: Int, z: Int) =
    GlassPacket(FLAG_SCREEN_OPEN_ID) {
      putInt("x", x)
      putInt("y", y)
      putInt("z", z)
    }

  /**
   * Create a sync packet for server -> client block entity updates.
   * Called by FlagBlockEntity.createUpdatePacket()
   */
  fun createSyncPacket(x: Int, y: Int, z: Int, pixels: ByteArray, artist : String): GlassPacket =
    GlassPacket(FLAG_SYNC_ID) {
      putInt("x", x)
      putInt("y", y)
      putInt("z", z)
      putByteArray(NBT_PIXELS, pixels.copyOf())
      putString(NBT_ARTIST, artist)
    }.apply {
      // ensure they are sent *after* chunk data (delayedSendQueue) so that the block entities exist on the client
      worldPacket = true
    }

  /**
   * Handle a flag screen open packet (received on the client when they right-click a flag).
   */
  fun onFlagScreenOpen(packet: GlassPacket, handler: ClientNetworkHandler?) {
    val minecraft = handler?.minecraft ?: Minecraft.INSTANCE
    val world = handler?.world ?: minecraft.world
    val nbt = packet.nbt

    val x = nbt.getInt("x")
    val y = nbt.getInt("y")
    val z = nbt.getInt("z")

    val entity = world.getBlockEntity(x, y, z)
    if (entity !is FlagBlockEntity) return

    val player = minecraft.player;

    player.sendMessage("\u00a77[Accessing canvas by: ${entity.artist}]")

    if (entity.isPainted && entity.artist.length > 0 && entity.artist != player.name) {
      player.sendMessage("\u00a77[Painted by ${entity.artist}]")
      return
    }
    entity.artist = player.name
    println("set artist name to ${player.name}")
    //Set artist clientside... it will sync with the server when a canvas is submitted

    minecraft.setScreen(FlagPaintScreen(entity))
  }

  /**
   * Handle a flag sync packet (received on the client from the server on chunk load).
   */
  fun onFlagSync(packet: GlassPacket, handler: ClientNetworkHandler?) {
    val world = handler?.world ?: Minecraft.INSTANCE.world
    val nbt = packet.nbt

    val x = nbt.getInt("x")
    val y = nbt.getInt("y")
    val z = nbt.getInt("z")
    val bytes = nbt.getByteArray(NBT_PIXELS)
    val artist = nbt.getString(NBT_ARTIST)

    if (bytes.size != FLAG_WIDTH * FLAG_HEIGHT) return

    val entity = world.getBlockEntity(x, y, z)
    if (entity !is FlagBlockEntity) return

    entity.setData(artist, bytes)
  }
}
