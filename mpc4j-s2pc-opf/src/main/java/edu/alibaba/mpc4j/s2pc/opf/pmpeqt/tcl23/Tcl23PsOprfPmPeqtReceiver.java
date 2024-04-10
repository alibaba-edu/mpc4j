package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.AbstractPmPeqtReceiver;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtPtoDesc.getInstance;

/**
 * TCL23 pm-PEQT from Permute Share and mp-OPRF receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public class Tcl23PsOprfPmPeqtReceiver extends AbstractPmPeqtReceiver {

    /**
     * OSN sender
     */
    private final OsnSender osnSender;
    /**
     * OPRF receiver
     */
    private final OprfReceiver oprfReceiver;

    public Tcl23PsOprfPmPeqtReceiver(Rpc receiverRpc, Party senderParty, Tcl23PsOprfPmPeqtConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        addSubPto(osnSender);
        oprfReceiver = OprfFactory.createOprfReceiver(receiverRpc, senderParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
    }

    @Override
    public void init(int maxRow, int maxColumn) throws MpcAbortException {
        setInitInput(maxRow, maxColumn);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // initialize OSN sender
        osnSender.init(maxRow * maxColumn);
        // initialize MP-OPRF receiver
        oprfReceiver.init(maxRow * maxColumn);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean[][] pmPeqt(byte[][][] inputMatrix, int byteLength, int row, int column) throws MpcAbortException {
        setPtoInput(inputMatrix, byteLength, row, column);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // input permutation
        Vector<byte[]> osnInputVector = generateOsnInputVector(inputMatrix);
        OsnPartyOutput osnPartyOutput = osnSender.osn(osnInputVector, byteLength);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, osnTime, "receiver executes OSN");

        stopWatch.start();
        // MP-OPRF
        byte[][] oprfInput = handleOsnOutput(osnPartyOutput);
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(oprfInput);
        DataPacketHeader prfPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> senderPrf = rpc.receive(prfPayloadHeader).getPayload();
        boolean[][] result = handleSenderPrf(senderPrf, oprfReceiverOutput);
        stopWatch.stop();
        long mpOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, mpOprfTime, "receiver executes OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    /**
     * generate OSN input vector.
     *
     * @param inputMatrix input matrix.
     * @return OSN input vector.
     */
    private Vector<byte[]> generateOsnInputVector(byte[][][] inputMatrix) {
        Vector<byte[]> payload = new Vector<>(row * column);
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                payload.add(inputMatrix[i][j]);
            }
        }
        return payload;
    }

    /**
     * handle OSN output.
     *
     * @param osnPartyOutput OSN output.
     * @return permuted matrix.
     */
    private byte[][] handleOsnOutput(OsnPartyOutput osnPartyOutput) {
        return IntStream.range(0, row * column)
            .mapToObj(osnPartyOutput::getShare)
            .toArray(byte[][]::new);
    }

    /**
     * check the equality between sender PRFs payload and receiver oprf output.
     * @param senderPrf          sender PRFs payload.
     * @param oprfReceiverOutput receiver oprf output.
     * @return the equality.
     */
    private boolean[][] handleSenderPrf(List<byte[]> senderPrf, OprfReceiverOutput oprfReceiverOutput) {
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) row * column) + 7;
        Hash peqtHash = HashFactory.createInstance(envType, CommonUtils.getByteLength(bitLength));
        IntStream intStream = IntStream.range(0, row * column);
        intStream = parallel ? intStream.parallel() : intStream;
        byte[][] items = intStream
            .mapToObj(oprfReceiverOutput::getPrf)
            .map(peqtHash::digestToBytes)
            .toArray(byte[][]::new);
        boolean[][] result = new boolean[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                result[i][j] = BytesUtils.equals(senderPrf.get(i * column + j), items[i * column + j]);
            }
        }
        return result;
    }
}
