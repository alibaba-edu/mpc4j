package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfPtoDesc.PtoStep;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RA17 ECC single-query OPRF sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class Ra17EccSqOprfSender extends AbstractSqOprfSender {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * key
     */
    private Ra17EccSqOprfKey ra17EccSqOprfKey;

    public Ra17EccSqOprfSender(Rpc senderRpc, Party receiverParty, Ra17EccSqOprfConfig config) {
        super(Ra17EccSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public Ra17EccSqOprfKey keyGen() {
        return new Ra17EccSqOprfKey(envType, ecc.randomZn(secureRandom));
    }

    @Override
    public void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // sets the key
        ra17EccSqOprfKey = (Ra17EccSqOprfKey) key;
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
            // decode H(m_c)^β
            .map(ecc::decode)
            // compute H(m_c)^βα
            .map(element -> ecc.multiply(element, ra17EccSqOprfKey.getAlpha()))
            // encode
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }
}
