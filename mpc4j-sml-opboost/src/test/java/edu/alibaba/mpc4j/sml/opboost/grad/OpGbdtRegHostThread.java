package edu.alibaba.mpc4j.sml.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.sml.smile.regression.GradientTreeBoost;
import smile.data.DataFrame;
import smile.data.formula.Formula;

/**
 * 回归OpGradBoost主机线程。
 *
 * @author Weiran Liu
 * @date 2020/11/27
 */
public class OpGbdtRegHostThread extends Thread {
    /**
     * 主机
     */
    private final RegOpGradBoostHost host;
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
    private final RegOpGradBoostHostConfig hostConfig;
    /**
     * 模型训练结果
     */
    private GradientTreeBoost model;

    OpGbdtRegHostThread(RegOpGradBoostHost host, Formula formula, DataFrame hostDataFrame, RegOpGradBoostHostConfig hostConfig) {
        this.host = host;
        this.formula = formula;
        this.hostDataFrame = hostDataFrame;
        this.hostConfig = hostConfig;
    }

    GradientTreeBoost getModel() {
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
