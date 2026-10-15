// Apply the launch identity on startup and after a server-script reload.
let mythforgeIdentityApplied = false;
ServerEvents.tick(event => {
  if (mythforgeIdentityApplied) return;
  event.server.setMotd('Mythforge: Beyond the Horizon');
  mythforgeIdentityApplied = true;
  console.info('[Mythforge] Launch server identity applied.');
});
