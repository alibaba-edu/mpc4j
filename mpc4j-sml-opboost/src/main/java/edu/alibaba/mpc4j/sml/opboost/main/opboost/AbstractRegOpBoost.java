package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * 回归OpBoost抽象类。
 *
 * @author Weiran Liu
 * @date 2022/7/1
 */
public abstract class AbstractRegOpBoost extends AbstractOpBoost {
    /**
     * 训练数据真实值
     */
    protected double[] trainTruths;
    /**
     * 测试数据真实值
     */
    protected double[] testTruths;

    public AbstractRegOpBoost(Properties properties, OpBoostTaskType taskType) {
        super(properties, taskType);
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        super.init();
        trainTruths = formula.y(trainDataFrame).toDoubleArray();
        testTruths = formula.y(testDataFrame).toDoubleArray();
    }
}
