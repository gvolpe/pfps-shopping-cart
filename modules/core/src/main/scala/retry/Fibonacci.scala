package retry

object Fibonacci {
  def fibonacci(n: Int): Long =
    if (n > 0)
      fib(n)._1
    else
      0

  // "Fast doubling" Fibonacci algorithm.
  // See e.g. http://funloop.org/post/2017-04-14-computing-fibonacci-numbers.html for explanation.
  private def fib(n: Int): (Long, Long) = n match {
    case 0 => (0, 1)
    case m =>
      val (a, b) = fib(m / 2)
      val c      = a * (b * 2 - a)
      val d      = a * a + b * b
      if (n % 2 == 0)
        (c, d)
      else
        (d, c + d)
  }
}
