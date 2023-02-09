package edu.alibaba.mpc4j.sml.opboost.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.DirectEncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.*;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.AdjMapRealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.GlobalMapRealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.LocalMapRealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.NaiveRangeRealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.PiecewiseLdpConfig;
import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.Read;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OpBoost主函数工具类。
 *
 * @author Weiran Liu
 * @date 2022/7/21
 */
public class OpBoostMainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpBoostMainUtils.class);
    /**
     * 私有构造函数
     */
    private OpBoostMainUtils() {
        // empty
    }

    /**
     * 默认CSV格式
     */
    public static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.Builder.create()
        .setHeader()
        .setIgnoreHeaderCase(true)
        .build();

    /**
     * 设置数据集名称。
     *
     * @param properties 配置项。
     * @return 数据集名称。
     */
    public static String setDatasetName(Properties properties) {
        LOGGER.info("-----set dataset name-----");
        return PropertiesUtils.readString(properties, "dataset_name");
    }

    /**
     * 设置标签。
     *
     * @param properties 配置项。
     * @return 标签。
     */
    public static Formula setFormula(Properties properties) {
        LOGGER.info("-----set label-----");
        String formulaString = PropertiesUtils.readString(properties, "formula");
        return Formula.lhs(formulaString);
    }

    /**
     * 设置元数据信息。
     *
     * @param properties 配置项。
     * @return 元数据信息。
     */
    public static StructType setSchema(Properties properties, Formula formula) {
        LOGGER.info("-----set whole schema-----");
        String[] columnTypes = PropertiesUtils.readTrimStringArray(properties, "column_types");
        String[] columnNames = PropertiesUtils.readTrimStringArray(properties, "column_names");
            Preconditions.checkArgument(
            Arrays.stream(columnNames).collect(Collectors.toSet()).size() == columnNames.length,
            "column_names contains duplicated names"
        );
        Preconditions.checkArgument(
            columnTypes.length == columnNames.length,
            "# of column type = %s, # of column name = %s, must be the same",
            columnTypes.length, columnNames.length
        );
        int ncols = columnTypes.length;
        StructField[] structFields = IntStream.range(0, ncols)
            .mapToObj(columnIndex -> {
                String columnName = columnNames[columnIndex];
                String columnType = columnTypes[columnIndex];
                switch (columnType) {
                    case "N":
                        // nominal，枚举类，必须为one-hot格式
                        return new StructField(columnName, DataTypes.ByteType, new NominalScale("0", "1"));
                    case "I":
                        // int，整数类
                        return new StructField(columnName, DataTypes.IntegerType);
                    case "F":
                        // float，浮点数类
                        return new StructField(columnName, DataTypes.FloatType);
                    case "D":
                        // double，双精度浮点数类
                        return new StructField(columnName, DataTypes.DoubleType);
                    case "C":
                        // 分类任务的标签类型
                        String[] classTypes = PropertiesUtils.readTrimStringArray(properties, "class_types");
                        return new StructField(columnName, DataTypes.ByteType, new NominalScale(classTypes));
                    default:
                        throw new IllegalArgumentException("Invalid columnType: " + columnType);
                }
            })
            .toArray(StructField[]::new);
        StructType schema = DataTypes.struct(structFields);
        Preconditions.checkNotNull(schema.field(formula.response().toString()), "Label must be in the schema.");
        return schema;
    }

    /**
     * 设置总测试轮数。
     *
     * @param properties 配置项。
     * @return 总测试轮数。
     */
    public static int setTotalRound(Properties properties) {
        int totalRound = PropertiesUtils.readInt(properties, "total_round");
        Preconditions.checkArgument(totalRound >= 1, "round must be greater than or equal to 1");
        return totalRound;
    }

    /**
     * 设置LDP列映射。
     *
     * @param properties 配置项。
     * @param schema 元数据信息。
     * @return LDP列映射。
     */
    public static Map<String, Boolean> setLdpColumnsMap(Properties properties, StructType schema) {
        LOGGER.info("-----set LDP columns-----");
        int ncols = schema.length();
        int[] dpColumns = PropertiesUtils.readIntArray(properties, "ldp_columns");
        Preconditions.checkArgument(dpColumns.length == ncols, "# ldp_column must match column_num");
        Arrays.stream(dpColumns).forEach(value ->
                Preconditions.checkArgument(
                    value == 0 || value == 1,
                    "Invalid ldp_column: %s, only support 0 or 1", value)
            );

        return IntStream.range(0, ncols)
            .boxed()
            .collect(Collectors.toMap(
                schema::fieldName,
                columnIndex -> (dpColumns[columnIndex] == 1)
            ));
    }

    /**
     * 设置ε。
     *
     * @param properties 配置项。
     * @return ε。
     */
    public static double[] setEpsilons(Properties properties) {
        return PropertiesUtils.readDoubleArray(properties, "epsilon");
    }

    /**
     * 设置θ。
     *
     * @param properties 配置项。
     * @return θ。
     */
    public static int[] setThetas(Properties properties) {
        return PropertiesUtils.readIntArray(properties, "theta");
    }

    /**
     * 设置α。
     *
     * @param properties 配置项。
     * @return α
     */
    public static double[] setAlphas(Properties properties) {
        return PropertiesUtils.readDoubleArray(properties, "alpha");
    }

    /**
     * 设置训练数据集。
     *
     * @param properties 配置项。
     * @param schema 元数据信息。
     * @return 训练数据集。
     * @throws IOException 如果出现IO异常。
     * @throws URISyntaxException 如果文件路径有误。
     */
    public static DataFrame setTrainDataFrame(Properties properties, StructType schema) throws IOException, URISyntaxException {
        String trainDatasetPath = PropertiesUtils.readString(properties, "train_dataset_path");
        return Read.csv(trainDatasetPath, DEFAULT_CSV_FORMAT, schema);
    }

    /**
     * 设置测试数据集。
     *
     * @param properties 配置项。
     * @param schema 元数据信息。
     * @return 训练数据集。
     * @throws IOException 如果出现IO异常。
     * @throws URISyntaxException 如果文件路径有误。
     */
    public static DataFrame setTestDataFrame(Properties properties, StructType schema) throws IOException, URISyntaxException {
        String testDatasetPath = PropertiesUtils.readString(properties, "test_dataset_path");
        return Read.csv(testDatasetPath, DEFAULT_CSV_FORMAT, schema);
    }

    /**
     * 读取上下界。
     *
     * @param dataFrame 数据帧。
     * @param structField 列信息。
     * @return [下界, 上界]。
     */
    private static int[] readIntBounds(DataFrame dataFrame, StructField structField) {
        int[] dataArray = dataFrame.column(structField.name).toIntArray();
        int[] bounds = new int[2];
        bounds[0] = Arrays.stream(dataArray).min().orElse(0);
        bounds[1] = Arrays.stream(dataArray).max().orElse(0);
        Preconditions.checkArgument(
            bounds[0] < bounds[1],
            "column %s: lowerBound (%s) must be less than upperBound (%s)",
            structField.name, bounds[0], bounds[1]
        );
        return bounds;
    }

    /**
     * 读取上下界。
     *
     * @param dataFrame 数据帧。
     * @param structField 列信息。
     * @return [下界, 上界]。
     */
    private static double[] readDoubleBounds(DataFrame dataFrame, StructField structField) {
        double[] dataArray = dataFrame.column(structField.name).toDoubleArray();
        double[] bounds = new double[2];
        bounds[0] = Arrays.stream(dataArray).min().orElse(0);
        bounds[1] = Arrays.stream(dataArray).max().orElse(0);
        Preconditions.checkArgument(
            bounds[0] < bounds[1],
            "column %s: lowerBound (%s) must be less than upperBound (%s)",
            structField.name, bounds[0], bounds[1]
        );
        return bounds;
    }

    /**
     * 创建LDP配置项。
     *
     * @param dataFrame 数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType LDP类型。
     * @param epsilon ε。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          OpBoostLdpType ldpType, double epsilon) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
                        case PIECEWISE:
                            ldpConfig = new NaiveRangeIntegralLdpConfig
                                .Builder(new PiecewiseLdpConfig.Builder(epsilon).build(), bounds[0], bounds[1])
                                .build();
                            break;
                        case GLOBAL_MAP:
                            ldpConfig = new GlobalMapIntegralLdpConfig
                                .Builder(epsilon, bounds[0], bounds[1])
                                .build();
                            break;
                        case GLOBAL_EXP_MAP:
                            ldpConfig = new GlobalExpMapIntegralLdpConfig
                                .Builder(epsilon, bounds[0], bounds[1])
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    // 浮点数类型，创建浮点数LDP机制
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
                        case PIECEWISE:
                            ldpConfig = new NaiveRangeRealLdpConfig
                                .Builder(new PiecewiseLdpConfig.Builder(epsilon).build(), bounds[0], bounds[1])
                                .build();
                            break;
                        case GLOBAL_MAP:
                        case GLOBAL_EXP_MAP:
                            ldpConfig = new GlobalMapRealLdpConfig
                                .Builder(epsilon, bounds[0], bounds[1])
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
    }

    /**
     * 创建LDP配置项。
     *
     * @param dataFrame 数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType LDP类型。
     * @param epsilon ε。
     * @param theta   θ。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          OpBoostLdpType ldpType, double epsilon, int theta) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
                        case LOCAL_MAP:
                            ldpConfig = new LocalMapIntegralLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .build();
                            break;
                        case LOCAL_EXP_MAP:
                            ldpConfig = new LocalExpMapIntegralLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
                        case LOCAL_MAP:
                        case LOCAL_EXP_MAP:
                            ldpConfig = new LocalMapRealLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
    }

    /**
     * 创建调整映射LDP配置项。
     *
     * @param dataFrame 数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType LDP类型。
     * @param epsilon ε。
     * @param theta   θ。
     * @param alpha   α。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          OpBoostLdpType ldpType, double epsilon, int theta, double alpha) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
                        case ADJ_MAP:
                            ldpConfig = new AdjMapIntegralLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .setAlpha(alpha)
                                .build();
                            break;
                        case ADJ_EXP_MAP:
                            ldpConfig = new AdjExpMapIntegralLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .setAlpha(alpha)
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
                        case ADJ_MAP:
                        case ADJ_EXP_MAP:
                            ldpConfig = new AdjMapRealLdpConfig
                                .Builder(epsilon, theta, bounds[0], bounds[1])
                                .setAlpha(alpha)
                                .build();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
    }
}
