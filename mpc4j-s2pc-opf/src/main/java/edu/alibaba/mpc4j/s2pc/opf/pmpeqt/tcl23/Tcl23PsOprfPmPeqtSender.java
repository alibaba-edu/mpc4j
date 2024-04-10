package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.AbstractPmPeqtSender;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.Tcl23PsOprfPmPeqtPtoDesc.getInstance;

/**
 * TCL23 pm-PEQT from Permute Share and mp-OPRF sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public class Tcl23PsOprfPmPeqtSender extends AbstractPmPeqtSender {

    /**
     * OSN receiver
     */
    private final OsnReceiver osnReceiver;
    /**
     * OPRF sender
     */
    private final OprfSender oprfSender;

    public Tcl23PsOprfPmPeqtSender(Rpc senderRpc, Party receiverParty, Tcl23PsOprfPmPeqtConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        addSubPto(osnReceiver);
        oprfSender = OprfFactory.createOprfSender(senderRpc, receiverParty, config.getOprfConfig());
        addSubPto(oprfSender);
    }

    @Override
    public void init(int maxRow, int maxColumn) throws MpcAbortException {
        setInitInput(maxRow, maxColumn);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // initialize OSN receiver
        osnReceiver.init(maxRow * maxColumn);
        // initialize OPRF sender
        oprfSender.init(maxRow * maxColumn);
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

        stopWatch.start();
        // combine row permutation and column permutation
        int[] permutationMap = new int[row * column];
        for (int i = 0; i < row; ++i) {
            for (int j = 0; j < column; j++) {
                permutationMap[i * column + j]  = rowPermutationMap[i] * column + columnPermutationMap[j];
            }
        }
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(permutationMap, byteLength);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, osnTime, "sender executes OSN");

        stopWatch.start();
        // MP-OPRF
        OprfSenderOutput oprfSenderOutput = oprfSender.oprf(row * column);
        byte[][] shareMatrix = handleOsnOutput(inputMatrix, osnPartyOutput, rowPermutationMap, columnPermutationMap);
        List<byte[]> prfPayload = computePrf(shareMatrix, oprfSenderOutput);
        DataPacketHeader prfPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PRF.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfPayloadHeader, prfPayload));
        stopWatch.stop();
        long mpOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, mpOprfTime, "sender executes OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * handle OSN output.
     *
     * @param inputMatrix          input matrix.
     * @param osnPartyOutput       OSN output.
     * @param rowPermutationMap    row permutation map.
     * @param columnPermutationMap column permutation map.
     * @return share matrix.
     */
    private byte[][] handleOsnOutput(byte[][][] inputMatrix, OsnPartyOutput osnPartyOutput, int[] rowPermutationMap,
                                     int[] columnPermutationMap) {
        byte[][] shareMatrix = new byte[row * column][];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                shareMatrix[i * column + j] = BytesUtils.xor(
                    osnPartyOutput.getShare(i * column + j), inputMatrix[rowPermutationMap[i]][columnPermutationMap[j]]
                );
            }
        }
        return shareMatrix;
    }

    /**
     * compute PRFs.
     *
     * @param itemArray        item array.
     * @param oprfSenderOutput oprf sender output.
     * @return PRFs.
     */
    private List<byte[]> computePrf(byte[][] itemArray, OprfSenderOutput oprfSenderOutput) {
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) row * column) + 7;
        Hash peqtHash = HashFactory.createInstance(envType, CommonUtils.getByteLength(bitLength));
        IntStream intStream = IntStream.range(0, oprfSenderOutput.getBatchSize());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> oprfSenderOutput.getPrf(i, itemArray[i]))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toList());
    }
}