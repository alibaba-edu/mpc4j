package edu.alibaba.mpc4j.work.db.dynamic.structure;

/**
 * @author Feng Han
 * @date 2025/3/7
 */
public enum MaterializedTableType {
    GROUP_BY_MT,
    ORDER_BY_MT,
    SELECT_MT,
    PK_PK_JOIN_MT,
    JOIN_INPUT_MT,
    GLOBAL_AGG_MT
}
