package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RA17 byte ECC single-query OPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Ra17ByteEccSqOprfSender extends AbstractSqOprfSender {
    /**
     * byte full ECC
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * key
     */
    private Ra17ByteEccSqOprfKey ra17ByteEccSqOprfKey;

    public Ra17ByteEccSqOprfSender(Rpc senderRpc, Party receiverParty, Ra17ByteEccSqOprfConfig config) {
        super(Ra17ByteEccSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
    }

    @Override
    public Ra17ByteEccSqOprfKey keyGen() {
        return new Ra17ByteEccSqOprfKey(envType, byteFullEcc.randomZn(secureRandom));
    }

    @Override
    public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // sets the key
        ra17ByteEccSqOprfKey = (Ra17ByteEccSqOprfKey) key;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();

        stopWatch.start();
        List<byte[]> blindPrf = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrf));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prfTime, "Sender computes PRFs");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> handleBlindPayload(List<byte[]> blindPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPayload.size() == batchSize);
        Stream<byte[]> blindStream = blindPayload.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // compute H(m_c)^βα
            .map(element -> byteFullEcc.mul(element, ra17ByteEccSqOprfKey.getAlpha()))
            .collect(Collectors.toList());
    }
}
