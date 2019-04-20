package tech.robins.caching

trait Cache[T] {
  protected def callRemovalHookIfPresent(element: T): Unit = element match {
    case removedElement: HasCacheRemovalHook => removedElement.onRemovedFromCache()
  }

  def getItems: Iterable[T]

  def add(element: T): Unit

  /** Remove the element from the cache if it is present
    * @param element the element to remove
    * @return true if the element was present, false otherwise
    */
  def removeIfPresent(element: T): Boolean

  def contains(element: T): Boolean

  def exists(predicate: T => Boolean): Boolean

  def find(predicate: T => Boolean): Option[T]
}
