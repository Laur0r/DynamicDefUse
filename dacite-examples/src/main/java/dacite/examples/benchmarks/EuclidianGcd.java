package dacite.examples.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EuclidianGcd {
  public int egcd(int a, int b) {
    if (a == 0) {
      return b;
    }

    while (b != 0) {
      if (a > b) {
        a = a - b;
      } else {
        b = b - a;
      }
    }

    return a;
  }

  @Test
  public void testGCD() {
    int i = egcd(94, 530);
    assertEquals(2, i);
    int i2 = egcd(940, 530);
    assertEquals(10, i2);
    int i3 = egcd(4, 4);
    assertEquals(4, i3);
    int i4 = egcd(0, 2);
    assertEquals(2, i4);
  }
}
