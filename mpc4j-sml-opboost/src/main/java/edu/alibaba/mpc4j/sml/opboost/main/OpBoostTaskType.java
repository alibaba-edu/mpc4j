package edu.alibaba.mpc4j.sml.opboost.main;

/**
 * OpBoost任务类型。
 *
 * @author Weiran Liu
 * @date 2022/5/5
 */
public enum OpBoostTaskType {
    /**
     * 回归OpGradBoost
     */
    REG_OP_GRAD_BOOST,
    /**
     * 分类OpGradBoost
     */
    CLS_OP_GRAD_BOOST,
    /**
     * 回归OpXgBoost
     */
    REG_OP_XG_BOOST,
    /**
     * 分类OpXgBoost
     */
    CLS_OP_XG_BOOST,
    /**
     * 加权Kendall系数
     */
    WEIGHTED_KENDALL,
    /**
     * 分类过拟合OpXgBoost
     */
    CLS_OVERFIT_OP_XG_BOOST,
}
