package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdpConfig;
import smile.data.measure.NominalScale;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 抽象OpBoost主机配置构造器。
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractOpBoostConfigBuilder<T> implements org.apache.commons.lang3.builder.Builder<T> {
    /**
     * 数据格式
     */
    private final StructType schema;
    /**
     * LDP机制配置项
     */
    private final Map<String, LdpConfig> ldpConfigMap;

    public AbstractOpBoostConfigBuilder(StructType schema) {
        this.schema = schema;
        // 初始化LDP机制配置项
        ldpConfigMap = new HashMap<>(schema.length());
    }

    /**
     * 增加LDP机制配置项。
     *
     * @param ldpConfigMap LDP机制配置项映射。
     * @return 配置项构造器。
     */
    protected AbstractOpBoostConfigBuilder<T> addLdpConfig(Map<String, LdpConfig> ldpConfigMap) {
        for (String name : ldpConfigMap.keySet()) {
            innerAddLdpConfig(name, ldpConfigMap.get(name));
        }
        return this;
    }

    /**
     * 增加LDP机制配置项。
     *
     * @param name 列名称。
     * @param ldpConfig LDP机制配置项。
     * @return 配置项构造器。
     */
    protected AbstractOpBoostConfigBuilder<T> addLdpConfig(String name, LdpConfig ldpConfig) {
        innerAddLdpConfig(name, ldpConfig);
        return this;
    }

    private void innerAddLdpConfig(String name, LdpConfig ldpConfig) {
        // 根据列名称得到对应的列描述
        StructField structField = schema.field(name);
        if (structField.measure instanceof NominalScale) {
            // 枚举类型，则LDP需要为编码类型
            assert ldpConfig instanceof EncodeLdpConfig
                : "LDP for " + name + " must be " + EncodeLdpConfig.class.getSimpleName();
            NominalScale nominalScale = (NominalScale)structField.measure;
            EncodeLdpConfig encodeLdpConfig = (EncodeLdpConfig)ldpConfig;
            Set<String> labelSet = encodeLdpConfig.getLabelSet();
            // 验证全包含，且验证数量相等即可
            for (String label : nominalScale.levels()) {
                assert labelSet.contains(label) : label + " is not in label set";
            }
            assert labelSet.size() == nominalScale.size() : "# labels in schema does not match # labels in label set";
        } else if (structField.type.isIntegral()) {
            // 数值整数类型，LDP需要为数值整数类型
            assert ldpConfig instanceof IntegralLdpConfig
                : "LDP for " + name + " must be " + IntegralLdpConfig.class.getSimpleName();
        } else if (structField.type.isFloating()) {
            // 数值浮点类型，LDP需要为数值浮点类型
            assert ldpConfig instanceof RealLdpConfig
                : "LDP for " + name + " must be " + RealLdpConfig.class.getSimpleName();
        } else {
            throw new IllegalArgumentException("Does not support LDP for " + name + " with measure: " + structField.measure);
        }
        ldpConfigMap.put(name, ldpConfig);
    }

    public StructType getSchema() {
        return schema;
    }

    public Map<String, LdpConfig> getLdpConfigMap() {
        return ldpConfigMap;
    }
}
