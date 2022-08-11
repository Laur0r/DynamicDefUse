package dacite.examples.benchmarks;

/*
The MIT License (MIT)

Copyright (c) 2016 JayHorn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

/* 2021-11-14 Taken and adjusted from https://github.com/sosy-lab/sv-benchmarks/blob/master/java/jayhorn-recursive/SatEvenOdd01/Main.java
    : LT */

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EvenOdd {
  static int isOdd(int n) {
    if (n == 0) {
      return 0;
    } else if (n == 1) {
      return 1;
    } else {
      return isEven(n - 1);
    }
  }

  static int isEven(int n) {
    if (n == 0) {
      return 1;
    } else if (n == 1) {
      return 0;
    } else {
      return isOdd(n - 1);
    }
  }

  @Test
  public void testEvenOdd() {
    int e = isEven(33);
    assertEquals(0, e);
    int o = isOdd(29);
    assertEquals(1, o);
  }
}
