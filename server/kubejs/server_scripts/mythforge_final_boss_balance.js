// Mythforge - Final Boss Balance Patch
// Target: mythforge-last-horizon-1.4.3 / Minecraft 1.21.1 / NeoForge / KubeJS 7
// v2: safe with nonstandard modded DamageSource implementations (e.g. cosmic_magic).
//
// Goals:
// - Preserve Last Horizon's native 3-phase health and multiplayer scaling.
// - Stop extreme modded burst damage from deleting a phase in one or two hits.
// - Give Avar meaningful physical + spell defense without creating a giant HP sponge.
// - Make clean Avar hits still matter against very strong late-game armor.
//
// Easy tuning knobs are grouped directly below.

const MF_AVAR = 'lasthorizon:avar'

// Defensive tuning. Avar's jar defaults are 16 armor / 8 toughness.
const MF_AVAR_ARMOR = 24
const MF_AVAR_TOUGHNESS = 12

// Iron's Spells 'n Spellbooks uses 1.0 as the neutral spell_resist baseline.
// 1.15 is roughly +15% general Iron's spell resistance before the pack-wide
// magic resistance below is applied.
const MF_AVAR_IRONS_SPELL_RESIST = 1.15

// Final damage Avar may lose from ONE successful damage event, after normal
// armor / toughness / enchantment / effect reductions have already happened.
// 60 HP = 30 hearts. With 900 HP per phase this guarantees at least 15 capped
// hits per solo phase, but ordinary hits are still governed by normal defenses.
const MF_AVAR_DAMAGE_CAP = 60.0

// Extra resistance for magic-system damage across the pack.
// 0.75 = Avar takes 75% of the post-mitigation damage (25% extra resistance).
const MF_AVAR_GENERIC_MAGIC_MULTIPLIER = 0.75

// Avar's native outgoing damage multiplier is now set in config/lasthorizon-common.toml.
// This avoids touching every LivingIncomingDamageEvent in the whole pack.

// After player armor/enchantments/effects, a landed attack from Avar cannot be
// reduced below these rules. Absorption hearts still soak this damage normally.
// Normal attacks: at least 4 HP, scales to 7% max HP, capped at 7 HP.
// Heavy attacks:  at least 7 HP, scales to 10% max HP, capped at 11 HP.
const MF_AVAR_NORMAL_FLOOR_MIN = 4.0
const MF_AVAR_NORMAL_FLOOR_MAX = 7.0
const MF_AVAR_NORMAL_FLOOR_PERCENT = 0.07
const MF_AVAR_HEAVY_FLOOR_MIN = 7.0
const MF_AVAR_HEAVY_FLOOR_MAX = 11.0
const MF_AVAR_HEAVY_FLOOR_PERCENT = 0.10

// Avar's jar uses 22-23 raw damage for its clearly heavy impacts; ordinary
// strikes / cuts are generally below this threshold.
const MF_AVAR_HEAVY_RAW_THRESHOLD = 20.0

function mfEntityId(entity) {
  if (!entity) return ''
  try {
    // Normalize KubeJS's exposed type identifier directly; re-querying the raw
    // registry with the JavaScript-facing type value returned null in runtime tests.
    return String(entity.type)
  } catch (e) {
    try { return String(entity.getType()) } catch (ignored) { return '' }
  }
}

// Some modded DamageSource implementations exposed through Rhino do not make
// getEntity() callable even though vanilla DamageSource does. Never let that
// crash unrelated combat. Avar's native config now handles its 1.40x damage;
// this helper is only used for the optional post-mitigation damage floor.
function mfSourceEntity(source) {
  if (!source) return null
  try { return source.getEntity() } catch (e) {}
  try { if (source.entity) return source.entity } catch (e) {}
  try { return source.getDirectEntity() } catch (e) {}
  try { if (source.directEntity) return source.directEntity } catch (e) {}
  return null
}

function mfDamageTypeId(entity, source) {
  if (!entity || !source) return ''
  try {
    var key = source.typeHolder().unwrapKey()
    if (key.isPresent()) return String(key.get().location())
  } catch (e) {
    // Fall through to the message ID. This keeps the patch compatible with
    // unusual modded DamageSource implementations.
  }

  try {
    return String(source.getMsgId())
  } catch (e) {
    return String(source)
  }
}

function mfDamageMessageId(source) {
  if (!source) return ''
  try { return String(source.getMsgId()) } catch (ignored) { return '' }
}

