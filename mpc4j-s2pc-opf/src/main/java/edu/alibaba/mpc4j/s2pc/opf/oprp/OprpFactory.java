package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcUtils;

/**
 * OPRP factory.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class OprpFactory implements PtoFactory {
    /**
     * private constructor
     */
    private OprpFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum OprpType {
        /**
         * LowMC
         */
        LOW_MC,
    }

    /**
     * Gets expected Z2 triple num.
     *
     * @param batchSize batch size.
     * @return expected Z2 triple num.
     */
    public static long expectZ2TripleNum(OprpType type, int batchSize) {
        MathPreconditions.checkPositive("batch_size", batchSize);
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LOW_MC:
                int roundBatchSize = CommonUtils.getByteLength(batchSize) * Byte.SIZE;
                return (long) LowMcUtils.SBOX_NUM * 3 * roundBatchSize * LowMcUtils.ROUND;
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param z2cSender     sender.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static OprpSender createSender(Z2cParty z2cSender, Party receiverParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LOW_MC:
                return new LowMcOprpSender(z2cSender, receiverParty, (LowMcOprpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiver    receiver.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static OprpReceiver createReceiver(Z2cParty receiver, Party senderParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LOW_MC:
                return new LowMcOprpReceiver(receiver, senderParty, (LowMcOprpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static OprpConfig createDefaultConfig() {
        return new LowMcOprpConfig.Builder().build();
    }
}
