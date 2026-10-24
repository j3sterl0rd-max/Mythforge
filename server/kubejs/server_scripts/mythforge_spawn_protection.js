// Exact, server-owned protection of the new Overworld spawn. No Refuge/beta edits.
var MFSP = JsonIO.read('config/mythforge-spawn.json');
var MFSPRanks = Java.loadClass('dev.ftb.mods.ftbranks.api.FTBRanksAPI');
var MFSPFake = Java.loadClass('net.neoforged.neoforge.common.util.FakePlayer');
var MFSPMonster = Java.loadClass('net.minecraft.world.entity.monster.Monster');
var MFSPBucket = Java.loadClass('net.minecraft.world.item.BucketItem');
var MFSPTri = Java.loadClass('net.neoforged.neoforge.common.util.TriState');
var MFSPHostileIds = {};
JsonIO.read('config/paxi/datapacks/mythforge-refuge/data/mythforge/tags/entity_type/refuge_hostiles.json').values.forEach(function (value) {
  MFSPHostileIds[typeof value === 'string' ? value : value.id] = true;
});
var MFSPRegistries = Java.loadClass('net.minecraft.core.registries.Registries');
var MFSPResource = Java.loadClass('net.minecraft.resources.ResourceLocation');
function mfspInside(level, pos) {
  return String(level.dimension) === MFSP.dimension &&
    pos.getX() >= MFSP.minX && pos.getX() <= MFSP.maxX &&
    pos.getZ() >= MFSP.minZ && pos.getZ() <= MFSP.maxZ &&
    pos.getY() >= MFSP.minY && pos.getY() <= MFSP.maxY;
}
function mfspStaff(player) {
  if (player == null || player instanceof MFSPFake || typeof player.getGameProfile !== 'function') return false;
  var ranks = MFSPRanks.manager().getAddedRanks(player.getGameProfile()).iterator();
  while (ranks.hasNext()) {
    var id = String(ranks.next().getId());
    if (id === 'moderator' || id === 'admin' || id === 'owner') return true;
  }
  return false;
}
function mfspBreak(event) {
  if (mfspInside(event.getLevel(), event.getPos()) && !mfspStaff(event.getPlayer())) event.setCanceled(true);
}
function mfspPlace(event) {
  if (mfspStaff(event.getEntity())) return;
  if (mfspInside(event.getLevel(), event.getPos())) { event.setCanceled(true); return; }
  if (typeof event.getReplacedBlockSnapshots === 'function') {
    var snapshots = event.getReplacedBlockSnapshots().iterator();
    while (snapshots.hasNext()) if (mfspInside(event.getLevel(), snapshots.next().getPos())) { event.setCanceled(true); return; }
  }
}
function mfspInteract(event) {
  if (mfspInside(event.getLevel(), event.getPos()) && !mfspStaff(event.getEntity())) {
    // Keep block use intact: chests, buttons, doors and NPCs remain usable.
    // Deny item use on the block: buckets, fire, tools and blocks cannot edit it.
    event.setUseItem(MFSPTri.FALSE);
  }
}
function mfspItem(event) {
  if (event.getItemStack().getItem() instanceof MFSPBucket &&
      mfspInside(event.getLevel(), event.getEntity().blockPosition()) && !mfspStaff(event.getEntity())) event.setCanceled(true);
}
function mfspExplosion(event) {
  if (String(event.getLevel().dimension) !== MFSP.dimension) return;
  event.getAffectedBlocks().removeIf(function (pos) { return mfspInside(event.getLevel(), pos); });
}
function mfspHostile(entity) {
  return entity instanceof MFSPMonster || (typeof entity.isMonster === 'function' && entity.isMonster()) || MFSPHostileIds[String(entity.getType())] === true;
}
function mfspJoin(event) {
  if (mfspInside(event.getLevel(), event.getEntity().blockPosition()) && mfspHostile(event.getEntity())) event.setCanceled(true);
}
function mfspPiston(event) {
  if (String(event.getLevel().dimension) !== MFSP.dimension) return;
  var source = event.getPos();
  if (source.getX() < MFSP.minX - 13 || source.getX() > MFSP.maxX + 13 || source.getZ() < MFSP.minZ - 13 || source.getZ() > MFSP.maxZ + 13) return;
  if (mfspInside(event.getLevel(), event.getPos())) { event.setCanceled(true); return; }
  var helper = event.getStructureHelper();
  if (!helper.resolve()) return;
  var blocks = helper.getToPush().iterator();
  while (blocks.hasNext()) {
    var pos = blocks.next();
    if (mfspInside(event.getLevel(), pos) || mfspInside(event.getLevel(), pos.relative(event.getDirection()))) { event.setCanceled(true); return; }
  }
}
NativeEvents.onEvent('net.neoforged.neoforge.event.level.BlockEvent$BreakEvent', mfspBreak);
NativeEvents.onEvent('net.neoforged.neoforge.event.level.BlockEvent$EntityPlaceEvent', mfspPlace);
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock', mfspInteract);
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickItem', mfspItem);
NativeEvents.onEvent('net.neoforged.neoforge.event.level.ExplosionEvent$Detonate', mfspExplosion);
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.EntityJoinLevelEvent', mfspJoin);
NativeEvents.onEvent('net.neoforged.neoforge.event.level.PistonEvent$Pre', mfspPiston);
function mfspArrival(event) {
  if (!MFSP.built) return;
  var player = event.getEntity();
  var level = player.serverLevel();
  if (String(level.dimension) !== MFSP.dimension) return;
  var pos = player.blockPosition();
  var atRoof = pos.getX() >= MFSP.minX && pos.getX() <= MFSP.maxX && pos.getZ() >= MFSP.minZ && pos.getZ() <= MFSP.maxZ && pos.getY() >= MFSP.maxY - 1;
  var firstLogin = player.getStats().getPlayTime() === 0;
  if (atRoof || firstLogin) player.teleportToLevel(level, MFSP.centerX + 0.5, MFSP.glowstoneY + 1, MFSP.centerZ + 0.5, 0, 0);
}
// Vanilla can choose the roof's heightmap when a player first joins. Bring
// those arrivals to the actual shared spawn inside the sealed enclosure.
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent', mfspArrival);
NativeEvents.onEvent('net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerRespawnEvent', mfspArrival);
var MFSPPurged = false;
// Spawn construction is complete; no construction tickets or private chunk-map access.
ServerEvents.tick(function (event) {
  if (MFSPPurged) return;
  MFSPPurged = true;
  var level = event.server.overworld(), remove = [], entities = level.getAllEntities().iterator();
  while (entities.hasNext()) {
    var entity = entities.next();
    if (mfspInside(level, entity.blockPosition()) && mfspHostile(entity)) remove.push(entity);
  }
  remove.forEach(function (entity) { entity.discard(); });
});

