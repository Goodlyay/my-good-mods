package pw.tmpim.goodflags.block

object FlagSpec {
  const val FLAG_WIDTH = 16
  const val FLAG_HEIGHT = 16

  const val FLAG_PALETTE_SIZE = 24
  const val FLAG_PALETTE_WHITE = 16
  const val FLAG_THICKNESS = 0.0625
  const val FLAG_WALL_FLOAT = 0.03125

  const val PREVIEW_DEFAULT_BASERES = 16
  const val PREVIEW_DEFAULT_OFFSETX = 1
  const val PREVIEW_DEFAULT_OFFSETY = 3
  const val PREVIEW_DEFAULT_WIDTH = 14
  const val PREVIEW_DEFAULT_HEIGHT = 12

  val colors = longArrayOf(
    0x3D2F35,
    0x77152E,
    0xC4392D,
    0xE38D31,
    0x255441,
    0x008751,
    0x51DB51,
    0xEAEA33,
    0x3A47A8,
    0x496BB3,
    0x40A0D8,
    0x38EEFF,
    0x212028,
    0x49484C,
    0x7F7F7F,
    0xA7B8C6,
    0xFFFFFF,
    0x593C2E,
    0x845E41,
    0xAF804D,
    0xEFC786,
    0x6735AD,
    0xB147C1,
    0xEF86A4,
  )

  fun getGLColor(i: Int) =
    (0xFF000000 or colors[if (i in 0..<FLAG_PALETTE_SIZE) i else FLAG_PALETTE_WHITE]).toInt()

}
