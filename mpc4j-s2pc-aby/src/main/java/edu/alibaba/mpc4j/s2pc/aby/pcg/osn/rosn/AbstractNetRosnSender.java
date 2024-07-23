package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

/**
 * abstract Network Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/10
 */
public abstract class AbstractNetRosnSender extends AbstractRosnSender {
    /**
     * COT
     */
    protected final CotSender cotSender;
    /**
     * pre-computed COT
     */
    protected final PreCotSender preCotSender;

    protected AbstractNetRosnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, NetRosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
    }
}
