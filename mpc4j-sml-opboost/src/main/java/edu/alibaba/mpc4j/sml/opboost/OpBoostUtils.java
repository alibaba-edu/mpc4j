package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdp;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdpFactory;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdp;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdpFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.Measure;
import smile.data.measure.NominalScale;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * OpBoost工具类。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2021/01/23
 */
public class OpBoostUtils {
    /**
     * 私有构造函数
     */
    private OpBoostUtils() {
        // empty
    }

    /**
     * 返回分类数量。
     *
     * @param formula        标签信息。
     * @param trainDataFrame 训练数据帧。
     * @return 分类数量。
     */
    public static int getNumClass(Formula formula, DataFrame trainDataFrame) {
        StructField labelStructField = formula.y(trainDataFrame).field();
        Measure labelMeasure = labelStructField.measure;
        assert labelMeasure instanceof NominalScale
            : "Label Measure must be Nominal: " + labelMeasure;
        NominalScale labelNominalScale = (NominalScale) labelMeasure;

        return labelNominalScale.size();
    }

    public static DataFrame ldpDataFrame(DataFrame dataFrame, Map<String, LdpConfig> ldpConfigMap) {
        StructType schema = dataFrame.schema();
        // 按列差分隐私处理，感谢@峰青实验结果，应该先创建BaseVector，最后一次性合并成为DataFrame
        @SuppressWarnings("rawtypes")
        BaseVector[] ldpBaseVectors = new BaseVector[schema.length()];
        for (int columnIndex = 0; columnIndex < schema.length(); columnIndex++) {
            StructField structField = schema.field(columnIndex);
            String columnName = structField.name;
            LdpConfig ldpConfig = ldpConfigMap.get(columnName);
            // 如果没有对此列配置差分隐私参数，则直接合并数据
            if (ldpConfig == null) {
                ldpBaseVectors[columnIndex] = dataFrame.column(columnIndex);
                continue;
            }
            // 枚举值类型，使用枚举LDP机制处理
            if (structField.measure instanceof NominalScale) {
                assert ldpConfig instanceof EncodeLdpConfig
                    : "LdpConfig for column " + structField.name + " must be EncodeLdpConfig";
                EncodeLdpConfig encodeLdpConfig = (EncodeLdpConfig) ldpConfig;
                EncodeLdp encodeLdp = EncodeLdpFactory.createInstance(encodeLdpConfig);
                // 差分隐私处理，枚举型数据都可以转换成int
                NominalScale nominalScale = (NominalScale) structField.measure;
                int[] intArray = Arrays.stream(dataFrame.column(columnIndex).toIntArray()).parallel()
                    // 转换为level
                    .mapToObj(nominalScale::toString)
                    // 对level进行差分隐私处理
                    .map(encodeLdp::randomize)
                    // 转换为value
                    .map(nominalScale::valueOf)
                    .mapToInt(Number::intValue)
                    .toArray();
                ldpBaseVectors[columnIndex] = createNominalVector(structField, intArray);
                continue;
            }
            // 整数数值类型，使用整数LDP机制处理
            if (structField.type.isIntegral()) {
                // 数值型整数数据
                assert ldpConfig instanceof IntegralLdpConfig
                    : "LdpConfig for column " + structField.name + " must be IntegralLdpConfig";
                IntegralLdpConfig integralLdpConfig = (IntegralLdpConfig) ldpConfig;
                IntegralLdp integralLdp = IntegralLdpFactory.createInstance(integralLdpConfig);
                // 差分隐私处理，数值型整数数据都可以转换成int
                int[] intArray = Arrays.stream(dataFrame.column(columnIndex).toIntArray()).parallel()
                    .map(integralLdp::randomize)
                    .toArray();
                ldpBaseVectors[columnIndex] = createIntegralVector(structField, intArray);
                continue;
            }
            // 实数数值类型，使用实数LDP机制处理
            if (structField.type.isFloating()) {
                // 数值型实数数据
                assert ldpConfig instanceof RealLdpConfig
                    : "LdpConfig for column " + structField.name + " must be RealLdpConfig";
                RealLdpConfig realLdpConfig = (RealLdpConfig) ldpConfig;
                RealLdp realLdp = RealLdpFactory.createInstance(realLdpConfig);
                // 差分隐私处理，数值型实数数据都可以转换成double
                double[] doubleArray = Arrays.stream(dataFrame.column(columnIndex).toDoubleArray()).parallel()
                    .map(realLdp::randomize)
                    .toArray();
                ldpBaseVectors[columnIndex] = createFloatingVector(structField, doubleArray);
                continue;
            }
            throw new IllegalArgumentException("Do not support type: " + structField.type);
        }
        return DataFrame.of(ldpBaseVectors);
    }

    public static ByteVector createNominalVector(StructField structField, int[] intArray) {
        assert (structField.measure instanceof NominalScale)
            : "StructField for column " + structField.name + " must be Nominal: " + structField.measure;
        assert structField.type.isByte()
            : "StructField type for column " + structField.name + " must be byte: " + structField.type;
        // 转换并合并ByteVector数据
        byte[] byteArray = new byte[intArray.length];
        IntStream.range(0, byteArray.length).forEach(index -> byteArray[index] = (byte) intArray[index]);
        return ByteVector.of(structField, byteArray);
    }

    /**
     * 创建整数类向量。
     *
     * @param structField 数据格式。
     * @param intArray    整数数据。
     * @return 整数类向量。
     */
    @SuppressWarnings("rawtypes")
    public static BaseVector createIntegralVector(StructField structField, int[] intArray) {
        assert structField.isNumeric() || (structField.measure instanceof NominalScale)
            : "StructField for column " + structField.name + " must be numeric or Nominal: " + structField.measure.toString();
        if (structField.type.isByte()) {
            // 转换并合并ByteVector数据
            byte[] byteArray = new byte[intArray.length];
            IntStream.range(0, byteArray.length).forEach(index -> byteArray[index] = (byte) intArray[index]);
            return ByteVector.of(structField, byteArray);
        } else if (structField.type.isShort()) {
            // 转换并合并ShortVector数据
            short[] shortArray = new short[intArray.length];
            IntStream.range(0, shortArray.length).forEach(index -> shortArray[index] = (short) intArray[index]);
            return ShortVector.of(structField, shortArray);
        } else if (structField.type.isInt()) {
            // 合并IntVector数据
            return IntVector.of(structField, intArray);
        } else {
            // 整数类只剩下LongVector数据，转换并合并LongVector数据
            long[] longArray = new long[intArray.length];
            IntStream.range(0, longArray.length).forEach(index -> longArray[index] = intArray[index]);
            return LongVector.of(structField, longArray);
        }
    }

    /**
     * 创建浮点数类向量。
     *
     * @param structField 数据格式。
     * @param doubleArray 浮点数据。
     * @return 浮点数类向量。
     */
    @SuppressWarnings("rawtypes")
    public static BaseVector createFloatingVector(StructField structField, double[] doubleArray) {
        assert structField.type.isFloating()
            : "StructField for column " + structField.name + " must be floating: " + structField.measure.toString();
        if (structField.type.isFloat()) {
            // 转换并合并FloatVector数据
            float[] floatArray = new float[doubleArray.length];
            IntStream.range(0, floatArray.length).forEach(index -> floatArray[index] = (float) doubleArray[index]);
            return FloatVector.of(structField, floatArray);
        } else {
            // 浮点类只剩下DoubleVector数据，转换并合并DoubleVector数据
            return DoubleVector.of(structField, doubleArray);
        }
    }
}
