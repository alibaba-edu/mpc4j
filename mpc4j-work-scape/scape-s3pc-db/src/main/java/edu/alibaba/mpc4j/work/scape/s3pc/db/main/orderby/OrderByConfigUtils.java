package edu.alibaba.mpc4j.work.scape.s3pc.db.main.orderby;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFactory.OrderByPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22.Hzf22OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive.NaiveOrderByConfig;

import java.util.Properties;

/**
 * configure utils for 3p order-by party.
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class OrderByConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private OrderByConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static OrderByConfig createConfig(Properties properties) {
        OrderByPtoType ptoType = MainPtoConfigUtils.readEnum(OrderByPtoType.class, properties, OrderByMain.PTO_NAME_KEY);
        return switch (ptoType) {
            case ORDER_BY_NAIVE -> generateNaiveOrderByConfig(properties);
            case ORDER_BY_HZF22 -> generateHzf22OrderByConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + OrderByPtoType.class.getSimpleName() + ": " + ptoType.name());
        };
    }

    private static NaiveOrderByConfig generateNaiveOrderByConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, OrderByMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new NaiveOrderByConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }

    private static Hzf22OrderByConfig generateHzf22OrderByConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, OrderByMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        Hzf22OrderByConfig config = new Hzf22OrderByConfig.Builder(malicious).build();
        config.setComparatorType(comparatorType);
        return config;
    }
}
