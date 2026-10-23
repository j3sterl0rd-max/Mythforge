# Chunk pregeneration and Overworld exploration area

Generate circles centered on X=0, Z=0 in this order: new Overworld radius 5,000; Nether radius 5,000; End radius 5,000; then only the new Overworld radius 10,000 and radius 15,000. The final Overworld target is 30,000 blocks across. The staged expansion gives a complete smaller area before attempting the largest one. Existing generated chunks are skipped. The beta Overworld and other mod dimensions are excluded. The active custom RTP uses radius 1,500 in the Overworld/Nether and 2,500 in the End; the disabled FTB Essentials RTP settings do not control this.

`ops/pregenerate-worlds.py` supervises the installed Chunky 1.4.23. It pauses while players are online, pauses for a 60-second cooldown if average ticks exceed 45ms, stops below 60 GiB free disk space, and stops for inspection after five minutes without progress. It waits up to ten minutes through a normal backend restart. Already generated terrain remains saved; this does not keep the whole area loaded. Only one supervisor can run at once.

Status: `ops/pregeneration-5000.json`. Chunky's durable per-world checkpoints are in `config/chunky/tasks/`. Create `ops/pregeneration-stop.txt` to request a pause and stop the supervisor. Remove that marker before resuming with `python ops/pregenerate-worlds.py`. Manual `/chunky pause` without a marker does not stop the supervisor; use the marker for a durable stop.

The original status filename is retained for compatibility. `queue` and `completedTasks` track each world/radius pair, while `radius` describes the current pass. The supervisor migrates the original 5,000-block queue automatically. The larger Overworld passes retain the same player, performance, stall and free-space safeguards; reaching the target depends on available disk space and generation throughput.

## Approved Regions Unexplored workaround

The initial generation stalled at 1,194 processed chunks. A worldgen worker was blocked in Lithostitched 1.8.1 `InStructurePredicate`, while the main thread processed chunk unloads. The corresponding upstream report is https://github.com/Apollounknowndev/lithostitched/issues/115, including a dedicated-server Chunky report with the same stack and workaround.

The user approved the tradeoff before activation. `config/paxi/datapacks/mythforge-regions-unexplored-pregen-fix.zip` overrides eight Regions Unexplored 0.6.3 placed features, removing only each `not(lithostitched:in_structure)` placement filter. Other placement filters, frequencies, heights and features are unchanged:

- `patch/ash_vents_inferno`
- `patch/cave_bioshrooms`
- `patch/dropleaf`
- `patch/prismarite_cluster`
- `patch/redstone_bud`
- `patch/redstone_bulb`
- `special/lava_fall`
- `special/overworld_lava_delta`

These features can consequently generate inside structures in newly generated terrain. Existing chunks are not regenerated. No client update is required. The pre-patch source definitions are retained locally in `ops/pregeneration-structure-predicates.json`. The archive contains only pack metadata and those eight JSON overrides; all were parsed and checked.

Keep this workaround until an upstream fix or tested custom-mod replacement is installed. To revert, stop generation, remove this one ZIP, and restart. Removing it while generation is active can bring back the stall. A server restart was used for activation because already-running worldgen tasks retain their old predicates across a reload.
