package tech.robins

class FixedSizeRoundRobinCache[T](size: Int) {
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
}
