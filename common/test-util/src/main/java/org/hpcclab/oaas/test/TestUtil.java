package org.hpcclab.oaas.test;

import java.util.function.BooleanSupplier;

public class TestUtil {

  public static boolean retryTillConditionMeet(BooleanSupplier condition) throws InterruptedException {
    return retryTillConditionMeet(condition, 20, 500);
  }

  public static boolean retryTillConditionMeet(BooleanSupplier condition,
                                               int max,
                                               int delay)
    throws InterruptedException {
    boolean check;
    do {
      check = condition.getAsBoolean();
      max--;
      Thread.sleep(delay);
    }
    while (!check && max > 0);
    return check;
  }
}
