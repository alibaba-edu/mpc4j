package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.AbstractBstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLT24-BST sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24BstSender extends AbstractBstSender {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfReceiver bpCdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Lll24BstSender(Rpc senderRpc, Party receiverParty, Lll24BstConfig config) {
        super(Lll24BstPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bpCdpprfReceiver = BpCdpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpCdpprfConfig());
        addSubPto(bpCdpprfReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bpCdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength) throws MpcAbortException {
        setPtoInput(piArray, byteLength);
        return shareTranslate();
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(piArray, byteLength, preReceiverOutput);
        this.cotReceiverOutput = preReceiverOutput;
        return shareTranslate();
    }

    private BstSenderOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P_0’s input in execution i is π(i)
        int paddingLogEachNum = LongUtils.ceilLog2(eachNum);
        int paddingEachNum = (1 << paddingLogEachNum);
        int[] alphaFlattenArray;
        if (eachNum > 2) {
            alphaFlattenArray = new int[(eachNum - 1) * batchNum];
            IntStream.range(0, batchNum).forEach(i ->
                System.arraycopy(piArray[i], 0, alphaFlattenArray, (eachNum - 1) * i, eachNum - 1));
        } else {
            alphaFlattenArray = Arrays.stream(piArray).flatMapToInt(Arrays::stream).toArray();
        }
        BpCdpprfReceiverOutput receiverOutput = bpCdpprfReceiver.puncture(
            alphaFlattenArray, paddingEachNum, cotReceiverOutput
        );
        cotReceiverOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SstSenderOutput[] senderOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                int offset = batchIndex * (eachNum > 2 ? eachNum - 1 : eachNum);
                // P_0 computes ∆[i] by taking the sum of column π(i) (except the element v_i[π(i)] which it doesn't know)
                // and adding the sum row i (again, except the element v_i[π(i)] which it doesn't know).
                byte[][][] extendMatrix = new byte[eachNum][][];
                if (eachNum > 2) {
                    byte[][] xorColumn = new byte[eachNum][CommonConstants.BLOCK_BYTE_LENGTH];
                    IntStream.range(0, eachNum - 1).map(i -> offset + i).forEach(i -> {
                        SpCdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(i);
                        byte[][] eachMatrix = eachReceiverOutput.getV1Array();
                        int pos = eachReceiverOutput.getAlpha();
                        eachMatrix[pos] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            if (j != eachReceiverOutput.getAlpha()) {
                                eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                                // update the column data xor result
                                BytesUtils.xori(xorColumn[j], eachMatrix[j]);
                            }
                        }
                        for (int j = 0; j < paddingEachNum; j++) {
                            if (j != pos) {
                                // update the column data with missing value in cdpprf
                                BytesUtils.xori(xorColumn[pos], eachMatrix[j]);
                            }
                        }
                        extendMatrix[i - offset] = eachExtendMatrix;
                    });
                    extendMatrix[eachNum - 1] = IntStream.range(0, eachNum).mapToObj(i ->
                        i != piArray[batchIndex][eachNum - 1] ? prg.extendToBytes(xorColumn[i]) : null
                    ).toArray(byte[][]::new);
                } else {
                    IntStream.range(0, eachNum).map(i -> offset + i).forEach(i -> {
                        SpCdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(i);
                        byte[][] eachMatrix = eachReceiverOutput.getV1Array();
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            if (j != eachReceiverOutput.getAlpha()) {
                                eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                            }
                        }
                        extendMatrix[i - offset] = eachExtendMatrix;
                    });
                }
                byte[][] deltas = IntStream.range(0, eachNum)
                    .mapToObj(i -> {
                        byte[] delta = new byte[byteLength];
                        // ⊕_{j ≠ i} v[j][π(i)]
                        for (int j = 0; j < eachNum; j++) {
                            if (j != i) {
                                BytesUtils.xori(delta, extendMatrix[j][piArray[batchIndex][i]]);
                            }
                        }
                        // ⊕_{j ≠ π(i)} v[i][j]
                        for (int j = 0; j < eachNum; j++) {
                            if (j != piArray[batchIndex][i]) {
                                BytesUtils.xori(delta, extendMatrix[i][j]);
                            }
                        }
                        return delta;
                    })
                    .toArray(byte[][]::new);
                return new SstSenderOutput(piArray[batchIndex], deltas);
            })
            .toArray(SstSenderOutput[]::new);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new BstSenderOutput(senderOutputs);
    }
}
