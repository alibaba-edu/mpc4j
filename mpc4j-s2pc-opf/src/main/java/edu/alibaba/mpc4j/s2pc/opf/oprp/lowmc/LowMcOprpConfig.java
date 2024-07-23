package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory.OprpType;

/**
 * LowMc-OPRP协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class LowMcOprpConfig extends AbstractMultiPartyPtoConfig implements OprpConfig {

    private LowMcOprpConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public OprpType getPtoType() {
        return OprpType.LOW_MC;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LowMcOprpConfig> {

        @Override
        public LowMcOprpConfig build() {
            return new LowMcOprpConfig();
        }
    }
}
