package edu.alibaba.mpc4j.sml.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.sml.smile.classification.GradientTreeBoost;
import smile.data.DataFrame;
import smile.data.formula.Formula;

/**
 * 分类OpGradBoost主机线程。
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
public class OpGbdtClsHostThread extends Thread {
    /**
     * 主机
     */
    private final ClsOpGradBoostHost host;
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
    private final ClsOpGradBoostHostConfig hostConfig;
    /**
     * 模型训练结果
     */
    private GradientTreeBoost model;

    OpGbdtClsHostThread(ClsOpGradBoostHost host, Formula formula, DataFrame hostDataFrame, ClsOpGradBoostHostConfig hostConfig) {
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
