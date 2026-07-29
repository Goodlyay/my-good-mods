package pw.tmpim.goodflags.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.WoolBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.platform.Lighting
import net.minecraft.util.math.MathHelper
import net.modificationstation.stationapi.api.client.StationRenderAPI
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector3f
import pw.tmpim.goodflags.block.FlagBlockEntity
import pw.tmpim.goodflags.block.FlagSpec.FLAG_HEIGHT
import pw.tmpim.goodflags.block.FlagSpec.FLAG_WIDTH
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Environment(EnvType.CLIENT)
class FlagBlockEntityRenderer : BlockEntityRenderer() {
  override fun render(entity: BlockEntity, dx: Double, dy: Double, dz: Double, tickDelta: Float) {
    if (entity !is FlagBlockEntity) return

    val meta = entity.world?.getBlockMeta(entity.x, entity.y, entity.z) ?: 0
    // meta: 0=south, 1=west, 2=north, 3=east (player facing direction when placed)
    val rotation = when (meta) {
      0 -> 0.0F    // south
      1 -> 90.0F   // west
      2 -> 180.0F  // north
      3 -> 270.0F  // east
      else -> 0.0F
    }

    val light = dispatcher.world.getNaturalBrightness(entity.x, entity.y, entity.z, 0)
    val xLight = when (meta) {
      0 -> light * 0.8f
      1 -> light * 0.6f
      2 -> light * 0.8f
      3 -> light * 0.6f
      else -> 0.4f
    }
    val zLight = when (meta) {
      0 -> light * 0.6f
      1 -> light * 0.8f
      2 -> light * 0.6f
      3 -> light * 0.8f
      else -> 0.4f
    }
    val yLight = light * 0.4f

    Lighting.turnOff()
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_CULL_FACE)
    if (Minecraft.isAmbientOcclusionEnabled()) {
      GL11.glShadeModel(GL11.GL_SMOOTH)
    } else {
      GL11.glShadeModel(GL11.GL_FLAT)
    }

    GL11.glPushMatrix()
    GL11.glTranslated(dx + 0.5, dy, dz + 0.5)
    GL11.glRotatef(-rotation, 0.0F, 1.0F, 0.0F)

    drawPole(light, xLight, zLight, yLight)
    drawFlag(entity, light, xLight, zLight, yLight)

    GL11.glPopMatrix()

