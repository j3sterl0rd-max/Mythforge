// Server-owned Refuge claims; runs once per server/world load, never every tick.
var RefugeTeamsAPI = Java.loadClass('dev.ftb.mods.ftbteams.api.FTBTeamsAPI');
var RefugeChunksAPI = Java.loadClass('dev.ftb.mods.ftbchunks.api.FTBChunksAPI');
var RefugeProps = Java.loadClass('dev.ftb.mods.ftbchunks.api.FTBChunksProperties');
var RefugePrivacy = Java.loadClass('dev.ftb.mods.ftbteams.api.property.PrivacyMode');
var RefugeUUID = Java.loadClass('java.util.UUID');
var RefugeLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RefugeKey = Java.loadClass('net.minecraft.resources.ResourceKey');
var RefugeRegistries = Java.loadClass('net.minecraft.core.registries.Registries');
var RefugeChunkPos = Java.loadClass('dev.ftb.mods.ftblibrary.math.ChunkDimPos');
var REFUGE_TEAM_ID = RefugeUUID.fromString('9a25de18-7cd4-52da-ae35-9f2dd4b368cb');

function establishRefugeProtection(server, attempt) {
  try {
    if (!RefugeTeamsAPI.api().isManagerLoaded() || !RefugeChunksAPI.api().isManagerLoaded()) {
      if (attempt < 5) server.scheduleInTicks(20, function () { establishRefugeProtection(server, attempt + 1); });
      else console.error('[Mythforge Refuge] FTB managers did not become available; claims were not initialized.');
      return;
    }
    var dim = RefugeKey.create(RefugeRegistries.DIMENSION, RefugeLocation.parse('mythforge:horizon_refuge'));
    if (server["getLevel(net.minecraft.resources.ResourceKey)"](dim) == null) {
      console.error('[Mythforge Refuge] Dimension missing; check Refuge datapack installation.');
      return;
    }
    var source = server.createCommandSourceStack();
    var teams = RefugeTeamsAPI.api().getManager();
    var team = teams.getTeamByID(REFUGE_TEAM_ID).orElse(null);
    if (team == null) team = teams.createServerTeam(source, 'mythforge_refuge', 'Protected Refuge sanctuary', null, REFUGE_TEAM_ID);
    team.setProperty(RefugeProps.ALLOW_EXPLOSIONS, false);
    team.setProperty(RefugeProps.ALLOW_MOB_GRIEFING, false);
    team.setProperty(RefugeProps.BLOCK_EDIT_MODE, RefugePrivacy.PRIVATE);
    team.setProperty(RefugeProps.BLOCK_INTERACT_MODE, RefugePrivacy.PRIVATE);
    team.setProperty(RefugeProps.ENTITY_INTERACT_MODE, RefugePrivacy.PUBLIC);
    team.setProperty(RefugeProps.ALLOW_ALL_FAKE_PLAYERS, false);
    team.setProperty(RefugeProps.ALLOW_FAKE_PLAYERS_BY_ID, false);
    team.markDirty();
    var manager = RefugeChunksAPI.api().getManager();
    var data = manager.getOrCreateData(team);
    var protectedCount = 0;
    for (var cx = -2; cx <= 14; cx++) {
      for (var cz = -2; cz <= 14; cz++) {
        var pos = new RefugeChunkPos(dim, cx, cz);
        var existing = manager.getChunk(pos);
        // Supersede old test-world claims only within the Refuge island and rescue margin.
        if (existing != null && !existing.getTeamData().getTeam().getId().equals(REFUGE_TEAM_ID)) {
          existing.unclaim(source, true);
          existing = null;
        }
        if (existing == null) {
          var result = data.claim(source, pos, false);
          if (!result.isSuccess()) {
            console.error('[Mythforge Refuge] Claim failed at ' + cx + ',' + cz + ': ' + result);
            continue;
          }
        }
        protectedCount++;
      }
    }
    if (protectedCount !== 289) console.error('[Mythforge Refuge] INCOMPLETE claim coverage: ' + protectedCount + '/289.');
    else console.info('[Mythforge Refuge] Automatic protection ready: 289/289 claims; no force-loaded chunks.');
  } catch (error) {
    console.error('[Mythforge Refuge] Protection setup failed: ' + error);
  }
}
ServerEvents.loaded(function (event) {
  event.server.scheduleInTicks(1, function () { establishRefugeProtection(event.server, 0); });
});

