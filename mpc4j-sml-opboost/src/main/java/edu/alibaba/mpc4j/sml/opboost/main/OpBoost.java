package edu.alibaba.mpc4j.sml.opboost.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * OpBoost接口。
 *
 * @author Weiran Liu
 * @date 2022/7/1
 */
public interface OpBoost {
    /**
     * 初始化OpBoost。
     */
    void init() throws IOException, URISyntaxException;

    /**
     * 执行OpBoost。
     */
    void run() throws IOException, XGBoostError, MpcAbortException;
}
