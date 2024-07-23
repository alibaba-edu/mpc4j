package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;

/**
 * abstract no-choice GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kNcVoleSender extends AbstractTwoPartyPto implements Gf2kNcVoleSender {
    /**
     * config
     */
    private final Gf2kNcVoleConfig config;
    /**
     * field
     */
    protected Sgf2k field;
    /**
     * field
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kNcVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kNcVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, int num) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
        subfield = field.getSubfield();
        this.subfieldL = subfieldL;
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
