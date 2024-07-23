package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

/**
 * abstract Network Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/10
 */
public abstract class AbstractNetRosnReceiver extends AbstractRosnReceiver {
    /**
     * COT
     */
    protected final CotReceiver cotReceiver;
    /**
     * pre-computed COT
     */
    protected final PreCotReceiver preCotReceiver;

    protected AbstractNetRosnReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, NetRosnConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }
}
