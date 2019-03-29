package tech.robins.caching

class FixedSizeRoundRobinCache[T](size: Int) extends Cache[T] {
  private val elements: Array[Option[T]] = Array.fill(size) { None }
  private var currentAdditionIndex: Int = 0

  private def shiftCurrentAdditionIndex(): Unit = {
    currentAdditionIndex += 1
    if (currentAdditionIndex >= elements.length) currentAdditionIndex = 0
  }

  def add(element: T): Unit = {
    elements.update(currentAdditionIndex, Some(element))
    shiftCurrentAdditionIndex()
  }

  def contains(element: T): Boolean = elements contains Some(element)

  def getItems: Iterable[T] = {
    val definedElements = elements collect { case Some(elem) => elem }
    definedElements.toIterable
  }

  def exists(predicate: T => Boolean): Boolean = {
    elements.exists({
      case Some(element) => predicate(element)
      case None          => false
    })
  }
}
