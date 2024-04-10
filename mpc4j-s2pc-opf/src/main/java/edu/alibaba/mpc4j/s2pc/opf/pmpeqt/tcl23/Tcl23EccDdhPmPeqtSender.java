package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.AbstractPmPeqtSender;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23EccDdhPmPeqtPtoDesc.PtoStep;

/**
 TCL23 pm-PEQT based on Ecc DDH sender.
 *
 * @author Liqiang Peng
 * @date 2024/4/1
 */
public class Tcl23EccDdhPmPeqtSender extends AbstractPmPeqtSender {

    /**
     * beta
     */
    private BigInteger beta;
    /**
     * ecc
     */
    private Ecc ecc;
    /**
     * compress encode
     */
    private final boolean compressEncode;

    public Tcl23EccDdhPmPeqtSender(Rpc senderRpc, Party receiverParty, Tcl23EccDdhPmPeqtConfig config) {
        super(Tcl23EccDdhPmPeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        this.compressEncode = config.isCompressEncode();
    }

    @Override
    public void init(int maxRow, int maxColumn) throws MpcAbortException {
        setInitInput(maxRow, maxColumn);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ecc = EccFactory.createInstance(envType);
        beta = ecc.randomZn(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pmPeqt(byte[][][] inputMatrix, int[] rowPermutationMap, int[] columnPermutationMap, int byteLength)
        throws MpcAbortException {
        setPtoInput(inputMatrix, rowPermutationMap, columnPermutationMap, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader prfPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> receiverPrfPayload = rpc.receive(prfPayloadHeader).getPayload();

        stopWatch.start();
        List<byte[]> prfsPermutation = handleReceiverPrfPayload(
            inputMatrix, receiverPrfPayload, rowPermutationMap, columnPermutationMap
        );
        DataPacketHeader prfPermutationPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PERMUTED_PRF.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfPermutationPayloadHeader, prfsPermutation));
        stopWatch.stop();
        long columnPermutationTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, columnPermutationTime, "sender computes PRFs permutation");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * handle receiver input PRFs payload.
     *
     * @param inputMatrix          input matrix.
     * @param receiverPrfPayload   receiver PRFs payload.
     * @param rowPermutationMap    row permutation map.
     * @param columnPermutationMap column permutation map.
     * @return input PRFs permutation.
     */
    private List<byte[]> handleReceiverPrfPayload(byte[][][] inputMatrix, List<byte[]> receiverPrfPayload,
                                                  int[] rowPermutationMap, int[] columnPermutationMap) {
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) row * column) + 7;
        Hash peqtHash = HashFactory.createInstance(envType, CommonUtils.getByteLength(bitLength));
        List<byte[]> inputList = new ArrayList<>();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                inputList.add(inputMatrix[i][j]);
            }
        }
        Stream<byte[]> inputStream = inputList.stream();
        inputStream = parallel ? inputStream.parallel() : inputStream;
        List<byte[]> inputPrfs = inputStream
            .map(input -> ecc.hashToCurve(input))
            .map(hash -> ecc.multiply(hash, beta))
            .map(point -> ecc.encode(point, compressEncode))
            .collect(Collectors.toCollection(ArrayList::new));
        // receiver PRFs
        Stream<byte[]> receiverPrfsStream = receiverPrfPayload.stream();
        receiverPrfsStream = parallel ? receiverPrfsStream.parallel() : receiverPrfsStream;
        List<byte[]> receiverPrfs = receiverPrfsStream
            .map(bytes -> ecc.decode(bytes))
            .map(hash -> ecc.multiply(hash, beta))
            .map(point -> ecc.encode(point, compressEncode))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toCollection(ArrayList::new));
        // PRFs permutation
        List<byte[]> prfsPermutation = new ArrayList<>();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                prfsPermutation.add(inputPrfs.get(rowPermutationMap[i] * column + columnPermutationMap[j]));
            }
        }
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                prfsPermutation.add(receiverPrfs.get(rowPermutationMap[i] * column + columnPermutationMap[j]));
            }
        }
        return prfsPermutation;
    }
}
