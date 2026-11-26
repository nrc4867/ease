package dev.chieppa.ease.math;

/**
 * Sine, cosine and tangent that give the same bits on every JVM and allocate nothing. The argument
 * is reduced by a multiple of a quarter turn into [-pi/4, pi/4] with plain double arithmetic, where
 * {@code StrictMath} evaluates without the reduction step that allocates; arguments too large to
 * reduce that way go to {@code StrictMath} directly.
 */
public final class Trig {
  private static final double QUARTER_TURN = Math.PI / 2;

  /** Past this, reducing by a double quarter turn loses too many bits; StrictMath reduces. */
  private static final double REDUCIBLE = 1.0e6;

  /** Taylor coefficients 1/n! for odd n from 3 (sine) and even n from 2 (cosine). */
  private static final double S3 = 0.16666666666666666;

  private static final double S5 = 0.008333333333333333;
  private static final double S7 = 0.0001984126984126984;
  private static final double S9 = 2.7557319223985893e-06;
  private static final double S11 = 2.505210838544172e-08;
  private static final double S13 = 1.6059043836821613e-10;
  private static final double S15 = 7.647163731819816e-13;
  private static final double C2 = 0.5;
  private static final double C4 = 0.041666666666666664;
  private static final double C6 = 0.001388888888888889;
  private static final double C8 = 2.48015873015873e-05;
  private static final double C10 = 2.755731922398589e-07;
  private static final double C12 = 2.08767569878681e-09;
  private static final double C14 = 1.1470745597729725e-11;
  private static final double C16 = 4.779477332387385e-14;

  private Trig() {}

  /** The sine of {@code r}, for |r| at most pi/4. */
  private static double sinNear(double r) {
    double z = r * r;
    double series = S13 - z * S15;
    series = S11 - z * series;
    series = S9 - z * series;
    series = S7 - z * series;
    series = S5 - z * series;
    series = S3 - z * series;
    return r - r * z * series;
  }

  /** The cosine of {@code r}, for |r| at most pi/4. */
  private static double cosNear(double r) {
    double z = r * r;
    double series = C14 - z * C16;
    series = C12 - z * series;
    series = C10 - z * series;
    series = C8 - z * series;
    series = C6 - z * series;
    series = C4 - z * series;
    series = C2 - z * series;
    return 1 - z * series;
  }

  /** The sine of {@code x} radians. */
  public static double sin(double x) {
    if (!(Math.abs(x) < REDUCIBLE)) {
      return StrictMath.sin(x);
    }
    double turns = Math.rint(x / QUARTER_TURN);
    double r = x - turns * QUARTER_TURN;
    return switch (quadrant(turns)) {
      case 0 -> sinNear(r);
      case 1 -> cosNear(r);
      case 2 -> -sinNear(r);
      default -> -cosNear(r);
    };
  }

  /** The cosine of {@code x} radians. */
  public static double cos(double x) {
    if (!(Math.abs(x) < REDUCIBLE)) {
      return StrictMath.cos(x);
    }
    double turns = Math.rint(x / QUARTER_TURN);
    double r = x - turns * QUARTER_TURN;
    return switch (quadrant(turns)) {
      case 0 -> cosNear(r);
      case 1 -> -sinNear(r);
      case 2 -> -cosNear(r);
      default -> sinNear(r);
    };
  }

  /** The tangent of {@code x} radians. */
  public static double tan(double x) {
    if (!(Math.abs(x) < REDUCIBLE)) {
      return StrictMath.tan(x);
    }
    double turns = Math.rint(x / QUARTER_TURN);
    double r = x - turns * QUARTER_TURN;
    double sin = sinNear(r);
    double cos = cosNear(r);
    return quadrant(turns) % 2 == 0 ? sin / cos : -cos / sin;
  }

  /** Which quarter turn, 0 to 3, a whole number of quarter turns lands in. */
  private static int quadrant(double turns) {
    return (int) (((long) turns % 4 + 4) % 4);
  }
}
