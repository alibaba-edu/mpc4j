package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20.Cgp20SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20.Cgp20SstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20.Cgp20SstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24.Lll24SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24.Lll24SstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24.Lll24SstSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;

/**
 * Single Share Translation factory.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SstFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SstFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum SstType {
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
     * @param config config.
     * @param num    num.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(SstConfig config, int num) {
        MathPreconditions.checkPositive("num", num);
        SstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                Cgp20SstConfig cgp20SstConfig = (Cgp20SstConfig) config;
                return BpRdpprfFactory.getPrecomputeNum(cgp20SstConfig.getBpRdpprfConfig(), num, num);
            case LLL24:
                Lll24SstConfig lll24SstConfig = (Lll24SstConfig) config;
                return BpCdpprfFactory.getPrecomputeNum(lll24SstConfig.getBpCdpprfConfig(), num > 2 ? num - 1 : num, num);
            default:
                throw new IllegalArgumentException("Invalid " + SstType.class.getSimpleName() + ": " + type.name());
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
    public static SstSender createSender(Rpc senderRpc, Party receiverParty, SstConfig config) {
        SstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20SstSender(senderRpc, receiverParty, (Cgp20SstConfig) config);
            case LLL24:
                return new Lll24SstSender(senderRpc, receiverParty, (Lll24SstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SstType.class.getSimpleName() + ": " + type.name());
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
    public static SstReceiver createReceiver(Rpc receiverRpc, Party senderParty, SstConfig config) {
        SstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20SstReceiver(receiverRpc, senderParty, (Cgp20SstConfig) config);
            case LLL24:
                return new Lll24SstReceiver(receiverRpc, senderParty, (Lll24SstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SstType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static SstConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Lll24SstConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
