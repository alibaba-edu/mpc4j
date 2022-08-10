package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.DirectEncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.NaiveRangeIntegralLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.NaiveRangeRealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.PiecewiseLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpConfig;
import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OpBoost测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class OpBoostTestUtils {

    private OpBoostTestUtils() {
        // empty
    }

    /**
     * 默认差分隐私参数ε
     */
    public static final double DEFAULT_EPSILON = 1.0;
    /**
     * 较大差分隐私参数ε
     */
    public static final double LARGE_EPSILON = 100;
    /**
     * 较小差分隐私参数ε
     */
    public static final double SMALL_EPSILON = 0.01;
    /**
     * 随机状态
     */
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 以给定的ε为基础，为所有列创建本地差分隐私配置参数映射。
     *
     * @param dataFrame 数据帧。
     * @param epsilon   基础ε。
     * @return 本地差分隐私配置参数映射。
     */
    public static Map<String, LdpConfig> createLdpConfigMap(DataFrame dataFrame, double epsilon) {
        int[] indexes = IntStream.range(0, dataFrame.schema().length()).toArray();
        return createLdpConfigMap(dataFrame, epsilon, indexes);
    }

    /**
     * 以给定的ε为基础，为指定列创建本地差分隐私配置参数映射。
     *
     * @param dataFrame 数据帧。
     * @param epsilon   基础ε。
     * @return 本地差分隐私配置参数映射。
     */
    public static Map<String, LdpConfig> createLdpConfigMap(DataFrame dataFrame, double epsilon, int... indexes) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (int index : indexes) {
            StructField structField = schema.field(index);
            // 枚举类型，创建编码本地差分隐私机制
            if (structField.measure instanceof NominalScale) {
                NominalScale nominalScale = (NominalScale) structField.measure;
                EncodeLdpConfig encodeLdpSpec = new DirectEncodeLdpConfig
                    .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                    .build();
                ldpConfigMap.put(structField.name, encodeLdpSpec);
                continue;
            }
            // 整数类型，创建整数本地差分隐私机制
            if (structField.type.isIntegral()) {
                int[] data = dataFrame.column(index).toIntArray();
                int lowerBound = Arrays.stream(data).min().orElse(0);
                int upperBound = Arrays.stream(data).max().orElse(0);
                RangeLdpConfig rangeLdpConfig = new PiecewiseLdpConfig
                    .Builder(epsilon)
                    .build();
                IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
                    .Builder(rangeLdpConfig, lowerBound, upperBound)
                    .build();
                ldpConfigMap.put(structField.name, ldpConfig);
                continue;
            }
            // 浮点类型，创建取整本地差分隐私机制
            if (structField.type.isFloating()) {
                double[] data = dataFrame.column(index).toDoubleArray();
                double lowerBound = Arrays.stream(data).min().orElse(0);
                double upperBound = Arrays.stream(data).max().orElse(0);
                RangeLdpConfig rangeLdpConfig = new PiecewiseLdpConfig
                    .Builder(epsilon)
                    .build();
                RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
                    .Builder(rangeLdpConfig, lowerBound, upperBound)
                    .build();
                ldpConfigMap.put(structField.name, ldpConfig);
                continue;
            }
            throw new IllegalArgumentException("Do not support type: " + structField.type);
        }
        return ldpConfigMap;
    }
}
