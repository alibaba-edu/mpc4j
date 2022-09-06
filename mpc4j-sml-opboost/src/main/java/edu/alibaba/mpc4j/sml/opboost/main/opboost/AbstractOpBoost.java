package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlave;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveConfig;
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
import java.util.*;
import java.util.stream.IntStream;

/**
 * OpBoost抽象类。
 *
 * @author Weiran Liu
 * @date 2021/10/05
 */
public abstract class AbstractOpBoost implements OpBoost {
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
     * 通信接口
     */
    protected Rpc ownRpc;
    /**
     * 另一个参与方
     */
    protected Party otherParty;
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
     * 测试轮数
     */
    protected int totalRound;
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
     * 自己的训练数据
     */
    protected DataFrame ownDataFrame;
    /**
     * 自己训练数据格式
     */
    protected StructType ownSchema;
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

    public AbstractOpBoost(Properties properties, OpBoostTaskType taskType) {
        this.properties = properties;
        this.taskType = taskType;
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        // 设置通信接口
        ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "host", "slave");
        if (ownRpc.ownParty().getPartyId() == 0) {
            otherParty = ownRpc.getParty(1);
        } else {
            otherParty = ownRpc.getParty(0);
        }
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
        int ncols = schema.length();
        DataFrame readTrainDataFrame = OpBoostMainUtils.setTrainDataFrame(properties, schema);
        DataFrame readTestDataFrame = OpBoostMainUtils.setTestDataFrame(properties, schema);
        LOGGER.info("-----set own dataframe-----");
        int[] partyColumns = PropertiesUtils.readIntArray(properties, "party_columns");
        Preconditions.checkArgument(partyColumns.length == ncols, "# of party_column must match column_num");
        Arrays.stream(partyColumns).forEach(partyId ->
                Preconditions.checkArgument(
                    partyId == 0 || partyId == 1,
                    "Invalid party_column: %s, party_colum must be 0 or 1", partyId)
            );
        int labelIndex = schema.fieldIndex(formula.response().toString());
        Preconditions.checkArgument(
            partyColumns[labelIndex] == 0,
            "label column (%s) must belong to host (with party_column = 0)", labelIndex
        );
        int[] ownColumns = IntStream.range(0, ncols)
            .filter(columnIndex -> partyColumns[columnIndex] == ownRpc.ownParty().getPartyId())
            .toArray();
        Preconditions.checkArgument(
            ownColumns.length > 0,
            "At least one column should belongs to party_id = %s", ownRpc.ownParty().getPartyId()
        );
        LOGGER.info("own_columns = {}", Arrays.toString(ownColumns));
        ownDataFrame = readTrainDataFrame.select(ownColumns);
        ownSchema = ownDataFrame.schema();
        // 挑选列后，数据列会发生变化，因此也需要调整输入列
        trainDataFrame = readTrainDataFrame.select(ownColumns).merge(readTrainDataFrame.drop(ownColumns));
        trainFeatureDataFrame = formula.x(trainDataFrame);
        testDataFrame = readTestDataFrame.select(ownColumns).merge(readTestDataFrame.drop(ownColumns));
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
        return OpBoostMainUtils.createLdpConfigs(ownDataFrame, ldpColumnsMap, ldpType, epsilon);
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
        return OpBoostMainUtils.createLdpConfigs(ownDataFrame, ldpColumnsMap, ldpType, epsilon, theta);
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
        return OpBoostMainUtils.createLdpConfigs(ownDataFrame, ldpColumnsMap, ldpType, epsilon, theta, alpha);
    }

    protected void writeInfo(PrintWriter printWriter,
                             String name, Double epsilon, Integer theta, Double alpha, Double time,
                             Double trainMeasure, Double testMeasure,
                             long packetNum, long payloadByteLength, long sendByteLength) {
        String information = name
            // ε
            + "\t" + (Objects.isNull(epsilon) ? "N/A" : epsilon)
            // θ
            + "\t" + (Objects.isNull(theta) ? "N/A" : theta)
            // α
            + "\t" + (Objects.isNull(alpha) ? "N/A" : alpha)
            // 时间
            + "\t" + (Objects.isNull(time) ? "N/A" : time)
            // 训练数据集度量值
            + "\t" + (Objects.isNull(trainMeasure) ? "N/A" : trainMeasure)
            // 测试数据集度量值
            + "\t" + (Objects.isNull(testMeasure) ? "N/A" : testMeasure)
            // 数据包发送次数
            + "\t" + packetNum
            // 负载量
            + "\t" + payloadByteLength
            // 数据量
            + "\t" + sendByteLength;
        printWriter.println(information);
    }

    protected void runSlavePlainTraining(PrintWriter printWriter) {
        ownRpc.reset();
        LOGGER.info("-----slave skips {} LDP training for {}-----", OpBoostLdpType.PLAIN.name(), taskType);
        writeInfo(printWriter, "PLAIN", null, null, null, null,
            null, null,
            ownRpc.getSendDataPacketNum(), ownRpc.getPayloadByteLength(), ownRpc.getSendByteLength()
        );
        ownRpc.reset();
    }

    protected void runSlaveEpsilonLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType)
        throws MpcAbortException {
        LOGGER.info("-----Slave {} LDP training for {}-----", ldpType.name(), taskType);
        for (double epsilon : epsilons) {
            Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon);
            OpBoostSlave slave = new OpBoostSlave(ownRpc, otherParty);
            OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig.Builder(ownSchema)
                .addLdpConfig(ldpConfigs)
                .build();
            OpBoostSlaveRunner slaveRunner = new OpBoostSlaveRunner(slave, slaveConfig, totalRound, ownDataFrame);
            slaveRunner.run();
            writeInfo(printWriter, ldpType.name(), epsilon, null, null, slaveRunner.getTime(),
                null, null,
                slaveRunner.getPacketNum(), slaveRunner.getPayloadByteLength(), slaveRunner.getSendByteLength()
            );
        }
    }

    protected void runSlaveLocalMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType)
        throws MpcAbortException {
        LOGGER.info("-----Slave {} LDP training for {}-----", ldpType.name(), taskType);
        for (int theta : thetas) {
            for (double epsilon : epsilons) {
                Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon, theta);
                OpBoostSlave slave = new OpBoostSlave(ownRpc, otherParty);
                OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig.Builder(ownSchema)
                    .addLdpConfig(ldpConfigs)
                    .build();
                OpBoostSlaveRunner slaveRunner = new OpBoostSlaveRunner(slave, slaveConfig, totalRound, ownDataFrame);
                slaveRunner.run();
                writeInfo(printWriter, ldpType.name(), epsilon, theta, null, slaveRunner.getTime(),
                    null, null,
                    slaveRunner.getPacketNum(), slaveRunner.getPayloadByteLength(), slaveRunner.getSendByteLength()
                );
            }
        }
    }

    protected void runSlaveAdjMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws MpcAbortException {
        LOGGER.info("-----Slave {} LDP training for {}-----", ldpType.name(), taskType);
        for (double alpha : alphas) {
            for (int theta : thetas) {
                for (double epsilon : epsilons) {
                    Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon, theta, alpha);
                    OpBoostSlave slave = new OpBoostSlave(ownRpc, otherParty);
                    OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig.Builder(ownSchema)
                        .addLdpConfig(ldpConfigs)
                        .build();
                    OpBoostSlaveRunner slaveRunner = new OpBoostSlaveRunner(slave, slaveConfig, totalRound, ownDataFrame);
                    slaveRunner.run();
                    writeInfo(printWriter, ldpType.name(), epsilon, theta, alpha, slaveRunner.getTime(),
                        null, null,
                        slaveRunner.getPacketNum(), slaveRunner.getPayloadByteLength(), slaveRunner.getSendByteLength()
                    );
                }
            }
        }
    }
}
