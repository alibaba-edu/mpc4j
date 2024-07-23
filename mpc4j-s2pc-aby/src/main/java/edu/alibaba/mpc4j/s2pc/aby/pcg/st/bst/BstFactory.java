package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;

/**
 * Batched Share Translation factory.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public class BstFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BstFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum BstType {
        /**
         * CGP20
         */
        CGP20,
        /**
         * LLL24
         */
        LLL24,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config   config.
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(BstConfig config, int batchNum, int eachNum) {
        MathPreconditions.checkPositive("batchNum", batchNum);
        MathPreconditions.checkPositive("eachNum", eachNum);
        BstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                Cgp20BstConfig cgp20BstConfig = (Cgp20BstConfig) config;
                return BpRdpprfFactory.getPrecomputeNum(cgp20BstConfig.getBpRdpprfConfig(), batchNum * eachNum, eachNum);
            case LLL24:
                Lll24BstConfig lll24BstConfig = (Lll24BstConfig) config;
                return BpCdpprfFactory.getPrecomputeNum(lll24BstConfig.getBpCdpprfConfig(), batchNum * (eachNum > 2 ? eachNum - 1 : eachNum), eachNum);
            default:
                throw new IllegalArgumentException("Invalid " + BstType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static BstSender createSender(Rpc senderRpc, Party receiverParty, BstConfig config) {
        BstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20BstSender(senderRpc, receiverParty, (Cgp20BstConfig) config);
            case LLL24:
                return new Lll24BstSender(senderRpc, receiverParty, (Lll24BstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BstType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static BstReceiver createReceiver(Rpc receiverRpc, Party senderParty, BstConfig config) {
        BstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20BstReceiver(receiverRpc, senderParty, (Cgp20BstConfig) config);
            case LLL24:
                return new Lll24BstReceiver(receiverRpc, senderParty, (Lll24BstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BstType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static BstConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Lll24BstConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
