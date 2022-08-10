package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.sml.opboost.OpBoostUtils;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * 分类OpBoost抽象类。
 *
 * @author Weiran Liu
 * @date 2022/7/1
 */
public abstract class AbstractClsOpBoost extends AbstractOpBoost {
    /**
     * 分类数量
     */
    protected int numClass;
    /**
     * 训练数据真实值
     */
    protected int[] trainTruths;
    /**
     * 测试数据真实值
     */
    protected int[] testTruths;

    public AbstractClsOpBoost(Properties properties, OpBoostTaskType taskType) {
        super(properties, taskType);
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        super.init();
        numClass = OpBoostUtils.getNumClass(formula, trainDataFrame);
        Preconditions.checkArgument(numClass > 1, "Num of Class must be greater than 1: %s", numClass);
        trainTruths = formula.y(trainDataFrame).toIntArray();
        testTruths = formula.y(testDataFrame).toIntArray();
    }
}
