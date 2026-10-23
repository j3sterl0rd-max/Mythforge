# Live server network access

The October 23 firewall change completed successfully. Five dedicated inbound rules replace fifteen broad Java/game exceptions. The original Windows Firewall export, disabled rule names and applied scopes are private local recovery files under `ops/audit/firewall-20261023/`. Do not publish the full firewall export or private device inventory.

- TCP 25565: the production Java executable receives Minecraft traffic from TCPShield's published IPv4 ranges only. Explicit blocks cover other usable IPv4 sources and global/ULA/link-local IPv6 sources, preventing another broad app rule bypassing the intended source restrictions. The server is bound to its existing LAN IPv4 address. Keep the router's game forward to that address.
- TCP 25576: direct RCON is restricted to the home subnet and the server/phone's private Tailscale addresses. Public IPv4 and global IPv6 sources are blocked. Do not forward RCON in the router.
- UDP 25565, 25576 and 8193: blocked; these services use TCP.
- TCP 8193: voting remains bound to 127.0.0.1. External voting is not activated by this change. Forwarding the port alone will not make it work. Configure the intended protected vote delivery route and listener before exposing it; publishing a direct home voting address would reveal the origin IP to that service.

TCPShield's verified source ranges were `198.178.119.0/24` and `104.234.6.0/25`, from https://tcpshield.com/v4/. Review both firewall scopes and ProperProxyProtocol's trusted senders when the provider changes its ranges. Current rules and the Java program path should be reviewed after a Java upgrade. Firewall filtering does not prevent a volumetric attack against an already-known home address from saturating the ISP link.

## CraftControl through Tailscale

Tailscale is installed and signed in on the PC and Android phone. Unattended mode is enabled on the PC. A private background Serve TCP forwarder maps the PC's Tailscale port 25576 to `192.168.1.25:25576`. This is Serve, not public Funnel. Inspect it with `tailscale serve status --json`.

Use the PC's Tailscale IPv4 address (shown by `tailscale ip -4`) and port 25576 in CraftControl, with the existing private RCON password. Keep Tailscale connected on the phone. The hostname from `tailscale status` is an alternative, but the explicit IPv4 address is simplest. Both devices must remain authorized on the same private Tailscale network. Phone app use on cellular should be tested by the owner; the automated checks authenticate through the private address from the PC and verify phone connectivity.

The running server and saved configuration use the LAN bind `192.168.1.25`. Keep that binding while the private forwarder uses the same port on Tailscale addresses; a wildcard RCON listener can conflict with the forwarder. The sanitized repository's server.properties remains a staging template and is not the live credential file.

## Recovery

`ops/build-firewall-scopes.py` prepares local scopes using the authorized devices' actual Tailscale addresses. `ops/apply-server-firewall.ps1` must run as Administrator. It refuses to overwrite an existing managed rule set and preserves the original full firewall backup. Its `-Rollback` switch removes its five rules and reenables the recorded old rules. Failed application attempts roll back automatically. Inspect rules and back up current state before changing a working installation.

Verification after application: a fresh Minecraft status connection through `play.mythforge.win` succeeded; local RCON and authenticated RCON through the Tailscale IPv4 endpoint succeeded; the phone answered a Tailscale ping. The live player stayed connected. The chunk-generation supervisor was restored and correctly paused for the online player.
