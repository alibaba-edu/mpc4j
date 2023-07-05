package edu.alibaba.mpc4j.common.rpc.pto;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * abstract multi-party protocol config
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public abstract class AbstractMultiPartyPtoConfig implements MultiPartyPtoConfig {
    /**
     * environment
     */
    private EnvType envType;
    /**
     * security model
     */
    private SecurityModel securityModel;
    /**
     * sub-protocol configs
     */
    private final MultiPartyPtoConfig[] subPtoConfigs;

    /**
     * Creates a protocol config, with its security model fully depending on its sub-protocol.
     *
     * @param subPtoConfig config for its sub-protocol.
     */
    protected AbstractMultiPartyPtoConfig(MultiPartyPtoConfig subPtoConfig) {
        this(SecurityModel.MALICIOUS, subPtoConfig);
    }

    /**
     * Creates a protocol config with a default security model.
     *
     * @param defaultModel default security model.
     * @param subPtoConfigs configs for sub-protocols.
     */
    protected AbstractMultiPartyPtoConfig(SecurityModel defaultModel, MultiPartyPtoConfig... subPtoConfigs) {
        this.subPtoConfigs = subPtoConfigs;
        if (subPtoConfigs.length == 0) {
            envType = EnvType.STANDARD;
            securityModel = defaultModel;
        } else {
            envType = subPtoConfigs[0].getEnvType();
            for (MultiPartyPtoConfig config : subPtoConfigs) {
                Preconditions.checkArgument(config.getEnvType().equals(envType));
            }
            securityModel = defaultModel;
            for (MultiPartyPtoConfig config : subPtoConfigs) {
                if (config.getSecurityModel().compareTo(securityModel) < 0) {
                    securityModel = config.getSecurityModel();
                }
            }
        }
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
        for (MultiPartyPtoConfig config : subPtoConfigs) {
            config.setEnvType(envType);
        }
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return securityModel;
    }
}
