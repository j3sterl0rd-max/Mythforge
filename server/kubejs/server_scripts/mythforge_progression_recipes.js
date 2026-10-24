// Mythforge progression and rank-only chunk loading, NeoForge 1.21.1 / KubeJS 7.
const MF_POLICY_DISABLED_ITEMS = [
  "ae2:spatial_anchor",
  "mekanism:dimensional_stabilizer",
  "mekanism:upgrade_anchor",
  "pneumaticcraft:chunkloader_upgrade",
  "advancedperipherals:chunk_controller",
  "alexscaves:nuclear_bomb",
  "industrialforegoing:infinity_nuke"
]
ServerEvents.recipes(event => {
  for (var id of MF_POLICY_DISABLED_ITEMS) if (Item.exists(id)) event.remove({ output: id })
  // Native repair recipes still require the already-crafted compass.
  event.remove({ id: 'naturescompass:natures_compass' })
  event.remove({ id: 'explorerscompass:explorers_compass' })
  event.shaped('naturescompass:naturescompass', ['LEL', 'SCS', 'LEL'], {
    L: '#minecraft:logs', E: 'minecraft:echo_shard', S: '#minecraft:saplings', C: 'minecraft:compass'
  }).id('mythforge:natures_compass')
  event.shaped('explorerscompass:explorerscompass', ['IEI', 'DCD', 'IEI'], {
    I: '#c:ingots/iron', E: 'minecraft:echo_shard', D: '#c:gems/diamond', C: 'minecraft:compass'
  }).id('mythforge:explorers_compass')
})
for (const id of MF_POLICY_DISABLED_ITEMS) ItemEvents.rightClicked(id, event => event.cancel())
BlockEvents.rightClicked(event => {
  if (MF_POLICY_DISABLED_ITEMS.indexOf(String(event.item.id)) !== -1 ||
      MF_POLICY_DISABLED_ITEMS.indexOf(String(event.block.id)) !== -1) event.cancel()
})
BlockEvents.placed(event => {
  if (MF_POLICY_DISABLED_ITEMS.indexOf(String(event.block.id)) !== -1) event.cancel()
})
