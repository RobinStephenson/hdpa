package tech.robins

trait Cache[T] {
  def add(element: T): Unit

  def contains(element: T): Boolean
}
