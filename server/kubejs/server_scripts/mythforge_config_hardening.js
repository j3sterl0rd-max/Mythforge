// Mythforge non-mod hardening. Minecraft 1.21.1 / KubeJS 7.
// Right-click guards are mitigations, not protection against arbitrary commands,
// imported schematics, automation, or Java-level block/entity code.
const MF_DISABLED_SURVIVAL_ITEMS = [
  'create:schematicannon',
  'ae2:matter_cannon',
  'simulated:docking_connector',
  'cataclysm:tidal_claws',
  'soulsweapons:withered_wabbajack',
  'inventorypets:pet_illuminati',
  'travelersbackpack:white_sleeping_bag',
  'travelersbackpack:orange_sleeping_bag',
  'travelersbackpack:magenta_sleeping_bag',
  'travelersbackpack:light_blue_sleeping_bag',
  'travelersbackpack:yellow_sleeping_bag',
  'travelersbackpack:lime_sleeping_bag',
  'travelersbackpack:pink_sleeping_bag',
  'travelersbackpack:gray_sleeping_bag',
  'travelersbackpack:light_gray_sleeping_bag',
  'travelersbackpack:cyan_sleeping_bag',
  'travelersbackpack:purple_sleeping_bag',
  'travelersbackpack:blue_sleeping_bag',
  'travelersbackpack:brown_sleeping_bag',
  'travelersbackpack:green_sleeping_bag',
  'travelersbackpack:red_sleeping_bag',
  'travelersbackpack:black_sleeping_bag'
]

ServerEvents.recipes(event => {
  for (const id of MF_DISABLED_SURVIVAL_ITEMS) event.remove({ output: id })
})

for (const id of MF_DISABLED_SURVIVAL_ITEMS) {
  ItemEvents.rightClicked(id, event => event.cancel())
}

// Prevent manual placement/use of existing docking connectors and sleeping bags.
BlockEvents.rightClicked(event => {
  if (MF_DISABLED_SURVIVAL_ITEMS.indexOf(event.item.id) !== -1) event.cancel()
})

// Reject Create contraption items before manual Capsid insertion, without
// resetting the Capsid or deleting its contents. Automated insertion needs Java.
BlockEvents.rightClicked('alexsmobs:capsid', event => {
  if (event.item.id === 'create:minecart_contraption' ||
      event.item.id === 'create:chest_minecart_contraption' ||
      event.item.id === 'create:furnace_minecart_contraption') event.cancel()
})
