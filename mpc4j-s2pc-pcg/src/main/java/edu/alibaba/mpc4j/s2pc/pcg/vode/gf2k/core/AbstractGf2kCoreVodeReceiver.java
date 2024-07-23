package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;

/**
 * abstract GF2K-core-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public abstract class AbstractGf2kCoreVodeReceiver extends AbstractTwoPartyPto implements Gf2kCoreVodeReceiver {
    /**
     * field
     */
    protected Dgf2k field;
    /**
     * field L
     */
    protected int fieldL;
    /**
     * field byte L
     */
    protected int fieldByteL;
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
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kCoreVodeReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kCoreVodeConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = delta;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
    }
}
