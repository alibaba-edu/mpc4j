package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;

/**
 * abstract GF2K-NC-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public abstract class AbstractGf2kNcVodeSender extends AbstractTwoPartyPto implements Gf2kNcVodeSender {
    /**
     * config
     */
    private final Gf2kNcVodeConfig config;
    /**
     * field
     */
    protected Dgf2k field;
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

    protected AbstractGf2kNcVodeSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kNcVodeConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, int num) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        subfield = field.getSubfield();
        this.subfieldL = subfieldL;
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
    }
}
