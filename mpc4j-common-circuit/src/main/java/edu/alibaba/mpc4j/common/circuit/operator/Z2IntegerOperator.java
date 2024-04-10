package edu.alibaba.mpc4j.common.circuit.operator;

/**
 * Integer Operator.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public enum Z2IntegerOperator {
    /**
     * ≤
     */
    LEQ,
    /**
     * -
     */
    SUB,
    /**
     * ++
     */
    INCREASE_ONE,
    /**
     * +
     */
    ADD,
    /**
     * *
     */
    MUL,
    /**
     * ==
     */
    EQ,
    /**
     * sort
     */
    SORT,
    /**
     * psort
     */
    P_SORT,
}
