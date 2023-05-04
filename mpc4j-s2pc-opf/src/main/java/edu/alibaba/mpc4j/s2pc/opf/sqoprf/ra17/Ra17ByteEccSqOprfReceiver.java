package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RA17 byte ECC single-query OPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Ra17ByteEccSqOprfReceiver extends AbstractSqOprfReceiver {
    /**
     * byte full ECC
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;

    public Ra17ByteEccSqOprfReceiver(Rpc receiverRpc, Party senderParty, Ra17ByteEccSqOprfConfig config) {
        super(Ra17ByteEccSqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        stopWatch.stop();
        long blindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, blindTime, "Receiver blinds");

        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();

        stopWatch.start();
        SqOprfReceiverOutput receiverOutput = handleBlindPrfPayload(blindPrfPayload);
        stopWatch.stop();
        long deBlindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, deBlindTime, "Receiver de-blinds");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private List<byte[]> generateBlindPayload() {
        BigInteger n = byteFullEcc.getN();
        inverseBetas = new BigInteger[batchSize];
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(index -> {
                // generate β
                BigInteger beta = byteFullEcc.randomZn(secureRandom);
                inverseBetas[index] = BigIntegerUtils.modInverse(beta, n);
                // hash to point
                byte[] element = byteFullEcc.hashToCurve(inputs[index]);
                // blind
                return byteFullEcc.mul(element, beta);
            })
            .collect(Collectors.toList());
    }

    private SqOprfReceiverOutput handleBlindPrfPayload(List<byte[]> blindPrfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == batchSize);
        byte[][] blindPrfArray = blindPrfPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] prfs = batchIntStream
            .mapToObj(index -> byteFullEcc.mul(blindPrfArray[index], inverseBetas[index]))
            .map(kdf::deriveKey)
            .toArray(byte[][]::new);
        return new SqOprfReceiverOutput(inputs, prfs);
    }
}
