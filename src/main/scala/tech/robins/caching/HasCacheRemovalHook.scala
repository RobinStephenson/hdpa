package tech.robins.caching

trait HasCacheRemovalHook {
  def onRemovedFromCache(): Unit
}

trait UnitCacheRemovalHook extends HasCacheRemovalHook {
  def onRemovedFromCache(): Unit = Unit
}
