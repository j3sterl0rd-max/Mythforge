// Mythforge - native Minor Gamba Warden token drops.
// One token per credited player every 16 real-world hours.
// Keep this persistent key unchanged so existing cooldowns survive updates.
const MF_GACHA_TOKEN = 'gachamachine:gacha_token'
const MF_GACHA_COOLDOWN_KEY = 'mythforge_warden_gacha_next_ms'
const MF_GACHA_COOLDOWN_MS = 16 * 60 * 60 * 1000

EntityEvents.death(event => {
  if (String(event.entity.type) !== 'minecraft:warden') return

  var player = event.source.player
  if (!player && event.source.actual && event.source.actual.player) player = event.source.actual
  if (!player) return

  var now = Date.now()
  var data = player.persistentData
  // Migrate the last award from the former 30-minute cooldown exactly once.
  if (!data.getBoolean('mythforge_warden_gacha_16h')) {
    var oldNext = Number(data.getLong(MF_GACHA_COOLDOWN_KEY))
    if (oldNext > 0) data.putLong(MF_GACHA_COOLDOWN_KEY, Math.floor(oldNext - 30 * 60 * 1000 + MF_GACHA_COOLDOWN_MS))
    data.putBoolean('mythforge_warden_gacha_16h', true)
  }
  var nextAllowed = Number(data.getLong(MF_GACHA_COOLDOWN_KEY))

  if (nextAllowed > now) {
    var seconds = Math.ceil((nextAllowed - now) / 1000)
    var minutes = Math.floor(seconds / 60)
    var remainder = seconds % 60
    player.tell(`\u00a77No Minor Gamba Token this time. Your Warden reward cooldown has \u00a7d${minutes}m ${remainder}s\u00a77 remaining.`)
    return
  }

  var tokenDrop = event.entity.spawnAtLocation(Item.of(MF_GACHA_TOKEN))
  if (!tokenDrop) return
  data.putLong(MF_GACHA_COOLDOWN_KEY, Math.floor(now + MF_GACHA_COOLDOWN_MS))
  player.tell('\u00a7d\u00a7lMinor Gamba Token dropped! \u00a7r\u00a77Collect it from the Warden. Your next Warden token reward is available in 16 hours.')
})
