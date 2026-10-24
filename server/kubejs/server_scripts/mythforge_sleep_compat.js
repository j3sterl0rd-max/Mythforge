// Creating Space 1.7.22/1.7.23 + Spectrum 1.12.9, NeoForge / KubeJS 7.
// CS changes the Overworld clock at NORMAL priority, before Spectrum examines it.
// Decide using the pre-event clock, then reconcile CS's cross-dimension clock.
;(function () {
  if (!Platform.isLoaded('creatingspace') || !Platform.isLoaded('spectrum')) return
  var mfSleepPriority = Java.loadClass('net.neoforged.bus.api.EventPriority')
  var mfSleepMap = Java.loadClass('java.util.WeakHashMap')
  var mfSleepOriginalTime = new mfSleepMap()
  var mfSleepOriginalTarget = new mfSleepMap()
  var mfSleepEvent = 'net.neoforged.neoforge.event.level.SleepFinishedTimeEvent'

  NativeEvents.onEvent(mfSleepPriority.HIGHEST, mfSleepEvent, event => {
    mfSleepOriginalTime.put(event, Number(event.getLevel().dayTime()))
    mfSleepOriginalTarget.put(event, Number(event.getNewTime()))
  })

  NativeEvents.onEvent(mfSleepPriority.LOWEST, mfSleepEvent, event => {
    if (!mfSleepOriginalTime.containsKey(event)) return
    var before = Number(mfSleepOriginalTime.remove(event))
    var originalTarget = Number(mfSleepOriginalTarget.remove(event))
    // Spectrum 1.12.9's TimeHelper.isDay() means DAY/NOON: [0,12000).
    // Avoid Rhino choosing the helper's overloaded Level argument for a number.
    var dayTime = before % 24000
    var target = dayTime >= 0 && dayTime < 12000
      ? before - (before % 24000) + 13000
      : originalTarget
    // NeoForge rejects backwards skips. Keep its validation rather than force one.
    if (!event.setTimeAddition(target)) return
    var server = event.getLevel().getServer()
    if (server == null) return
    var overworld = server.overworld()
    if (overworld != null) overworld.setDayTime(event.getNewTime())
  })
})()
