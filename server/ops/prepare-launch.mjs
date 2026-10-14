import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const item=(id,count=1,extra={})=>({id,count,...extra});
const pool=(name,count=1)=>({pool:name,count});
const power=[item('excessive_utilities:furnace_generator',2),item('minecraft:coal',64),item('cyclic:energy_pipe',16)];
const wireless=[item('ae2:wireless_crafting_terminal'),item('ae2:wireless_access_point'),item('ae2:wireless_booster',8),item('ae2:charger')];
const milestones={
  5:[item('minecraft:bundle'),item('minecraft:cooked_beef',16)],
  10:[item('transmog:transmogrification_table'),item('minecraft:amethyst_shard',16)],
  20:[],25:[item('hostilenetworks:deep_learner'),item('hostilenetworks:sim_chamber'),item('hostilenetworks:loot_fabricator'),item('hostilenetworks:blank_data_model',2),item('hostilenetworks:prediction_matrix',64),item('excessive_utilities:furnace_generator',16),item('minecraft:coal',256),item('cyclic:energy_pipe',32)],
  30:[],40:[],50:[item('ae2:drive'),item('ae2:terminal'),item('ae2:item_storage_cell_1k',2),item('ae2:fluix_glass_cable',16),item('ae2:energy_acceptor'),...power],
  60:[item('storagedrawers:diamond_storage_upgrade',8)],70:[],80:wireless,90:[],100:[],120:[],150:[],175:[],200:[],250:[],300:[],400:[],500:[item('infinitestoragecell:infinity_item_cell')]
};
const pools={
  decoration:['createdeco:long_red_bricks','createdeco:blue_bricks','createdeco:dean_bricks'],
  furniture:['decocraft:bistro_chair','decocraft:cottage_armchair','decocraft:cottage_coffee_table'],
  normalEgg:['bat','bee','fox','frog','goat','panda','cat','axolotl','mooshroom','camel'].map(id=>'minecraft:'+id+'_spawn_egg'),
  exoticEgg:['minecraft:sniffer_spawn_egg','minecraft:allay_spawn_egg'],
  backpack:['travelersbackpack:standard','travelersbackpack:diamond','travelersbackpack:gold'],
  pets:['inventorypets:pet_cow','inventorypets:pet_pig','inventorypets:pet_chicken'],
  standardSword:['longsword','katana','spear','glaive','cutlass','claymore','rapier','twinblade','halberd','greataxe','greathammer','chakram','scythe','sai','warglaive'].map(id=>'simplyswords:diamond_'+id),
  uniqueSword:['simplyswords:bramblethorn','simplyswords:stormbringer','simplyswords:hearthflame'],
  relic:['relics:experience_disperser','relics:hunting_belt','relics:pet_bone','relics:chef_hat','relics:piglin_mask']
};
const prize=(weight,name,items)=>({weight,name,items});
const tiers={
 common:[
  prize(300,'Botany Pot x1',[item('botanypots:terracotta_botany_pot')]),
  prize(300,'Charged Certus Quartz Crystals x16',[item('ae2:charged_certus_quartz_crystal',16)]),
  prize(350,'Industrial Foregoing Plastic x16',[item('industrialforegoing:plastic',16)]),
  prize(450,'Random Decorative Block Stack x64',[pool('decoration',64)]),
  prize(200,'Copycats+ Panels x32',[item('copycats:copycat_half_panel',32)]),
  prize(180,'Basic Storage Drawers x4',[item('storagedrawers:oak_full_drawers_1',4)]),
  prize(160,'Empty Mob Lassos x4',[item('moblassos:golden_lasso',4)]),
  prize(160,'AE2 ME Glass Cables x16',[item('ae2:fluix_glass_cable',16)]),
  prize(180,'Create Brass Funnels x8',[item('create:brass_funnel',8)]),
  prize(170,'Random Decocraft Furniture Bundle x6',Array.from({length:6},()=>pool('furniture'))),
  prize(300,'Cyclic Item Cables x64',[item('cyclic:item_pipe',64)]),
  prize(300,'Cyclic Fluid Cables x64',[item('cyclic:fluid_pipe',64)]),
  prize(300,'Cyclic Energy Cables x64',[item('cyclic:energy_pipe',64)]),
  prize(320,'Just Dire Things Creature Catchers x64',[item('justdirethings:creaturecatcher',64)]),
  prize(310,'Golden Carrots x64',[item('minecraft:golden_carrot',64)]),
  prize(200,'Cyclic Cloud Blocks x32',[item('cyclic:cloud',32)]),
  prize(250,'Dark Utilities Vector Plates x32',[item('darkutils:vector_plate',32)]),
  prize(220,"Steam 'n' Rails Tracks x64",[item('railways:track_oak',64)]),
  prize(150,'Cyclic Filter Cards x8',[item('cyclic:filter_data',8)])
 ],
 uncommon:[
  prize(440,'10-Minute Creative Flight Potion x1',[item('minecraft:potion',1,{custom:'flight_10'})]),
  prize(400,'Vote Crate Keys x2',[{keys:2}]),
  prize(400,'4K ME Item Storage Cell x1',[item('ae2:item_storage_cell_4k')]),
  prize(350,'Random Whitelisted Mob Spawn Egg x1',[pool('normalEgg')]),
  prize(300,'Hopper Botany Pot x1',[item('botanypots:terracotta_hopper_botany_pot')]),
  prize(210,"Random Traveler's Backpack x1",[pool('backpack')]),
  prize(170,'Storage Drawers Drawer Controller x1',[item('storagedrawers:controller')]),
  prize(140,'PneumaticCraft Speed Upgrades x8',[item('pneumaticcraft:speed_upgrade',8)]),
  prize(110,'Linked Ender Storage Chests x2',[item('enderstorage:ender_chest',2)]),
  prize(160,'4K ME Fluid Storage Cell x1',[item('ae2:fluid_storage_cell_4k')]),
  prize(80,'Random Safe Inventory Pet x1',[pool('pets')]),
  prize(180,'Create Aeronautics Levitite Blocks x4',[item('aeronautics:levitite',4)]),
  prize(180,'Ferricore Solar Panel x1',[item('justdynathings:ferricore_solar_panel')]),
  prize(140,'Cyclic Build Scepter x1',[item('cyclic:scepter_build')]),
  prize(140,'Ender Hopper x1',[item('cyclic:hopper')]),
  prize(200,'Flux Network Starter Bundle x1',[item('fluxnetworks:flux_plug',2),item('fluxnetworks:flux_point',2)])
 ],
 rare:[
  prize(220,'Random Standard Simply Swords Weapon x1',[pool('standardSword')]),
  prize(170,'Gacha Token x1',[item('gachamachine:gacha_token')]),
  prize(165,'Nameless Trinkets Mysterious Trinket x1',[item('nameless_trinkets:mysterious_trinket')]),
  prize(145,'Relic Level-Up Consumable x1',[item('minecraft:experience_bottle',1,{custom:'relic_level'})]),
  prize(85,'Create Creative Blaze Cake x1',[item('create:creative_blaze_cake')]),
  prize(90,'Random Approved Relic x1',[pool('relic')]),
  prize(55,'16K ME Item Storage Cell x1',[item('ae2:item_storage_cell_16k')]),
  prize(65,'60-Minute Creative Flight Potion x1',[item('minecraft:potion',1,{custom:'flight_60'})]),
  prize(45,'Random Exotic Spawn Egg x1',[pool('exoticEgg')]),
  prize(130,'Random Simply Swords Unique Weapon x1',[pool('uniqueSword')]),
  prize(55,'Blazegold Solar Panel x1',[item('justdynathings:blazegold_solar_panel')]),
  prize(40,'Celestigem Solar Panel x1',[item('justdynathings:celestigem_solar_panel')]),
  prize(30,'Eclipse Alloy Solar Panel x1',[item('justdynathings:eclipse_alloy_solar_panel')]),
  prize(90,'Wireless Crafting Terminal x1',[item('ae2:wireless_crafting_terminal')]),
  prize(35,'Lilypad of Fertility x1',[item('reliquary:fertile_lily_pad')]),
  prize(40,'Magnum Torch x1',[item('magnumtorch:diamond_magnum_torch')]),
  prize(40,'Flux Controller x1',[item('fluxnetworks:flux_controller')])
 ],
 jackpot:[
  prize(50,'Infinity Range Card x1',[item('aeinfinitybooster:infinity_card')]),
  prize(25,'Infinite ME Storage Cell x1',[item('infinitestoragecell:infinity_item_cell')]),
  prize(15,'Relic Level-Up Consumables x3',[item('minecraft:experience_bottle',3,{custom:'relic_level'})]),
  prize(10,'Gacha Tokens x3',[item('gachamachine:gacha_token',3)])
 ]
};
const config={version:1,startingClaims:64,startingForceLoads:2,teamLimitMode:'largest',functionalBlocks:['create:shaft','create:cogwheel','create:large_cogwheel','create:encased_chain_drive','create:belt','create:gearbox','create:clutch','create:gearshift'],milestones,pools,crate:Object.entries(tiers).flatMap(([tier,rows])=>rows.map(row=>({...row,tier}))),paidItems:{1:[item('create:creative_blaze_cake'),item('relics:experience_disperser',1,{maxed:true})],2:[item('aeinfinitybooster:infinity_card'),...wireless],3:[{relicChoice:true}],4:[item('infinitestoragecell:infinity_item_cell'),item('aeinfinitybooster:dimension_card')]}};
if(config.crate.length!==56||config.crate.reduce((s,r)=>s+r.weight,0)!==10000)throw Error('Crate specification mismatch');
fs.writeFileSync(path.join(root,'config/mythforge-server.json'),JSON.stringify(config,null,2)+'\n');
const playerCommands=['help','list','msg','tell','w','me','teammsg','tm','home','sethome','delhome','homes','back','spawn','warp','tpa','tpaccept','tpdeny','tpahere','rewards','mail','crate','vote','rtp','ascend','feed','heal','fly','repair','effects','grindstone','pwarp','waypoint','prefix','rename','lore','joinmessage','worldtime','worldweather','ftbteams','ftbchunks','ftbquests','ftbultimine','kickme','rec','afk','relic'];
const player={name:'Player',power:1,condition:'always_active',command:false,'ftbranks.name_format':'&7{name}&r','ftbchunks.max_claimed':64,'ftbchunks.max_force_loaded':2,'ftbchunks.chunk_load_offline':false,'ftbessentials.home.max':1,...Object.fromEntries(playerCommands.map(command=>['command.'+command,true]))};
const ranks={player};
for(const node of ['command.enderchest.target','command.ftbchunks.admin','command.ftbteams.force-add','command.ftbteams.force-remove','command.ftbteams.force-disband','command.ftbteams.nbtedit','command.ftbteams.server','command.ftbteams.serverconfig','command.ftbteams.teamstage','command.ftbteams.party.settings_for','command.ftbteams.party.transfer_ownership_for','command.nicknamefor'])player[node]=false;
for(const sub of ['block_rewards','change_progress','cleanup_stale_translations','clear_item_display_cache','delete_empty_reward_tables','editing_mode','export_reward_table_to_chest','generate_chapter','import_reward_table_from_chest','locked','reload'])player['command.ftbquests.'+sub]=false;
player['command.ftbquests.open_book']=true;
for(const command of ['chatcolor','ptime','pweather','particles'])player['command.'+command]=true;
for(const [i,id] of ['ember','artisan','runeforged','mythic'].entries())ranks[id]={name:id[0].toUpperCase()+id.slice(1),power:100+i,'ftbranks.name_format':'&6['+id[0].toUpperCase()+id.slice(1)+'] &f{name}&r'};
ranks.junior_mod={name:'Junior Moderator',power:200,'ftbranks.name_format':'&3[JrMod] &f{name}&r','command.kick':true,'command.mute':true,'command.unmute':true};
ranks.moderator={name:'Moderator',power:300,'ftbranks.name_format':'&9[Mod] &f{name}&r','command.kick':true,'command.mute':true,'command.unmute':true,'command.ban':true,'command.banlist':true,'command.pardon':true};
ranks.admin={name:'Admin',power:400,'ftbranks.name_format':'&d[Admin] &f{name}&r','command.kick':true,'command.mute':true,'command.unmute':true,'command.ban':true,'command.pardon':true,'command.invsee':true,'command.spark':true,'command.grieflogger':true};
ranks.owner={name:'Owner',power:1000,condition:'op',command:true,'ftbranks.name_format':'&6[Owner] &f{name}&r'};
// JSON is valid SNBT, including quoted dotted node names.
for(const dest of ['defaultconfigs/ftbranks/ranks.snbt','world/serverconfig/ftbranks/ranks.snbt']){fs.mkdirSync(path.dirname(path.join(root,dest)),{recursive:true});fs.writeFileSync(path.join(root,dest),JSON.stringify(ranks,null,2)+'\n');}
fs.mkdirSync(path.join(root,'ops/reference'),{recursive:true});
for(const name of ['Mythforge_Paid_Ranks.txt','Mythforge_Playtime_Rewards.txt','Mythforge_Vote_Crate_Complete_Loot_Table_v2.txt','Mythforge_Other_Changes.txt']){const source='C:/Users/Dad/Downloads/'+name;if(fs.existsSync(source))fs.copyFileSync(source,path.join(root,'ops/reference',name));}
console.log('Generated current launch specification: '+Object.keys(milestones).length+' milestones, four paid ranks, 56 crate entries and 10,000 weight.');


