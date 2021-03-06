package org.basex.query.expr;

import static org.basex.query.QueryError.*;
import static org.basex.query.value.type.AtomType.*;

import java.math.*;
import java.util.function.*;

import org.basex.query.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Calculation.
 *
 * @author BaseX Team 2005-18, BSD License
 * @author Christian Gruen
 */
public enum Calc {
  /** Addition. */
  PLUS("+") {
    @Override
    public Item eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      final Type type1 = item1.type, type2 = item2.type;
      final boolean num1 = type1.isNumberOrUntyped(), num2 = type2.isNumberOrUntyped();
      if(num1 ^ num2) throw numberError(num1 ? item2 : item1, info);

      if(num1) {
        // numbers or untyped values
        final Type type = type(type1, type2);
        if(type == ITR) {
          final long itr1 = item1.itr(info), itr2 = item2.itr(info);
          if(itr2 > 0 ? itr1 > Long.MAX_VALUE - itr2 : itr1 < Long.MIN_VALUE - itr2)
            throw RANGE_X.get(info, itr1 + " + " + itr2);
          return Int.get(itr1 + itr2);
        }
        if(type == DBL) return Dbl.get(item1.dbl(info) + item2.dbl(info));
        if(type == FLT) return Flt.get(item1.flt(info) + item2.flt(info));
        return Dec.get(item1.dec(info).add(item2.dec(info)));
      }

      // dates or durations
      if(type1 == type2) {
        if(!(item1 instanceof Dur)) throw numberError(item1, info);
        if(type1 == YMD) return new YMDur((YMDur) item1, (YMDur) item2, true, info);
        if(type1 == DTD) return new DTDur((DTDur) item1, (DTDur) item2, true, info);
      }
      if(type1 == DTM) return new Dtm((Dtm) item1, dur(info, item2), true, info);
      if(type2 == DTM) return new Dtm((Dtm) item2, dur(info, item1), true, info);
      if(type1 == DAT) return new Dat((Dat) item1, dur(info, item2), true, info);
      if(type2 == DAT) return new Dat((Dat) item2, dur(info, item1), true, info);
      if(type1 == TIM && type2 == DTD) return new Tim((Tim) item1, (DTDur) item2, true);
      if(type2 == TIM && type1 == DTD) return new Tim((Tim) item2, (DTDur) item1, true);
      throw typeError(info, type1, type2);
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      // check for neutral numbers
      final BiFunction<Expr, Expr, Expr> func = (expr1, expr2) ->
        expr1 instanceof ANum && ((ANum) expr1).dbl() == 0 ? expr2 : null;
      final Expr expr = func.apply(ex1, ex2);
      return expr != null ? expr : func.apply(ex2, ex1);
    }
  },

  /** Subtraction. */
  MINUS("-") {
    @Override
    public Item eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      final Type type1 = item1.type, type2 = item2.type;
      final boolean num1 = type1.isNumberOrUntyped(), num2 = type2.isNumberOrUntyped();
      if(num1 ^ num2) throw numberError(num1 ? item2 : item1, info);

      if(num1) {
        // numbers or untyped values
        final Type type = type(type1, type2);
        if(type == ITR) {
          final long itr1 = item1.itr(info), itr2 = item2.itr(info);
          if(itr2 < 0 ? itr1 > Long.MAX_VALUE + itr2 : itr1 < Long.MIN_VALUE + itr2)
            throw RANGE_X.get(info, itr1 + " - " + itr2);
          return Int.get(itr1 - itr2);
        }
        if(type == DBL) return Dbl.get(item1.dbl(info) - item2.dbl(info));
        if(type == FLT) return Flt.get(item1.flt(info) - item2.flt(info));
        return Dec.get(item1.dec(info).subtract(item2.dec(info)));
      }

      // dates or durations
      if(type1 == type2) {
        if(type1 == DTM || type1 == DAT || type1 == TIM)
          return new DTDur((ADate) item1, (ADate) item2, info);
        if(type1 == YMD) return new YMDur((YMDur) item1, (YMDur) item2, false, info);
        if(type1 == DTD) return new DTDur((DTDur) item1, (DTDur) item2, false, info);
        throw numberError(item1, info);
      }
      if(type1 == DTM) return new Dtm((Dtm) item1, dur(info, item2), false, info);
      if(type1 == DAT) return new Dat((Dat) item1, dur(info, item2), false, info);
      if(type1 == TIM && type2 == DTD) return new Tim((Tim) item1, (DTDur) item2, false);
      throw typeError(info, type1, type2);
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      // check for neutral number and identical arguments
      return ex2 instanceof ANum && ((ANum) ex2).dbl() == 0 ? ex1 :
        ex1.equals(ex2) ? zero(ex1) : null;
    }
  },

  /** Multiplication. */
  MULT("*") {
    @Override
    public Item eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      final Type type1 = item1.type, type2 = item2.type;
      if(type1 == YMD) {
        if(item2 instanceof ANum) return new YMDur((Dur) item1, item2.dbl(info), true, info);
        throw numberError(item2, info);
      }
      if(type2 == YMD) {
        if(item1 instanceof ANum) return new YMDur((Dur) item2, item1.dbl(info), true, info);
        throw numberError(item1, info);
      }
      if(type1 == DTD) {
        if(item2 instanceof ANum) return new DTDur((Dur) item1, item2.dbl(info), true, info);
        throw numberError(item2, info);
      }
      if(type2 == DTD) {
        if(item1 instanceof ANum) return new DTDur((Dur) item2, item1.dbl(info), true, info);
        throw numberError(item1, info);
      }

      final boolean num1 = type1.isNumberOrUntyped(), num2 = type2.isNumberOrUntyped();
      if(num1 ^ num2) throw typeError(info, type1, type2);
      if(num1) {
        final Type type = type(type1, type2);
        if(type == ITR) {
          final long l1 = item1.itr(info);
          final long l2 = item2.itr(info);
          if(l2 > 0 ? l1 > Long.MAX_VALUE / l2 || l1 < Long.MIN_VALUE / l2
                    : l2 < -1 ? l1 > Long.MIN_VALUE / l2 || l1 < Long.MAX_VALUE / l2
                              : l2 == -1 && l1 == Long.MIN_VALUE)
            throw RANGE_X.get(info, l1 + " * " + l2);
          return Int.get(l1 * l2);
        }
        if(type == DBL) return Dbl.get(item1.dbl(info) * item2.dbl(info));
        if(type == FLT) return Flt.get(item1.flt(info) * item2.flt(info));
        return Dec.get(item1.dec(info).multiply(item2.dec(info)));
      }
      throw numberError(item1, info);
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      // check for absorbing and neutral numbers
      final BiFunction<Expr, Expr, Expr> func = (expr1, expr2) -> {
        final double dbl1 = expr1 instanceof ANum ? ((ANum) expr1).dbl() : Double.NaN;
        return dbl1 == 1 ? expr2 : dbl1 == 0 ? zero(expr1) : null;
      };
      final Expr expr = func.apply(ex1, ex2);
      return expr != null ? expr : func.apply(ex2, ex1);
    }
  },

  /** Division. */
  DIV("div") {
    @Override
    public Item eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      final Type type1 = item1.type, type2 = item2.type;
      if(type1 == type2) {
        if(type1 == YMD) {
          final BigDecimal bd = BigDecimal.valueOf(((YMDur) item2).ymd());
          if(bd.doubleValue() == 0.0) throw zeroError(info, item1);
          return Dec.get(BigDecimal.valueOf(((YMDur) item1).ymd()).
              divide(bd, MathContext.DECIMAL64));
        }
        if(type1 == DTD) {
          final BigDecimal bd = ((DTDur) item2).dtd();
          if(bd.doubleValue() == 0.0) throw zeroError(info, item1);
          return Dec.get(((DTDur) item1).dtd().divide(bd, MathContext.DECIMAL64));
        }
      }
      if(type1 == YMD) {
        if(item2 instanceof ANum) return new YMDur((Dur) item1, item2.dbl(info), false, info);
        throw numberError(item2, info);
      }
      if(type1 == DTD) {
        if(item2 instanceof ANum) return new DTDur((Dur) item1, item2.dbl(info), false, info);
        throw numberError(item2, info);
      }

      checkNum(info, item1, item2);
      final Type type = type(type1, type2);
      if(type == DBL) return Dbl.get(item1.dbl(info) / item2.dbl(info));
      if(type == FLT) return Flt.get(item1.flt(info) / item2.flt(info));

      final BigDecimal dec1 = item1.dec(info), dec2 = item2.dec(info);
      if(dec2.signum() == 0) throw zeroError(info, item1);
      final int scale = Math.max(18, Math.max(dec1.scale(), dec2.scale()));
      return Dec.get(dec1.divide(dec2, scale, RoundingMode.HALF_EVEN));
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      // check for neutral number and identical arguments
      return ex2 instanceof ANum && ((ANum) ex2).dbl() == 1 ? ex1 :
        ex1.equals(ex2) ? one(ex1) : null;
    }
  },

  /** Integer division. */
  IDIV("idiv") {
    @Override
    public Int eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      checkNum(info, item1, item2);
      final Type type = type(item1.type, item2.type);
      if(type == DBL || type == FLT) {
        final double dbl1 = item1.dbl(info), dbl2 = item2.dbl(info);
        if(dbl2 == 0) throw zeroError(info, item1);
        final double dbl = dbl1 / dbl2;
        if(Double.isNaN(dbl) || Double.isInfinite(dbl))
          throw DIVFLOW_X.get(info, dbl1 + " idiv " + dbl2);
        if(dbl < Long.MIN_VALUE || dbl > Long.MAX_VALUE)
          throw RANGE_X.get(info, dbl1 + " idiv " + dbl2);
        return Int.get((long) dbl);
      }

      if(type == ITR) {
        final long itr1 = item1.itr(info), itr2 = item2.itr(info);
        if(itr2 == 0) throw zeroError(info, item1);
        if(itr1 == Integer.MIN_VALUE && itr2 == -1) throw RANGE_X.get(info, itr1 + " idiv " + itr2);
        return Int.get(itr1 / itr2);
      }

      final BigDecimal dec1 = item1.dec(info), dec2 = item2.dec(info);
      if(dec2.signum() == 0) throw zeroError(info, item1);
      final BigDecimal res = dec1.divideToIntegralValue(dec2);
      if(!(MIN_LONG.compareTo(res) <= 0 && res.compareTo(MAX_LONG) <= 0))
        throw RANGE_X.get(info, dec1 + " idiv " + dec2);
      return Int.get(res.longValueExact());
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      // check for neutral number and identical arguments
      return ex2 instanceof ANum && ((ANum) ex2).dbl() == 1 ? ex1 :
        ex1.equals(ex2) ? one(ex1) : null;
    }
  },

  /** Modulo. */
  MOD("mod") {
    @Override
    public Item eval(final Item item1, final Item item2, final InputInfo info)
        throws QueryException {
      checkNum(info, item1, item2);
      final Type type = type(item1.type, item2.type);
      if(type == DBL) return Dbl.get(item1.dbl(info) % item2.dbl(info));
      if(type == FLT) return Flt.get(item1.flt(info) % item2.flt(info));
      if(type == ITR) {
        final long itr1 = item1.itr(info), itr2 = item2.itr(info);
        if(itr2 == 0) throw zeroError(info, item1);
        return Int.get(itr1 % itr2);
      }

      final BigDecimal dec1 = item1.dec(info), dec2 = item2.dec(info);
      if(dec2.signum() == 0) throw zeroError(info, item1);
      final BigDecimal sub = dec1.divide(dec2, 0, RoundingMode.DOWN);
      return Dec.get(dec1.subtract(sub.multiply(dec2)));
    }

    @Override
    public Expr optimize(final Expr ex1, final Expr ex2) throws QueryException {
      return null;
    }
  };

  /** {@link Long#MIN_VALUE} as a {@link BigDecimal}. */
  private static final BigDecimal MIN_LONG = BigDecimal.valueOf(Long.MIN_VALUE);
  /** {@link Long#MAX_VALUE} as a {@link BigDecimal}. */
  private static final BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

  /** Name of operation. */
  final String name;

  /**
   * Constructor.
   * @param name name
   */
  Calc(final String name) {
    this.name = name;
  }

  /**
   * Performs the calculation.
   * @param item1 first item
   * @param item2 second item
   * @param info input info
   * @return result type
   * @throws QueryException query exception
   */
  public abstract Item eval(Item item1, Item item2, InputInfo info) throws QueryException;

  /**
   * Optimizes the expressions.
   * @param ex1 first expression
   * @param ex2 second expression
   * @return result type
   * @throws QueryException query exception
   */
  public abstract Expr optimize(Expr ex1, Expr ex2) throws QueryException;

  /**
   * Returns the numeric type with the highest precedence.
   * @param type1 first item type
   * @param type2 second item type
   * @return type
   */
  public static Type type(final Type type1, final Type type2) {
    if(type1 == DBL || type2 == DBL || type1.isUntyped() || type2.isUntyped()) return DBL;
    if(type1 == FLT || type2 == FLT) return FLT;
    if(type1 == DEC || type2 == DEC) return DEC;
    return ITR;
  }

  /**
   * Tries to rewrite the expression to {@code 0}.
   * @param expr expression
   * @return zero value or {@code null}
   */
  private static Expr zero(final Expr expr) {
    // floating points
    final Type type = expr.seqType().type;
    return type == DEC ? Dec.ZERO : type.instanceOf(AtomType.ITR) ? Int.ZERO : null;
  }

  /**
   * Tries to rewrite the expression to {@code 1}.
   * @param expr expression
   * @return zero value or {@code null}
   */
  private static Expr one(final Expr expr) {
    // floating points
    final Type type = expr.seqType().type;
    return type == DEC ? Dec.ONE : type.instanceOf(AtomType.ITR) ? Int.ONE : null;
  }

  /**
   * Throws a division by zero exception.
   * @param info input info
   * @param item item
   * @return query exception (indicates that an error is raised)
   */
  private static QueryException zeroError(final InputInfo info, final Item item) {
    return DIVZERO_X.get(info, chop(item, info));
  }

  /**
   * Returns a type error.
   * @param info input info
   * @param type1 first type
   * @param type2 second type
   * @return query exception
   */
  final QueryException typeError(final InputInfo info, final Type type1, final Type type2) {
    return CALCTYPE_X_X_X.get(info, info(), type1, type2);
  }

  /**
   * Returns a duration type error.
   * @param info input info
   * @param item item
   * @return duration
   * @throws QueryException query exception
   */
  static Dur dur(final InputInfo info, final Item item) throws QueryException {
    if(item instanceof Dur) {
      if(item.type == DUR) throw NOSUBDUR_X.get(info, item);
      return (Dur) item;
    }
    throw NODUR_X_X.get(info, item.type, item);
  }

  /**
   * Checks if the specified items are numeric or untyped.
   * @param info input info
   * @param item1 first item
   * @param item2 second item
   * @throws QueryException query exception
   */
  static void checkNum(final InputInfo info, final Item item1, final Item item2)
      throws QueryException {
    if(!item1.type.isNumberOrUntyped()) throw numberError(item1, info);
    if(!item2.type.isNumberOrUntyped()) throw numberError(item2, info);
  }

  /**
   * Returns a string representation of the operator.
   * @return string
   */
  final String info() {
    return '\'' + name + "' operator";
  }

  @Override
  public String toString() {
    return name;
  }
}
