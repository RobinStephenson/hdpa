package tech.robins.caching

trait Cache[T] {
  def getItems: Iterable[T]

  def add(element: T): Unit

  def contains(element: T): Boolean

  def exists(predicate: T => Boolean): Boolean
}
