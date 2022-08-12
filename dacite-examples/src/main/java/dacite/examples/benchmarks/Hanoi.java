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

/* 2021-11-14 Taken and adjusted from https://github.com/sosy-lab/sv-benchmarks/blob/master/java/jayhorn-recursive/SatHanoi01/Main.java
    : LT */

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Hanoi {

  public int counter; // removed static declaration

  /*
   * This function returns the optimal amount of steps,
   * needed to solve the problem for n-disks
   */
  public int hanoi(int n) {
    if (n == 1) {
      return 1;
    }
    return 2 * (hanoi(n - 1)) + 1;
  }

  /*
   * This applies the known algorithm, without executing it (so no arrays).
   * But the amount of steps is counted in a global variable.
   */
  public void applyHanoi(int n, int from, int to, int via) {
    if (n == 0) {
      return;
    }
    // increment the number of steps
    counter++;
    applyHanoi(n - 1, from, via, to);
    applyHanoi(n - 1, via, to, from);
  }

  public void setCounter(int i) {
    this.counter = i;
  }

  public int getCounter() {
    return this.counter;
  }

  @Test
  public void testHanoi() {
    counter = 0;
    applyHanoi(5, 1, 3, 2);
    int result = hanoi(5);
    assertEquals(result, counter);
  }
}
