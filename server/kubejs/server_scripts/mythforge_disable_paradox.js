// User-selected policy: the Paradox Machine is disabled in Mythforge.
// Java patches independently prevent operation of pre-existing machines.
ServerEvents.recipes(event => {
  event.remove({ output: 'justdirethings:paradoxmachine' })
})
