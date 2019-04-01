package tech.robins.caching

class FixedSizeRoundRobinCache[T](size: Int) extends Cache[T] {
  private val elements: Array[Option[T]] = Array.fill(size) { None }
  private var currentAdditionIndex: Int = 0

  private def shiftCurrentAdditionIndex(): Unit = {
    currentAdditionIndex += 1
    if (currentAdditionIndex >= elements.length) currentAdditionIndex = 0
  }

  def add(element: T): Unit = {
    // Check if there is a free slot, if so use it, otherwise replace by round robin
    elements.indexOf(None) match {
      case -1 =>
        elements.update(currentAdditionIndex, Some(element))
        shiftCurrentAdditionIndex()
      case emptySlot =>
        elements.update(emptySlot, Some(element))
        // if the empty slot is the current addition index we still need to shift
        if (emptySlot == currentAdditionIndex) shiftCurrentAdditionIndex()
    }
  }

  def removeIfPresent(element: T): Boolean = {
    val elementIndex = elements.indexOf(Some(element))
    if (elementIndex != -1) {
      elements.update(elementIndex, None)
      true
    } else {
      false
    }
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
