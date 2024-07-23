package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * abstract GF2K-NC-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kNcVodeReceiver extends AbstractTwoPartyPto implements Gf2kNcVodeReceiver {
    /**
     * config
     */
    private final Gf2kNcVodeConfig config;
    /**
     * field
     */
    protected Dgf2k field;
    /**
     * subfield
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kNcVodeReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kNcVodeConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta, int num) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        subfield = field.getSubfield();
        this.subfieldL = subfieldL;
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
    }
}