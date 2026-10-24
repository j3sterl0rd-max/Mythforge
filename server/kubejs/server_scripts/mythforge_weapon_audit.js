// Read-only registry coverage report, produced when a world/server starts.
ServerEvents.loaded(function (event) {
  try {
    var auditHooks = Java.loadClass('mythforge.policy.PackPolicy')
    var registry = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries').ITEM
    var expected = JsonIO.read('kubejs/server_scripts/mythforge_weapon_families.json')
    var report = { scanned: 0, classified: [], unclassified: [], mismatched: [], errors: [] }
    var ids = registry.keySet().iterator()
    while (ids.hasNext()) {
      var key = ids.next()
      var id = String(key)
      report.scanned++
      try {
        var nativeAudit = JSON.parse(String(auditHooks.weaponAuditData(id)))
        var tagNames = nativeAudit.tags
        var primary = tagNames.filter(t => t.indexOf('mythforge:weapons/') === 0 &&
          ['mythforge:weapons/all', 'mythforge:weapons/melee', 'mythforge:weapons/ranged'].indexOf(t) === -1)
        var attributes = nativeAudit.attributes
        var className = nativeAudit.itemClass
        var record = { id: id, itemClass: className, families: primary, attributes: attributes }
        var wanted = expected ? expected[id] : null
        if (wanted) {
          report.classified.push(record)
          if (primary.length !== 1 || primary[0] !== 'mythforge:weapons/' + wanted) report.mismatched.push(record)
        } else {
          var combatTag = tagNames.some(t => /(?:weapons$|tools\/(?:melee_weapon|ranged_weapon)$|enchantable\/weapon$)/.test(t))
          var combatClass = /(?:Sword|Axe|Trident|Mace|Bow|Crossbow|Gun|Spear|Scythe|Dagger|Whip|Gauntlet|Knuckle|Weapon)Item$/.test(className)
          var damage = attributes.some(a => a.attribute === 'minecraft:generic.attack_damage' && a.amount > 0)
          var miningTool = tagNames.some(t => /(?:pickaxes|shovels|hoes|tools\/paxel)$/.test(t))
          if ((combatTag || combatClass || damage) && !miningTool) report.unclassified.push(record)
        }
      } catch (error) { report.errors.push({ id: id, error: String(error) }) }
    }
    JsonIO.write('logs/mythforge-weapon-audit.json', report)
    console.info('[Mythforge weapons] Registry audit: ' + report.classified.length +
      ' classified, ' + report.unclassified.length + ' candidates to review, ' +
      report.mismatched.length + ' tag mismatches, ' + report.errors.length + ' inspection errors.')
  } catch (error) { console.error('[Mythforge weapons] Registry audit failed: ' + error) }
})
