package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.AbstractPmPeqtReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23ByteEccDdhPmPeqtPtoDesc.PtoStep;

/**
 * TCL23 pm-PEQT based on DDH receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class Tcl23ByteEccDdhPmPeqtReceiver extends AbstractPmPeqtReceiver {

    /**
     * alpha
     */
    private byte[] alpha;
    /**
     * ecc
     */
    private ByteMulEcc ecc;

    public Tcl23ByteEccDdhPmPeqtReceiver(Rpc receiverRpc, Party senderParty, Tcl23ByteEccDdhPmPeqtConfig config) {
        super(Tcl23ByteEccDdhPmPeqtPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init(int maxRow, int maxColumn) throws MpcAbortException {
        setInitInput(maxRow, maxColumn);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ecc = ByteEccFactory.createMulInstance(envType);
        alpha = ecc.randomScalar(secureRandom);
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
        List<byte[]> prfs = computePrf(inputMatrix);
        DataPacketHeader prfPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PRF.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfPayloadHeader, prfs));
        stopWatch.stop();
        long columnPermutationTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, columnPermutationTime, "receiver computes PRFs");

        DataPacketHeader prfPermutationPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PERMUTED_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfsPermutationPayload = rpc.receive(prfPermutationPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(prfsPermutationPayload.size() == 2 * row * column);

        stopWatch.start();
        boolean[][] result = handlePrfsPermutationPayload(prfsPermutationPayload);
        stopWatch.stop();
        long handlePrfsPermutationPayloadTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, handlePrfsPermutationPayloadTime, "Receiver equality test");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    /**
     * compute input PRFs.
     *
     * @param inputMatrix input matrix.
     * @return input PRFs.
     */
    private List<byte[]> computePrf(byte[][][] inputMatrix) {
        List<byte[]> inputList = new ArrayList<>();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                inputList.add(inputMatrix[i][j]);
            }
        }
        Stream<byte[]> inputStream = inputList.stream();
        inputStream = parallel ? inputStream.parallel() : inputStream;
        return inputStream
            .map(input -> ecc.hashToCurve(input))
            .map(hash -> ecc.mul(hash, alpha))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * handle PRFs permutation payload.
     *
     * @param prfsPermutationPayload PRFs permutation payload.
     * @return the equality.
     */
    private boolean[][] handlePrfsPermutationPayload(List<byte[]> prfsPermutationPayload) {
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) row * column) + 7;
        Hash peqtHash = HashFactory.createInstance(envType, CommonUtils.getByteLength(bitLength));
        List<byte[]> senderInput = prfsPermutationPayload.subList(0, row * column);
        List<byte[]> receiverInput = prfsPermutationPayload.subList(row * column, 2 * row * column);
        Stream<byte[]> inputStream = senderInput.stream();
        inputStream = parallel ? inputStream.parallel() : inputStream;
        List<byte[]> senderInputPrfs = inputStream
            .map(hash -> ecc.mul(hash, alpha))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toCollection(ArrayList::new));
        boolean[][] result = new boolean[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                result[i][j] = BytesUtils.equals(receiverInput.get(i * column + j), senderInputPrfs.get(i * column + j));
            }
        }
        return result;
    }
}
