package tech.robins.caching

trait HasCacheRemovalHook {

  /** Called by the cache when removing from the cache */
  def onRemovedFromCache(): Unit
}

trait UnitCacheRemovalHook extends HasCacheRemovalHook {
  def onRemovedFromCache(): Unit = Unit
}