ServerEvents.commandRegistry(function (event) {
  var C = event.commands;
  event.register(C.literal('mfspawn').requires(function (source) { return source.getEntity() == null; })
    .then(C.literal('verify').executes(function (context) {
      var server = context.source.server;
      var level = server.overworld();
      var Pos = Java.loadClass('net.minecraft.core.BlockPos');
      var Player = Java.loadClass('net.minecraft.server.level.ServerPlayer');
      var Profile = Java.loadClass('com.mojang.authlib.GameProfile');
      var UUID = Java.loadClass('java.util.UUID');
      var Info = Java.loadClass('net.minecraft.server.level.ClientInformation');
      var Break = Java.loadClass('net.neoforged.neoforge.event.level.BlockEvent$BreakEvent');
      var Join = Java.loadClass('net.neoforged.neoforge.event.entity.EntityJoinLevelEvent');
      var Types = Java.loadClass('net.minecraft.world.entity.EntityType');
      var List = Java.loadClass('java.util.ArrayList');
      var inside = new Pos(0, 127, 0), outside = new Pos(101, 127, 0);
      var player = new Player(server, level, new Profile(UUID.fromString('c8eace22-bb00-4aa0-9000-000000000088'), 'MF_SpawnCheck'), Info.createDefault());
      var passed = [];
      function check(condition, label) { if (!condition) throw new Error(label); passed.push(label); }
      check(server.getSpawnProtectionRadius() === 0, 'Vanilla OP-only spawn protection disabled');
      check(mfspInside(level, inside) && !mfspInside(level, outside), 'Exact square bounds');
      var other = server['getLevel(net.minecraft.resources.ResourceKey)'](Java.loadClass('net.minecraft.resources.ResourceKey').create(MFSPRegistries.DIMENSION, MFSPResource.parse('mythforge:old_overworld')));
      if (other != null) check(!mfspInside(other, inside), 'Old beta Overworld excluded');
      var denied = new Break(level, inside, level.getBlockState(inside), player); mfspBreak(denied);
      check(denied.isCanceled(), 'Ordinary player cannot break spawn blocks');
      var placed = {getEntity:function(){return player;}, getLevel:function(){return level;}, getPos:function(){return inside;}, setCanceled:function(value){this.denied=value;}};
      mfspPlace(placed); check(placed.denied === true, 'Ordinary player cannot place spawn blocks');
      var permitted = new Break(level, outside, level.getBlockState(outside), player); mfspBreak(permitted);
      check(!permitted.isCanceled(), 'Outside spawn is unaffected');
      for (var roleIndex = 0; roleIndex < 3; roleIndex++) {
        var role = ['moderator', 'admin', 'owner'][roleIndex];
        var rank = MFSPRanks.manager().getRank(role).orElseThrow(); rank.add(player.getGameProfile());
        try {
          var allowed = new Break(level, inside, level.getBlockState(inside), player); mfspBreak(allowed);
          check(!allowed.isCanceled(), role + ' can build at spawn');
        } finally { rank.remove(player.getGameProfile()); }
      }
      var click = { getLevel:function(){return level;}, getPos:function(){return inside;}, getEntity:function(){return player;}, setUseItem:function(value){this.item=value;} };
      mfspInteract(click); check(click.item === MFSPTri.FALSE, 'Block interaction retained while item-based edits are denied');
      var blocks = new List(); blocks.add(inside); blocks.add(outside);
      mfspExplosion({getLevel:function(){return level;}, getAffectedBlocks:function(){return blocks;}});
      check(blocks.size() === 1 && blocks.get(0).equals(outside), 'Explosion removes only protected blocks from its damage list');
      var zombie = Types.ZOMBIE.create(level); zombie.setPos(0.5, 127, 0.5);
      var spawn = new Join(zombie, level); mfspJoin(spawn); check(spawn.isCanceled(), 'Hostile monster spawning rejected');
      var cow = Types.COW.create(level); cow.setPos(0.5, 127, 0.5);
      var passive = new Join(cow, level); mfspJoin(passive); check(!passive.isCanceled(), 'Passive entities allowed');
      var savedBuilt = MFSP.built, arrival = null;
      MFSP.built = true;
      try {
        var arriving = {serverLevel:function(){return level;}, blockPosition:function(){return new Pos(0,320,0);}, getStats:function(){return {getPlayTime:function(){return 0;}};}, teleportToLevel:function(destination,x,y,z,yaw,pitch){arrival=[x,y,z];}};
        mfspArrival({getEntity:function(){return arriving;}});
        check(arrival != null && arrival[0] === 0.5 && arrival[1] === 128 && arrival[2] === 0.5, 'New arrivals are placed on center glowstone, not the roof');
      } finally { MFSP.built = savedBuilt; }
      JsonIO.write('ops/evidence/spawn-protection-tests.json', {success:true, passed:passed});
      context.source.sendSuccess(function(){return Text.of('Spawn protection checks passed: ' + passed.length);}, false);
      return 1;
    }))
    .then(C.literal('purge').executes(function (context) {
      var level = context.source.server.overworld();
      var remove = [], entities = level.getAllEntities().iterator();
      while (entities.hasNext()) {
        var entity = entities.next();
        if (mfspInside(level, entity.blockPosition()) && mfspHostile(entity)) remove.push(entity);
      }
      remove.forEach(function (entity) { entity.discard(); });
      context.source.sendSuccess(function(){return Text.of('Removed ' + remove.length + ' hostile entities from spawn.');}, false);
      return 1;
    })));
});
console.info('[Mythforge Spawn] Protected region registered in ' + MFSP.dimension + ': ' + MFSP.minX + '..' + MFSP.maxX + ', ' + MFSP.minZ + '..' + MFSP.maxZ);
