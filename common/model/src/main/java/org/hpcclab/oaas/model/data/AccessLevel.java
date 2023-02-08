package org.hpcclab.oaas.model.data;

public enum AccessLevel {
  /**
   * root access
   */
  ALL(0),
  /**
   * access from the same package
   */
  INTERNAL(1),
  /**
   * access from its dependency
   */
  INVOKE_DEP(2),
  /**
   * other access
   */
  UNIDENTIFIED(3);
  private final int level;

  AccessLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public static AccessLevel fromLevel(int level) {
    if (level == 0) return ALL;
    if (level == 1) return INTERNAL;
    if (level == 2) return INVOKE_DEP;
    if (level == 3) return UNIDENTIFIED;
    throw new IllegalArgumentException("Unknown level");
  }
}
