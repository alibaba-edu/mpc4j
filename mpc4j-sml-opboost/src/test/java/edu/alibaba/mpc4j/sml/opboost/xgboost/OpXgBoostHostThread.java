package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import smile.data.DataFrame;
import smile.data.formula.Formula;

/**
 * 分类OpXgBoost主机线程。
 *
 * @author Weiran Liu
 * @date 2021/10/09
 */
public class OpXgBoostHostThread extends Thread {
    /**
     * 主机
     */
    private final OpXgBoostHost host;
    /**
     * 特征
     */
    private final Formula formula;
    /**
     * 主机训练数据
     */
    private final DataFrame hostDataFrame;
    /**
     * 主机配置参数
     */
    private final OpXgBoostHostConfig hostConfig;
    /**
     * 模型训练结果
     */
    private Predictor model;

    OpXgBoostHostThread(OpXgBoostHost host, Formula formula, DataFrame hostDataFrame, OpXgBoostHostConfig hostConfig) {
        this.host = host;
        this.formula = formula;
        this.hostDataFrame = hostDataFrame;
        this.hostConfig = hostConfig;
    }

    Predictor getModel() {
        return model;
    }

    @Override
    public void run() {
        try {
            model = host.fit(formula, hostDataFrame, hostConfig);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
