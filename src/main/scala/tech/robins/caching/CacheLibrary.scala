package tech.robins.caching

object CacheLibrary {
  def byName[T <: HasCacheRemovalHook](name: String, size: Int): Cache[T] = name match {
    case "FixedSizeRoundRobin" => new FixedSizeRoundRobinCache[T](size)
    case _                     => throw new IllegalArgumentException(s"Unknown cache: $name")
  }
}
