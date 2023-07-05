package edu.alibaba.mpc4j.sml.opboost.main.kendall;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.metrics.KendallCorrelation;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.*;
import edu.alibaba.mpc4j.dp.ldp.range.PiecewiseLdpConfig;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoost;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostMainUtils;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.type.IntegerType;
import smile.io.CSV;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * 权重Kendall系数。
 *
 * @author Li Peng, Weiran Liu
 * @date 2021/09/13
 */
public class WeightedKendall implements OpBoost {
    public static final Logger LOGGER = LoggerFactory.getLogger(WeightedKendall.class);
    /**
     * 任务类型
     */
    private static final OpBoostTaskType TASK_TYPE = OpBoostTaskType.WEIGHTED_KENDALL;
    /**
     * 配置参数
     */
    private final Properties properties;
    /**
     * 数据集名称
     */
    private String datasetName;
    /**
     * 数据
     */
    private int[] data;
    /**
     * 浮点数数据
     */
    private double[] doubleData;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 测试轮数
     */
    private int totalRound;
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

    public WeightedKendall(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        datasetName = PropertiesUtils.readString(properties, "dataset_name");
        // 设置数据
        setData();
        // 设置轮数
        totalRound = PropertiesUtils.readInt(properties, "total_round");
        Preconditions.checkArgument(totalRound > 0, "total_round must be greater than 0: %s", totalRound);
        // 设置LDP参数
        setLdpParameters();
    }

    private void setData() throws IOException, URISyntaxException {
        LOGGER.info("-----set data-----");
        String datasetPath = PropertiesUtils.readString(properties, "dataset_path");
        CSV csv = new CSV(OpBoostMainUtils.DEFAULT_CSV_FORMAT);
        DataFrame dataFrame = csv.read(datasetPath);
        LOGGER.info("dataset = {}", dataFrame.toString());

        int columnIndex = PropertiesUtils.readInt(properties, "column_index");
        String columnName = dataFrame.schema().fieldName(columnIndex);
        LOGGER.info("column index = {}, column name = {}", columnIndex, columnName);
        Preconditions.checkArgument(dataFrame.schema().field(columnIndex).type instanceof IntegerType,
            "The selected column must be IntegerType, current type: %s", columnName
        );
        data = dataFrame.column(columnIndex).toIntArray();
        doubleData = Arrays.stream(data).mapToDouble(value -> (double)value).toArray();
        lowerBound = Arrays.stream(data).min().orElse(0);
        upperBound = Arrays.stream(data).max().orElse(0);
        Preconditions.checkArgument(
            lowerBound < upperBound,
            "column %s: lowerBound (%s) must be less than upperBound (%s)",
            columnName, lowerBound, upperBound
        );
    }

    private void setLdpParameters() {
        LOGGER.info("-----set LDP parameters-----");
        // 设置ε
        epsilons = PropertiesUtils.readDoubleArray(properties, "epsilon");
        // 设置θ
        thetas = PropertiesUtils.readIntArray(properties, "theta");
        // 设置α
        alphas = PropertiesUtils.readDoubleArray(properties, "alpha");
    }

    @Override
    public void run() throws IOException {
        String filePath = TASK_TYPE.name()
            // 数据集
            + "_" + datasetName
            // 测试轮数
            + "_" + totalRound
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 输出表头
        String tab = "name\tε\tθ\tα\tKendall";
        printWriter.println(tab);
        // 分段
        runEpsilonKendall(printWriter, OpBoostLdpType.PIECEWISE);
        // GlobalMap
        runEpsilonKendall(printWriter, OpBoostLdpType.GLOBAL_MAP);
        // GlobalExpMap
        runEpsilonKendall(printWriter, OpBoostLdpType.GLOBAL_EXP_MAP);
        // LocalMap
        runLocalMapKendall(printWriter, OpBoostLdpType.LOCAL_MAP);
        // LocalExpMap
        runLocalMapKendall(printWriter, OpBoostLdpType.LOCAL_EXP_MAP);
        // AdjMap
        runAdjMapKendall(printWriter, OpBoostLdpType.ADJ_MAP);
        // AdjExpMap
        runAdjMapKendall(printWriter, OpBoostLdpType.ADJ_EXP_MAP);
        printWriter.close();
        fileWriter.close();
    }

