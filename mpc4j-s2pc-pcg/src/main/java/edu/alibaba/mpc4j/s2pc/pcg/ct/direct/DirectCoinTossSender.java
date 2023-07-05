package edu.alibaba.mpc4j.s2pc.pcg.ct.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ct.AbstractCoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * direct coin-tossing protocol sender.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class DirectCoinTossSender extends AbstractCoinTossParty {

    public DirectCoinTossSender(Rpc senderRpc, Party receiverParty, DirectCoinTossConfig config) {
        super(DirectCoinTossPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty init step
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] coinToss(int num, int bitLength) {
        setPtoInput(num, bitLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // sender generates the randomness and send them to the receiver.
        byte[][] coins = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.randomByteArray(byteLength, bitLength, secureRandom))
            .toArray(byte[][]::new);
        List<byte[]> coinsPayload = Arrays.stream(coins).collect(Collectors.toList());
        DataPacketHeader coinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COINS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(coinsHeader, coinsPayload));
        stopWatch.stop();
        long coinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coinTime);

        logPhaseInfo(PtoState.PTO_END);
        return coins;
    }
}
