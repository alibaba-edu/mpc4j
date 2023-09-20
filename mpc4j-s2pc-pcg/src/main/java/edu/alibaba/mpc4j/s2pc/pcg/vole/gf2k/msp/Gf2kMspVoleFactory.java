package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleSender;

/**
 * multi single-point GF2K-VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kMspVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kMspVoleType {
        /**
         * BCG19 (regular index)
         */
        BCG19_REG,
    }

    /**
     * Gets pre-computed num.
     *
     * @param config config.
     * @param t      sparse num.
     * @param num    num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kMspVoleConfig config, int t, int num) {
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        Gf2kMspVoleType type = config.getPtoType();
        Gf2kBspVoleConfig gf2kBspVoleConfig = config.getGf2kBspVoleConfig();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return Gf2kBspVoleFactory.getPrecomputeNum(gf2kBspVoleConfig, t, (int) Math.ceil((double) num / t));
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kMspVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kMspVoleConfig config) {
        Gf2kMspVoleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegGf2kMspVoleSender(senderRpc, receiverParty, (Bcg19RegGf2kMspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kMspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kMspVoleConfig config) {
        Gf2kMspVoleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegGf2kMspVoleReceiver(receiverRpc, senderParty, (Bcg19RegGf2kMspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @return a default config.
     */
    public static Gf2kMspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Bcg19RegGf2kMspVoleConfig.Builder(securityModel).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
