package org.pipifax

object Utils {

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

  implicit class MyRichString(val s: String) extends AnyVal{
    def padLeft(minLength: Int, ch: Char): String = {
      if (s.length >= minLength) s
      else {
        val out = new Array[Char](minLength)


        val toPad = minLength - s.length
        var index = 0
        while (index < toPad) {
          out(index) = ch
          index += 1
        }
        s.getChars(0, s.length, out, toPad)

        return new String(out)
      }
    }
  }
}