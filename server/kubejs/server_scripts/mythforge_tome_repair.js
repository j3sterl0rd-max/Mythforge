// Repair missing identity components in existing and newly crafted Akashic Tomes.
// Preserve real/custom books; remove only empty Patchouli/GuideME templates.
const MF_TomeRepairRegistries = Java.loadClass('vazkii.akashictome.Registries')
const MF_TomeRepairContent = Java.loadClass('vazkii.akashictome.data_components.ToolContentComponent')
const MF_TomeRepairArrayList = Java.loadClass('java.util.ArrayList')
const MF_TomeRepairRegistry = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
const MF_TomeRepairLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')
const MF_TomeRepairComponent = Java.loadClass('net.minecraft.network.chat.Component')
const MF_TomeRepairVanillaComponents = Java.loadClass('net.minecraft.core.component.DataComponents')
const MF_TomeRepairGuideMe = Java.loadClass('guideme.internal.GuideME')
const MF_TomeRepairJsonOps = Java.loadClass('com.mojang.serialization.JsonOps')
const MF_TomeRepairNames = {"advancedperipherals:manual": "Advanced Peripherals Manual", "betterend:guidebook": "BetterEnd Guidebook", "buildinggadgets2:buildinggadgets2book": "Gadgets for Derps", "cyclic:guide_book": "Cyclic Guidebook", "enderio:guide": "Ender IO Guide", "grimoireofgaia:gaiapedia": "Gaiapedia", "industrialforegoing:industrial_foregoing": "Industrial Foregoing Manual", "irons_spellbooks:iss_guide_book": "Iron's Guidebook", "justdirethings:justdirethingsbook": "Just Dires Guide", "minecells:minecells_guidebook": "Mine Cells Guide", "pneumaticcraft:book": "PneumaticCraft Manual", "yuushya:yuushya_guidebook": "Yuushya Guidebook"}

function mfRepairTome(stack, registries) {
  var contentType = MF_TomeRepairRegistries.TOOL_CONTENT.get()
  var content = stack.get(contentType)
  if (content == null) return
  var definitionType = MF_TomeRepairRegistries.DEFINED_MOD.get()
  var patchouliType = MF_TomeRepairRegistry.DATA_COMPONENT_TYPE.get(MF_TomeRepairLocation.parse('patchouli:book'))
  var spectrumType = MF_TomeRepairRegistry.DATA_COMPONENT_TYPE.get(MF_TomeRepairLocation.parse('modonomicon:book_id'))
  var guideType = MF_TomeRepairGuideMe.GUIDE_ID_COMPONENT
  var repaired = new MF_TomeRepairArrayList()
  var changed = false
  for (var original of content.getItems()) {
    var book = original.copy()
    var id = String(MF_TomeRepairRegistry.ITEM.getKey(book.getItem()))
    var patchouliId = patchouliType == null ? null : book.get(patchouliType)
    var guideId = guideType == null ? null : book.get(guideType)
    if ((id === 'patchouli:guide_book' && patchouliId == null) ||
        (id === 'guideme:guide' && guideId == null)) {
      changed = true
      continue
    }
    var namespace = patchouliId != null ? String(patchouliId).split(':')[0] :
      guideId != null ? String(guideId).split(':')[0] : id.split(':')[0]
    if (!book.has(definitionType)) {
      book.set(definitionType, namespace)
      changed = true
    }
    if (id === 'spectrum:guidebook' && spectrumType != null && !book.has(spectrumType)) {
      book.set(spectrumType, 'spectrum:guidebook')
      changed = true
    }
    if (patchouliId != null && !book.has(MF_TomeRepairVanillaComponents.CUSTOM_NAME)) {
      var name = MF_TomeRepairNames[String(patchouliId)]
      if (name) {
        book.setCustomName(MF_TomeRepairComponent.literal(name))
        changed = true
      }
    }
    repaired.add(book)
  }
  // KubeJS 7's set() expects codec-compatible JSON for custom components.
  // Encode the complete native value first, preserving all book components.
  if (changed) {
    var ops = registries.createSerializationContext(MF_TomeRepairJsonOps.INSTANCE)
    var encoded = MF_TomeRepairContent.CODEC.encodeStart(ops, new MF_TomeRepairContent(repaired)).getOrThrow()
    stack.set(contentType, encoded)
  }
}

PlayerEvents.loggedIn(event => {
  var inventory = event.player.getInventory()
  for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
    mfRepairTome(inventory.getItem(slot), event.player.registryAccess())
  }
})

ItemEvents.crafted(event => mfRepairTome(event.item, event.player.registryAccess()))
