# Server tools 1.0.7

Fixes AFK kick iteration crashes. Adds a server-validated playtime reward panel, explicit item claims, login reminders, persistent mutual PvP opt-in and vote reminder integration with Modded Votifier. Travel uses a five-second movement/damage warmup without a combat cooldown or visited-dimension gate.

Runtime verification: 26 update checks, 163 role/lifecycle checks, complete reward registry/weight audit and Votifier protocol/invalid-signature checks passed in an isolated full-modpack server.

The banned-item EMI tooltip is a client KubeJS addition and must ship in the client pack. Vote listing links must be registered before reminders are enabled.
