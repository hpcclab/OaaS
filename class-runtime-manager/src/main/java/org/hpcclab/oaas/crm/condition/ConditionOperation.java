package org.hpcclab.oaas.crm.condition;

import java.math.BigDecimal;
import java.util.Objects;

public enum ConditionOperation {
  IS_NULL,
  NOT_NULL,
  GT,
  GTE,
  EQ,
  NEQ,
  LT,
  LTE;

  public boolean check(BigDecimal target, String arg) {
    BigDecimal val = null;
    try {
      val = new BigDecimal(arg);
    } catch (Exception ignored) {
    }
    return check(target, val);
  }

  public boolean check(Number target, String arg) {
    BigDecimal val = null;
    try {
      val = new BigDecimal(arg);
    } catch (Exception ignored) {
    }
    BigDecimal dTarget = null;
    if (target!=null)
      dTarget = new BigDecimal(target.toString());
    return check(dTarget, val);
  }

  private boolean check(BigDecimal target, BigDecimal val) {
    return switch (this) {
      case IS_NULL -> target==null;
      case NOT_NULL -> target!=null;
      case EQ -> Objects.equals(target, val);
      case NEQ -> !Objects.equals(target, val);
      case GT -> val!=null && target!=null && target.compareTo(val) > 0;
      case GTE -> val!=null && target!=null && target.compareTo(val) >= 0;
      case LT -> val!=null && target!=null && target.compareTo(val) < 0;
      case LTE -> val!=null && target!=null && target.compareTo(val) <= 0;
    };
  }

  public boolean check(String target, String arg) {
    return switch (this) {
      case IS_NULL -> target==null;
      case NOT_NULL -> target!=null;
      case EQ -> Objects.equals(target, arg);
      case NEQ -> !Objects.equals(target, arg);
      default -> false;
    };
  }
  public boolean check(Boolean target, String arg) {
    return switch (this) {
      case IS_NULL -> target==null;
      case NOT_NULL -> target!=null;
      case EQ -> Objects.equals(target, parseBool(arg));
      case NEQ -> !Objects.equals(target, parseBool(arg));
      default -> false;
    };
  }

  public boolean parseBool(String arg) {
    if (arg == null) return false;
    if (arg.equalsIgnoreCase("y")) return true;
    if (arg.equalsIgnoreCase("yes")) return true;
    return arg.equalsIgnoreCase("true");
  }

}
