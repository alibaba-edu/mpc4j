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

import java.util.Arrays;

/**
 * abstract GF2K-core-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public abstract class AbstractGf2kCoreVodeSender extends AbstractTwoPartyPto implements Gf2kCoreVodeSender {
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
     * x
     */
    protected byte[][] xs;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kCoreVodeSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kCoreVodeConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int subfieldL) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        initState();
    }

    protected void setPtoInput(byte[][] xs) {
        checkInitialized();
        MathPreconditions.checkPositive("num", xs.length);
        num = xs.length;
        this.xs = Arrays.stream(xs)
            .peek(xi -> Preconditions.checkArgument(subfield.validateElement(xi)))
            .toArray(byte[][]::new);
    }
}
