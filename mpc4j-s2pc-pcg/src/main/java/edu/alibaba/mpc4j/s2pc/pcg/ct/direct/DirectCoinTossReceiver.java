package edu.alibaba.mpc4j.s2pc.pcg.ct.direct;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.ct.AbstractCoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossPtoDesc.PtoStep;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * direct coin-tossing protocol receiver.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class DirectCoinTossReceiver extends AbstractCoinTossParty {

    public DirectCoinTossReceiver(Rpc receiverRpc, Party senderParty, DirectCoinTossConfig config) {
        super(DirectCoinTossPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty init step
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] coinToss(int num, int bitLength) throws MpcAbortException {
        setPtoInput(num, bitLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader coinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COINS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> coinsPayload = rpc.receive(coinsHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(coinsPayload.size() == num);
        // receiver parses coins
        byte[][] coins = coinsPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long coinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coinTime);

        logPhaseInfo(PtoState.PTO_END);
        return coins;
    }
}
