package org.pipifax.monitor

package object utils {

  /**
   * Implementation of the single resource "try-with-resources" statement of Java7 in scala.
   *
   * @param resource The automatically closable resource
   * @param f The function to call with the resource as parameter
   * @tparam A The actual resource type
   * @tparam B The result type of f
   * @return the return value of f
   * @throws NullPointerException when resource is null.
   */
  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    if (resource eq null) throw new NullPointerException()

    val result: B =
      try {
        f(resource)
      } catch {
        case th: Throwable =>
          try {
            resource.close()
          } catch {
            case onClose: Throwable =>
              th.addSuppressed(onClose)
          }
          throw th
      }

    resource.close()

    result
  }
}


