package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * abstract GF2K-SSP-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractGf2kSspVoleSender extends AbstractTwoPartyPto implements Gf2kSspVoleSender {
    /**
     * config
     */
    protected final Gf2kSspVoleConfig config;
    /**
     * field
     */
    protected Sgf2k field;
    /**
     * field byte L
     */
    protected int fieldByteL;
    /**
     * field L
     */
    protected int fieldL;
    /**
     * subfield
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * subfield Byte L
     */
    protected int subfieldByteL;
    /**
     * r
     */
    protected int r;
    /**
     * α
     */
    protected int alpha;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kSspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kSspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(alpha, num);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preSenderOutput.getNum(), Gf2kSspVoleFactory.getPrecomputeNum(config, subfieldL, num)
            );
        }
    }
}
