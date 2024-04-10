package edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprp.*;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.stream.IntStream;

/**
 * PSSW09 single-query OPRF Receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class Pssw09SqOprfReceiver extends AbstractSqOprfReceiver {
    /**
     * OPRP Receiver
     */
    private final OprpReceiver oprpReceiver;
    /**
     * hash function
     */
    private final Hash hash;

    public Pssw09SqOprfReceiver(Rpc receiverRpc, Party senderParty, Pssw09SqOprfConfig config) {
        super(Pssw09SqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        oprpReceiver = OprpFactory.createReceiver(receiverRpc, senderParty, config.getOprpConfig());
        addSubPto(oprpReceiver);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }


    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init OPRP
        oprpReceiver.init(maxBatchSize);
        stopWatch.stop();
        long initOprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initOprpTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // hash inputs
        byte[][] hashInputs = Arrays.stream(inputs).map(hash::digestToBytes).toArray(byte[][]::new);
        OprpReceiverOutput oprpReceiverOutput = oprpReceiver.oprp(hashInputs);
        stopWatch.stop();
        long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprpTime, "Receiver runs OPRP");

        DataPacketHeader sharesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_SHARES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sharesPayload = rpc.receive(sharesHeader).getPayload();

        stopWatch.start();
        // handle shares
        MpcAbortPreconditions.checkArgument(sharesPayload.size() == batchSize);
        byte[][] prfs = IntStream.range(0, batchSize)
            .mapToObj(index -> BytesUtils.xor(oprpReceiverOutput.getShare(index), sharesPayload.get(index)))
            .toArray(byte[][]::new);
        SqOprfReceiverOutput receiverOutput = new SqOprfReceiverOutput(inputs, prfs);
        stopWatch.stop();
        long sharesTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, sharesTime, "Receiver generate OPRFs.");

        logPhaseInfo(PtoState.PTO_END);

        return receiverOutput;
    }
}
