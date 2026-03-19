package edu.alibaba.mpc4j.work.db.sketch.main.HLL;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;
import edu.alibaba.mpc4j.work.db.sketch.HLL.v1.v1HLLConfig;

import java.util.Properties;

public class HLLConfigUtils {
    private static final String SORT_TYPE = "sort_pto_type";
    private HLLConfigUtils() {

    }

    public static HLLConfig createConfig(Properties properties) {
        HLLPtoType hllPtoType = MainPtoConfigUtils.readEnum(HLLPtoType.class,properties,HLLMain.PTO_NAME_KEY);
        return switch(hllPtoType){
            case V1 -> generateHLLConfig(properties);
            default ->
                    throw new IllegalArgumentException();
        };
    }
    private static HLLConfig generateHLLConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, HLLMain.IS_MALICIOUS);
        PgSortFactory.PgSortType sortType = MainPtoConfigUtils.readEnum(PgSortFactory.PgSortType.class, properties, SORT_TYPE);
        PgSortConfig sortConfig = PgSortFactory.createSortConfig(sortType, malicious);
        return new v1HLLConfig.Builder(malicious).setPgSortConfig(sortConfig).build();
    }
}
