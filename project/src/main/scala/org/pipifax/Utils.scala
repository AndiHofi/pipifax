package org.pipifax

import java.nio.file.{Paths, Path}

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

  implicit class MyRichString(val s: String) extends AnyVal {
    def padLeft(minLength: Int, ch: Char): String = {
      if (s.length >= minLength) {
        s
      }
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

    def nioPath: Path = Paths.get(s)
  }

  implicit class MyRichPath(val p: Path) extends AnyVal {
    def toUnixPath: String = Utils.toUnixPath(p)

    def /(child: Path): Path = p resolve child

    def /(child: String): Path = p resolve child
  }


  def toUnixPath(path: Path): String = {
    if (path.getNameCount == 0) {
      "/"
    } else {
      val normalizedIter = path.normalize().iterator()
      val sb = new StringBuilder
      if (path.getRoot != null) sb.append(path.getRoot.toString)
      if (normalizedIter.hasNext) {
        sb.append(normalizedIter.next().toString)
        while (normalizedIter.hasNext) {
          sb.append('/').append(normalizedIter.next())
        }
        if (sb.isEmpty) "."
        else sb.toString
      } else {
        "."
      }
    }
  }
}