// Namespaces / identifiers used by the magic systems present in Mythforge.
// The generic keywords catch custom damage types from addons without needing
// a hard dependency on every individual spell mod.
const MF_MAGIC_MARKERS = [
  'irons_spellbooks',
  'ars_nouveau',
  'ars_elemental',
  'ars_elemancy',
  'ars_n_spells',
  'ars_technica',
  'spell_engine',
  'spell_power',
  'wizards',
  'elemental_wizards_rpg',
  'lne_wizards',
  'cataclysm_spellbooks',
  'magic',
  'spell',
  'arcane',
  'eldritch'
]

function mfIsMagicDamage(entity, source) {
  var probe = (
    mfDamageTypeId(entity, source) + ' ' +
    mfDamageMessageId(source) + ' ' +
    String(source)
  ).toLowerCase()

  for (var marker of MF_MAGIC_MARKERS) {
    if (probe.includes(marker)) return true
  }
  return false
}

function mfIsEmergencyAdminDamage(entity, source) {
  var id = mfDamageTypeId(entity, source).toLowerCase()
  var msg = mfDamageMessageId(source).toLowerCase().replace(/[^a-z0-9]/g, '')
  // Keep /kill and out-of-world cleanup usable for admins/debugging.
  return id.includes('generic_kill') || id.includes('out_of_world') || msg.includes('generickill') || msg.includes('outofworld')
}

// Apply the attribute portion repeatedly until each newly-created Avar is tagged.
// This also upgrades a boss that is already alive when the patch is installed.
let mfAvarAttributeTicker = 0
ServerEvents.tick(event => {
  mfAvarAttributeTicker++
  if (mfAvarAttributeTicker < 40) return // every 2 seconds
  mfAvarAttributeTicker = 0

  // A server command source starts in the Overworld. Explicitly visit every
  // loaded dimension so the Still Horizon arena and existing bosses are covered.
  for (var level of event.server.getAllLevels()) {
    var boss = `execute in ${String(level.dimension)} as @e[type=${MF_AVAR},tag=!mf_final_boss_tuned] run `
    event.server.runCommandSilent(boss + `attribute @s minecraft:generic.armor base set ${MF_AVAR_ARMOR}`)
    event.server.runCommandSilent(boss + `attribute @s minecraft:generic.armor_toughness base set ${MF_AVAR_TOUGHNESS}`)
    event.server.runCommandSilent(boss + `attribute @s irons_spellbooks:spell_resist base set ${MF_AVAR_IRONS_SPELL_RESIST}`)
    event.server.runCommandSilent(boss + 'tag @s add mf_final_boss_tuned')
  }
})

// Avar's outgoing damage multiplier is handled by lasthorizon-common.toml.
// Do not subscribe globally here: custom spell DamageSources can be nonstandard.

// LivingDamageEvent.Pre happens after normal armor, enchantment, mob-effect and
// other mitigation calculations. This makes the cap reliable against huge modded
// damage numbers while leaving ordinary combat calculations intact.
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre', event => {
  var target = event.getEntity()
  var source = event.getSource()
  var targetId = mfEntityId(target)

  // -------- Damage INTO Avar --------
  if (targetId === MF_AVAR) {
    if (mfIsEmergencyAdminDamage(target, source)) return

    var damage = Number(event.getNewDamage())
    if (!Number.isFinite(damage) || damage <= 0) return

    // Cap first, then apply extra magic resistance. This means a gigantic spell
    // cannot simply hit the same 60-HP ceiling as a gigantic physical attack.
    damage = Math.min(damage, MF_AVAR_DAMAGE_CAP)
    if (mfIsMagicDamage(target, source)) {
      damage *= MF_AVAR_GENERIC_MAGIC_MULTIPLIER
    }

    event.setNewDamage(Math.max(0.0, damage))
    return
  }

  // -------- Damage FROM Avar into players --------
  if (targetId !== 'minecraft:player') return
  var attacker = mfSourceEntity(source)
  if (mfEntityId(attacker) !== MF_AVAR) return

  var damage = Number(event.getNewDamage())
  if (!Number.isFinite(damage) || damage <= 0) return

  var raw = Number(event.getOriginalDamage())
  if (!Number.isFinite(raw)) raw = 0

  var maxHealth = Math.max(1.0, Number(target.getMaxHealth()))
  var floor

  if (raw >= MF_AVAR_HEAVY_RAW_THRESHOLD) {
    floor = Math.max(MF_AVAR_HEAVY_FLOOR_MIN, maxHealth * MF_AVAR_HEAVY_FLOOR_PERCENT)
    floor = Math.min(MF_AVAR_HEAVY_FLOOR_MAX, floor)
  } else {
    floor = Math.max(MF_AVAR_NORMAL_FLOOR_MIN, maxHealth * MF_AVAR_NORMAL_FLOOR_PERCENT)
    floor = Math.min(MF_AVAR_NORMAL_FLOOR_MAX, floor)
  }

  if (damage < floor) event.setNewDamage(floor)
})