    Lighting.turnOn()
  }

  private fun drawPole(light: Float, xLight : Float, zLight : Float, yLight : Float) {
    val flagWidth  = 1.0
    val flagHeight = 1.0
    val flagTop    = 1.0
    val flagBottom = flagTop - flagHeight
    val flagRight  = 0.5
    val flagLeft   = flagRight - flagWidth
    val flagThickness = 0.0625
    val flagZFront    = 0.5 - 0.03125
    val flagZBack     = flagZFront - flagThickness

    val gameAtlas = StationRenderAPI.getBakedModelManager().getAtlas(Atlases.GAME_ATLAS_TEXTURE)
    gameAtlas.bindTexture()

    val atlas = Atlases.getTerrain()
    val woodBlockSideId = Block.PLANKS.getTexture(2)
    val woodBlockSideSprite = atlas.getTexture(woodBlockSideId)

    val uSize = woodBlockSideSprite.endU - woodBlockSideSprite.startU
    val vSize = woodBlockSideSprite.endV - woodBlockSideSprite.startV
    val uSideStart = woodBlockSideSprite.startU
    val uSideEnd   = woodBlockSideSprite.startU + uSize / 16
    val vSideStart = woodBlockSideSprite.startV
    val vSideEnd   = woodBlockSideSprite.endV
    val uEndsStart = woodBlockSideSprite.startU
    val uEndsEnd   = woodBlockSideSprite.startU + uSize / 16
    val vEndsStart = woodBlockSideSprite.startV
    val vEndsEnd   = woodBlockSideSprite.startV + vSize / 16

    val yBottom = 0.0
    val yTop    = yBottom + 1.0

    val t = Tessellator.INSTANCE
    GL11.glColor4f(
      xLight,
      xLight,
      xLight, 1.0F)
    t.startQuads()

    // North face (negative Z)
    t.vertex(flagLeft,  flagTop,    flagZFront, woodBlockSideSprite.startU, woodBlockSideSprite.startV) //top left
    t.vertex(flagLeft,  flagBottom, flagZFront, woodBlockSideSprite.startU, woodBlockSideSprite.endV) //bottom left
    t.vertex(flagRight, flagBottom, flagZFront, woodBlockSideSprite.endU,   woodBlockSideSprite.endV) //bottom right
    t.vertex(flagRight, flagTop,    flagZFront, woodBlockSideSprite.endU,   woodBlockSideSprite.startV) //top right
    t.draw()
    GL11.glColor4f(
      zLight,
      zLight,
      zLight, 1.0F)
    t.startQuads()
    // Left face (negative X)
    t.vertex(flagRight, flagTop,    flagZFront, uSideStart + 2*uSize / 16, vSideStart)
    t.vertex(flagRight, flagBottom, flagZFront, uSideStart + 2*uSize / 16, vSideEnd)
    t.vertex(flagRight, flagBottom, flagZBack,  uSideEnd + 2*uSize / 16,   vSideEnd)
    t.vertex(flagRight, flagTop,    flagZBack,  uSideEnd + 2*uSize / 16,   vSideStart)
    // Right face (positive X)
    t.vertex(flagLeft, flagTop,    flagZFront, uSideStart + 3*uSize / 16, vSideStart)
    t.vertex(flagLeft, flagBottom, flagZFront, uSideStart + 3*uSize / 16, vSideEnd)
    t.vertex(flagLeft, flagBottom, flagZBack,  uSideEnd + 3*uSize / 16,   vSideEnd)
    t.vertex(flagLeft, flagTop,    flagZBack,  uSideEnd + 3*uSize / 16,   vSideStart)

    t.draw()
    GL11.glColor4f(
      light,
      light,
      light, 1.0F)
    t.startQuads()
    // Top face
    t.vertex(flagLeft,  flagTop, flagZBack,  woodBlockSideSprite.startU, woodBlockSideSprite.startV) //top left
    t.vertex(flagLeft,  flagTop, flagZFront, woodBlockSideSprite.startU, woodBlockSideSprite.startV + vSize / 16) //bottom left
    t.vertex(flagRight, flagTop, flagZFront, woodBlockSideSprite.endU,   woodBlockSideSprite.startV + vSize / 16) //bottom right
    t.vertex(flagRight, flagTop, flagZBack,  woodBlockSideSprite.endU,   woodBlockSideSprite.startV) //top right

    t.draw()
    GL11.glColor4f(
      yLight,
      yLight,
      yLight, 1.0F)
    t.startQuads()
    // 🥺
    t.vertex(flagLeft,  flagBottom, flagZFront, woodBlockSideSprite.startU, woodBlockSideSprite.startV) //top left
    t.vertex(flagLeft,  flagBottom, flagZBack,  woodBlockSideSprite.startU, woodBlockSideSprite.startV + vSize / 16) //bottom left
    t.vertex(flagRight, flagBottom, flagZBack,  woodBlockSideSprite.endU,   woodBlockSideSprite.startV + vSize / 16) //bottom right
    t.vertex(flagRight, flagBottom, flagZFront, woodBlockSideSprite.endU,   woodBlockSideSprite.startV) //top right

    t.draw()
  }

  private fun drawFlag(entity: FlagBlockEntity, light: Float, xLight : Float, zLight : Float, yLight : Float) {
    // Flag extends from the pole to the right, at the top of the pole (3:2 ratio)
    val flagWidth  = 1.0
    val flagHeight = 1.0
    val flagTop    = 1.0
    val flagBottom = flagTop - flagHeight
    val flagRight  = 0.5
    val flagLeft   = flagRight - flagWidth
    val flagThickness = 0.0625
    val flagZFront    = 0.5 - 0.03125
    val flagZBack     = flagZFront - flagThickness

    // UV edge fractions for the edge strips
    val uMin = 0.0
    val uMax = 1.0
    val vMin = 0.0
    val vMax = 1.0
    val vTopRow    = 1.0 / FLAG_HEIGHT       // one texel row from top
    val vBottomRow = 1.0 - 1.0 / FLAG_HEIGHT // one texel row from bottom
    val uRightCol  = 1.0 - 1.0 / FLAG_WIDTH  // one texel column from right

    // Get or create the GL texture
    val textureId = getOrCreateTexture(entity)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

    GL11.glColor4f(
      xLight,
      xLight,
      xLight, 1.0F)

    val t = Tessellator.INSTANCE
    t.startQuads()

    // Front face of the flag (positive Z)
    //t.vertex(flagLeft,  flagTop,    flagZFront, uMax, vMin)
    //t.vertex(flagLeft,  flagBottom, flagZFront, uMax, vMax)
    //t.vertex(flagRight, flagBottom, flagZFront, uMin, vMax)
    //t.vertex(flagRight, flagTop,    flagZFront, uMin, vMin)
    // South face of the flag (negative Z, reversed winding, mirrored U)
    t.vertex(flagRight, flagTop,    flagZBack, uMin, vMin)
    t.vertex(flagRight, flagBottom, flagZBack, uMin, vMax)
    t.vertex(flagLeft,  flagBottom, flagZBack, uMax, vMax)
    t.vertex(flagLeft,  flagTop,    flagZBack, uMax, vMin)

    t.draw()
    //GL11.glColor4f(
    //  light,
    //  light,
    //  light, 1.0F)
    //t.startQuads()
    //// Top edge – uses the top row of the texture (vMin..vTopRow)
    //t.vertex(flagLeft,  flagTop, flagZBack,  uMax, vMin)
    //t.vertex(flagLeft,  flagTop, flagZFront, uMax, vTopRow)
    //t.vertex(flagRight, flagTop, flagZFront, uMin, vTopRow)
    //t.vertex(flagRight, flagTop, flagZBack,  uMin, vMin)
//
    //t.draw()
    //GL11.glColor4f(
    //  yLight,
    //  yLight,
    //  yLight, 1.0F)
    //t.startQuads()
    //// Bottom edge – uses the bottom row of the texture (vBottomRow..vMax)
    //t.vertex(flagLeft,  flagBottom, flagZFront, uMax, vBottomRow)
    //t.vertex(flagLeft,  flagBottom, flagZBack,  uMax, vMax)
    //t.vertex(flagRight, flagBottom, flagZBack,  uMin, vMax)
    //t.vertex(flagRight, flagBottom, flagZFront, uMin, vBottomRow)
//
    //t.draw()
    //GL11.glColor4f(
    //  zLight,
    //  zLight,
    //  zLight, 1.0F)
    //t.startQuads()
    //// Right (free) edge – uses the rightmost column (uMax..uRightCol)
    //t.vertex(flagLeft, flagTop,    flagZFront, uMax,      vMin)
    //t.vertex(flagLeft, flagBottom, flagZFront, uMax,      vMax)
    //t.vertex(flagLeft, flagBottom, flagZBack,  uRightCol, vMax)
    //t.vertex(flagLeft, flagTop,    flagZBack,  uRightCol, vMin)
    //
    //t.draw()
  }

  private fun getOrCreateTexture(entity: FlagBlockEntity): Int {
    val key = System.identityHashCode(entity)

    if (!entity.dirty && textureCache.containsKey(key)) {
      return textureCache[key]!!
    }

    // Delete old texture if it exists
    textureCache[key]?.let { oldId ->
      GL11.glDeleteTextures(oldId)
    }

    // Generate a new texture from pixel data
    val texId = GL11.glGenTextures()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId)

    // Create pixel buffer
    val buffer = ByteBuffer.allocateDirect(FLAG_WIDTH * FLAG_HEIGHT * 4)
      .order(ByteOrder.nativeOrder())

    // Sample wool texture from Station's Arsenic sprites
    val atlas = Atlases.getTerrain()
    val woolTextures = (0..<16).map { dyeIndex ->
      val woolBlockMeta = WoolBlock.getBlockMeta(dyeIndex)
      val woolTexture = Block.WOOL.getTexture(0, woolBlockMeta)
      atlas.getTexture(woolTexture).sprite.contents
    }

    @Suppress("UnstableApiUsage")
    for (y in 0 until FLAG_HEIGHT) {
      for (x in 0 until FLAG_WIDTH) {
        val colorIndex = entity.getPixel(x, y)
        val dyeIndex = colorIndex and 0xF
        val woolTex = woolTextures[dyeIndex] ?: woolTextures[0] // fallback to white

        val texturedWoolCol = unpackedColor(woolTex.baseFrame.getColor(x % woolTex.width, y % woolTex.height))
        //Pick an average-ish color on the wool texture at 0,3
        val plainWoolCol = unpackedColor(woolTex.baseFrame.getColor(0, 3))
        lerp(texturedWoolCol, texturedWoolCol, plainWoolCol, 0.5f)

        //multiplyVector(plainWoolCol, texturedWoolCol)

        buffer.putInt(packedColor(texturedWoolCol))
      }
    }
    buffer.flip()

    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)
    GL11.glTexImage2D(
      GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
      FLAG_WIDTH, FLAG_HEIGHT, 0,
      GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer
    )

    textureCache[key] = texId
    entity.dirty = false

    return texId
  }

  companion object {
    /** Cache of GL texture IDs keyed by block entity identity hash. */
    private val textureCache = HashMap<Int, Int>()

    fun clearTextureCache() {
      textureCache.clear()
    }

    public fun unpackedColor(packed : Int) : Vector3f {
      return Vector3f(
        (packed and 0xFF) / 255.0f,
        ((packed shr 8) and 0xFF) / 255.0f,
        ((packed shr 16) and 0xFF) / 255.0f
      )
    }
    public fun packedColor(color : Vector3f) : Int {
      color.x = Math.clamp(color.x, 0.0f, 1.0f)
      color.y = Math.clamp(color.y, 0.0f, 1.0f)
      color.z = Math.clamp(color.z, 0.0f, 1.0f)

      val r = (MathHelper.floor(color.x * 255.0f)) and 0xFF
      val g = (MathHelper.floor(color.y * 255.0f)) and 0xFF
      val b = (MathHelper.floor(color.z * 255.0f)) and 0xFF
      return r or (g shl 8) or (b shl 16) or (0xFF shl 24)
    }
    public fun lerp(target : Vector3f, start : Vector3f, goal : Vector3f, t : Float) {
      val xDiff = goal.x - start.x; val x = start.x + (xDiff * t);
      val yDiff = goal.y - start.y; val y = start.y + (yDiff * t);
      val zDiff = goal.z - start.z; val z = start.z + (zDiff * t);
      target.x = x; target.y = y; target.z = z
    }
    public fun multiplyVector(a : Vector3f, b: Vector3f) {
      a.x *= b.x; a.y *= b.y; a.z *= b.z
    }
  }
}