    private void runEpsilonKendall(PrintWriter printWriter, OpBoostLdpType ldpType) {
        LOGGER.info("-----{} LDP Kendall-----", ldpType.name());
        for (double epsilon : epsilons) {
            IntegralLdpConfig integralLdpConfig;
            switch (ldpType) {
                case PIECEWISE:
                    PiecewiseLdpConfig piecewiseLdpConfig = new PiecewiseLdpConfig.Builder(epsilon).build();
                    integralLdpConfig = new NaiveRangeIntegralLdpConfig
                        .Builder(piecewiseLdpConfig, lowerBound, upperBound)
                        .build();
                    break;
                case GLOBAL_MAP:
                    integralLdpConfig = new GlobalMapIntegralLdpConfig
                        .Builder(epsilon, lowerBound, upperBound)
                        .build();
                    break;
                case GLOBAL_EXP_MAP:
                    integralLdpConfig =  new GlobalExpMapIntegralLdpConfig
                        .Builder(epsilon, lowerBound, upperBound)
                        .build();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
            }
            IntegralLdp integralLdp = IntegralLdpFactory.createInstance(integralLdpConfig);
            double kendall = calculateKendall(integralLdp);
            writeInfo(printWriter, ldpType.name(), epsilon, null, null, kendall);
        }
    }

    private void runLocalMapKendall(PrintWriter printWriter, OpBoostLdpType ldpType) {
        LOGGER.info("-----{} LDP Kendall-----", ldpType.name());
        for (int theta : thetas) {
            for (double epsilon : epsilons) {
                IntegralLdpConfig integralLdpConfig;
                switch (ldpType) {
                    case LOCAL_MAP:
                        integralLdpConfig = new LocalMapIntegralLdpConfig
                            .Builder(epsilon, theta, lowerBound, upperBound)
                            .build();
                        break;
                    case LOCAL_EXP_MAP:
                        integralLdpConfig = new LocalExpMapIntegralLdpConfig
                            .Builder(epsilon, theta, lowerBound, upperBound)
                            .build();
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                }
                IntegralLdp integralLdp = IntegralLdpFactory.createInstance(integralLdpConfig);
                double kendall = calculateKendall(integralLdp);
                writeInfo(printWriter, ldpType.name(), epsilon, theta, null, kendall);
            }
        }
    }

    private void runAdjMapKendall(PrintWriter printWriter, OpBoostLdpType ldpType) {
        LOGGER.info("-----{} LDP Kendall-----", ldpType.name());
        for (double alpha : alphas) {
            for (int theta : thetas) {
                for (double epsilon : epsilons) {
                    IntegralLdpConfig integralLdpConfig;
                    switch (ldpType) {
                        case ADJ_MAP:
                            integralLdpConfig = new AdjMapIntegralLdpConfig
                                .Builder(epsilon, theta, lowerBound, upperBound)
                                .setAlpha(alpha)
                                .build();
                            break;
                        case ADJ_EXP_MAP:
                            integralLdpConfig = new AdjExpMapIntegralLdpConfig
                                .Builder(epsilon, theta, lowerBound, upperBound)
                                .setAlpha(alpha)
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                    IntegralLdp integralLdp = IntegralLdpFactory.createInstance(integralLdpConfig);
                    double kendall = calculateKendall(integralLdp);
                    writeInfo(printWriter, ldpType.name(), epsilon, theta, alpha, kendall);
                }
            }
        }
    }

    private double calculateKendall(IntegralLdp integralLdp) {
        double sumKendall = 0;
        for (int round = 1; round <= totalRound; round++) {
            double[] randomizedDoubleData = Arrays.stream(data)
                .parallel()
                .map(integralLdp::randomize)
                .mapToDouble(value -> (double)value)
                .toArray();
            double currentKendall = KendallCorrelation.directTauDr(doubleData, randomizedDoubleData);
            LOGGER.info("{}: round {}, Kendall = {}", integralLdp.getMechanismName(), round, currentKendall);
            sumKendall += currentKendall;
        }
        return sumKendall / totalRound;
    }

    private void writeInfo(PrintWriter printWriter, String name,
                           Double epsilon, Integer theta, Double alpha, double measure) {
        String information = name
            // ε
            + "\t" + (Objects.isNull(epsilon) ? "N/A" : epsilon)
            // θ
            + "\t" + (Objects.isNull(theta) ? "N/A" : theta)
            // α
            + "\t" + (Objects.isNull(alpha) ? "N/A" : alpha)
            // measure
            + "\t" + measure;
        printWriter.println(information);
    }
}
