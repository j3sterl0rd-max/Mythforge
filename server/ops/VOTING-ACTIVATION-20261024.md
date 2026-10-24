# Voting activation - October 24, 2026

Modded Votifier 1.0.3 is configured on 192.168.1.25 TCP 8193. The Windows inbound allow rule is restricted to that address, TCP port and the server Java executable. Existing TCPShield game rules, private RCON rules and the UDP block remain unchanged.

The existing reward command remains `mythforge vote {player}`: one crate key per accepted vote, with the Votifier mod retaining offline votes until login. V1 RSA and V2 token delivery are supported; prefer V2 when a site offers it.

Private form settings are stored locally at ops/private/voting-setup/VOTING-SITE-SETTINGS.txt. Never commit this file, the token, private.pem or config/votifier/config.json.

Public game address: play.mythforge.win:25565. Vote delivery uses the current home public IPv4 and TCP 8193, entered only in the Votifier settings. Do not publish a home-IP DNS record or substitute this address for the public game address. This direct voting endpoint is outside TCPShield's game protection.

Register listings at:
- https://minecraftservers.org/
- https://minecraft-mp.com/
- https://minecraft-server-list.com/
- https://minecraft.buzz/
- https://topg.org/minecraft-servers/

After each listing exists: enter the private Votifier settings, send its built-in test vote to a real Minecraft username, confirm one crate key, and send the listing's vote URL so /vote and reminders can be connected. Router forwarding and real site delivery cannot be confirmed solely by testing from inside the home network.
