package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20.Cgp20PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20.Cgp20PstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20.Cgp20PstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24.Lll24PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24.Lll24PstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24.Lll24PstSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;

/**
 * partial ST factory
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public class PstFactory {
    /**
     * protocol type.
     */
    public enum PstType {
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
    public static int getPrecomputeNum(PstConfig config, int batchNum, int eachNum) {
        MathPreconditions.checkPositive("batchNum", batchNum);
        MathPreconditions.checkPositive("eachNum", eachNum);
        // T = 2^t
        Preconditions.checkArgument(IntMath.isPowerOfTwo(eachNum), "eachNum must be a power of 2: %s", eachNum);
        PstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                Cgp20BstConfig cgp20BstConfig = (Cgp20BstConfig) config.getBstConfig();
                return BpRdpprfFactory.getPrecomputeNum(cgp20BstConfig.getBpRdpprfConfig(), batchNum * eachNum, eachNum);
            case LLL24:
                Lll24BstConfig lll24BstConfig = (Lll24BstConfig) config.getBstConfig();
                if (eachNum <= 2) {
                    return batchNum;
                } else {
                    // 最后一行可以不用生成，并且最高位（right part of net）或者最低位（left part of net）可以OT数量减半
                    return BpCdpprfFactory.getPrecomputeNum(lll24BstConfig.getBpCdpprfConfig(),
                        batchNum * (eachNum - 1), eachNum)
                        - (eachNum / 2 - 1) * batchNum
                        - (eachNum / 4 - 1) * batchNum;
                }
            default:
                throw new IllegalArgumentException("Invalid " + PstType.class.getSimpleName() + ": " + type.name());
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
    public static PstSender createSender(Rpc senderRpc, Party receiverParty, PstConfig config) {
        PstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20PstSender(senderRpc, receiverParty, (Cgp20PstConfig) config);
            case LLL24:
                return new Lll24PstSender(senderRpc, receiverParty, (Lll24PstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PstType.class.getSimpleName() + ": " + type.name());
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
    public static PstReceiver createReceiver(Rpc receiverRpc, Party senderParty, PstConfig config) {
        PstType type = config.getPtoType();
        switch (type) {
            case CGP20:
                return new Cgp20PstReceiver(receiverRpc, senderParty, (Cgp20PstConfig) config);
            case LLL24:
                return new Lll24PstReceiver(receiverRpc, senderParty, (Lll24PstConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PstType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PstConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Lll24PstConfig.Builder(silent).build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

}
