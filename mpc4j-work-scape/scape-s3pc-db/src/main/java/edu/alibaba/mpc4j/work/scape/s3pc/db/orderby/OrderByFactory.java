package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22.Hzf22OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22.Hzf22OrderByParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive.NaiveOrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive.NaiveOrderByParty;

/**
 * order-by factory
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class OrderByFactory implements PtoFactory {

    public enum OrderByPtoType {
        /**
         * HZF22 OrderBy
         */
        ORDER_BY_HZF22,
        /**
         * naive order by, where the payload is swift during the sorting process
         */
        ORDER_BY_NAIVE,
    }

    public static OrderByParty createParty(Abb3Party abb3Party, OrderByConfig config) {
        return switch (config.getOrderByPtoType()) {
            case ORDER_BY_HZF22 -> new Hzf22OrderByParty(abb3Party, (Hzf22OrderByConfig) config);
            case ORDER_BY_NAIVE -> new NaiveOrderByParty(abb3Party, (NaiveOrderByConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getOrderByPtoType() in creating OrderByParty");
        };
    }

    public static OrderByConfig createDefaultConfig(SecurityModel securityModel) {
        return new NaiveOrderByConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}
