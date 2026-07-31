package pw.tmpim.goodflags.item
import net.modificationstation.stationapi.api.template.item.TemplateItem
import pw.tmpim.goodflags.GoodFlags.namespace

class PaletteItem : TemplateItem(namespace.id("palette")) {
  init {
    setTranslationKey(namespace, "palette")
  }
}
