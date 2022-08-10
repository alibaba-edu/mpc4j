package edu.alibaba.mpc4j.sml.opboost.xgboost;

import java.util.Map;

/**
 * XgBoost参数接口。
 *
 * @author Weiran Liu
 * @date 2022/6/29
 */
public interface XgBoostParams {
    /**
     * 返回配置参数。
     *
     * @return 配置参数。
     */
    Map<String, Object> getParams();

    /**
     * 返回树数量。
     *
     * @return 树数量。
     */
    int getTreeNum();
}
