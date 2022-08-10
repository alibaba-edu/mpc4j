package edu.alibaba.mpc4j.sml.opboost.main.overfit;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractOpBoost;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoost;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostMainUtils;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.type.StructType;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 过拟合测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/7/21
 */
abstract class AbstractOverfitOpXgBoost implements OpBoost {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpBoost.class);
    /**
     * 配置参数
     */
    protected final Properties properties;
    /**
     * 任务类型
     */
    protected final OpBoostTaskType taskType;
    /**
     * 数据集名称
     */
    protected String datasetName;
    /**
     * 元数据信息
     */
    protected StructType schema;
    /**
     * 标签
     */
    protected Formula formula;
    /**
     * 所有训练数据
     */
    protected DataFrame trainDataFrame;
    /**
     * 测试数据
     */
    protected DataFrame testDataFrame;
    /**
     * 训练特征数据
     */
    protected DataFrame trainFeatureDataFrame;
    /**
     * 测试特征数据
     */
    protected DataFrame testFeatureDataFrame;
    /**
     * the number of iterations (trees)
     */
    protected int treeNum;
    /**
     * the maximum depth of the tree
     */
    protected int maxDepth;
    /**
     * the shrinkage parameter in (0, 1] controls the learning rate of procedure.
     */
    protected double shrinkage;
    /**
     * 测试轮数
     */
    protected int totalRound;
    /**
     * LDP列映射
     */
    protected Map<String, Boolean> ldpColumnsMap;
    /**
     * 差分隐私参数ε
     */
    protected double[] epsilons;
    /**
     * 分区长度θ
     */
    protected int[] thetas;
    /**
     * 划分比例α
     */
    protected double[] alphas;

    public AbstractOverfitOpXgBoost(Properties properties, OpBoostTaskType taskType) {
        this.properties = properties;
        this.taskType = taskType;
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        // 设置数据集名称
        datasetName = OpBoostMainUtils.setDatasetName(properties);
        // 设置标签
        formula = OpBoostMainUtils.setFormula(properties);
        // 设置数据格式
        schema = OpBoostMainUtils.setSchema(properties, formula);
        // 读取训练和测试集，并调整顺序
        setDataSet();
        // 设置机器学习参数
        setMachineLearningParameters();
        // 设置总测试轮数
        totalRound = OpBoostMainUtils.setTotalRound(properties);
        // 设置LDP列
        ldpColumnsMap = OpBoostMainUtils.setLdpColumnsMap(properties, schema);
        // 设置LDP参数
        setLdpParameters();
    }

    private void setDataSet() throws IOException, URISyntaxException {
        LOGGER.info("-----set whole dataset-----");
        trainDataFrame = OpBoostMainUtils.setTrainDataFrame(properties, schema);
        testDataFrame = OpBoostMainUtils.setTestDataFrame(properties, schema);
        trainFeatureDataFrame = formula.x(trainDataFrame);
        testFeatureDataFrame = formula.x(testDataFrame);
    }

    private void setMachineLearningParameters() {
        LOGGER.info("-----set machine learning parameters-----");
        treeNum = Integer.parseInt(properties.getProperty("tree_num", "100"));
        maxDepth = Integer.parseInt(properties.getProperty("max_depth", "3"));
        shrinkage = Double.parseDouble(properties.getProperty("shrinkage", "0.1"));
    }

    protected void setLdpParameters() {
        LOGGER.info("-----set LDP parameters-----");
        // 设置ε
        epsilons = OpBoostMainUtils.setEpsilons(properties);
        // 设置θ
        thetas = OpBoostMainUtils.setThetas(properties);
        // 设置α
        alphas = OpBoostMainUtils.setAlphas(properties);
    }

    /**
     * 创建LDP配置项。
     *
     * @param ldpType LDP类型。
     * @param epsilon 差分隐私参数ε。
     * @return LDP配置项。
     */
    protected Map<String, LdpConfig> createLdpConfigs(OpBoostLdpType ldpType, double epsilon) {
        return OpBoostMainUtils.createLdpConfigs(trainDataFrame, ldpColumnsMap, ldpType, epsilon);
    }

    /**
     * 创建LDP配置项。
     *
     * @param ldpType LDP类型。
     * @param epsilon 差分隐私参数ε。
     * @param theta   分区长度θ。
     * @return LDP配置项。
     */
    protected Map<String, LdpConfig> createLdpConfigs(OpBoostLdpType ldpType, double epsilon, int theta) {
        return OpBoostMainUtils.createLdpConfigs(trainDataFrame, ldpColumnsMap, ldpType, epsilon, theta);
    }

    /**
     * 创建LDP配置项。
     *
     * @param ldpType LDP类型。
     * @param epsilon 差分隐私参数ε。
     * @param theta   分区长度θ。
     * @param alpha   划分比例α。
     */
    protected Map<String, LdpConfig> createLdpConfigs(OpBoostLdpType ldpType, double epsilon, int theta, double alpha) {
        return OpBoostMainUtils.createLdpConfigs(trainDataFrame, ldpColumnsMap, ldpType, epsilon, theta, alpha);
    }

    protected void writeInfo(PrintWriter printWriter,
                             String name, Double epsilon, Integer theta, Double alpha, int treeNum,
                             Double ldpTrainMeasure, double trainMeasure, double testMeasure) {
        String information = name
            // ε
            + "\t" + (Objects.isNull(epsilon) ? "N/A" : epsilon)
            // θ
            + "\t" + (Objects.isNull(theta) ? "N/A" : theta)
            // α
            + "\t" + (Objects.isNull(alpha) ? "N/A" : alpha)
            // 树数量
            + "\t" + treeNum
            // LDP训练数据集度量值
            + "\t" + (Objects.isNull(ldpTrainMeasure) ? "N/A" : ldpTrainMeasure)
            // 训练数据集度量值
            + "\t" + trainMeasure
            // 测试数据集度量值
            + "\t" + testMeasure;
        printWriter.println(information);
    }
}
