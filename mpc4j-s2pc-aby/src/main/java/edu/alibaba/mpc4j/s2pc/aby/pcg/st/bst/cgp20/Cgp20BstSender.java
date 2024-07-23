package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.AbstractBstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGP20-BST sender.
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
public class Cgp20BstSender extends AbstractBstSender {
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfReceiver bpRdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Cgp20BstSender(Rpc senderRpc, Party receiverParty, Cgp20BstConfig config) {
        super(Cgp20BstPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bpRdpprfReceiver = BpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpRdpprfConfig());
        addSubPto(bpRdpprfReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bpRdpprfReceiver.init();
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
        int[] alphaFlattenArray = Arrays.stream(piArray).flatMapToInt(Arrays::stream).toArray();
        BpRdpprfReceiverOutput bpRdpprfReceiverOutput = bpRdpprfReceiver.puncture(
            alphaFlattenArray, eachNum, cotReceiverOutput
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
                int offset = batchIndex * eachNum;
                // P_0 computes ∆[i] by taking the sum of column π(i) (except the element v_i[π(i)] which it doesn't know)
                // and adding the sum row i (again, except the element v_i[π(i)] which it doesn't know).
                byte[][][] extendMatrix = IntStream.range(0, eachNum)
                    .map(i -> offset + i)
                    .mapToObj(i -> {
                        SpRdpprfReceiverOutput spRdpprfReceiverOutput = bpRdpprfReceiverOutput.get(i);
                        byte[][] eachMatrix = spRdpprfReceiverOutput.getV1Array();
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            if (j != spRdpprfReceiverOutput.getAlpha()) {
                                eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                            }
                        }
                        return eachExtendMatrix;
                    })
                    .toArray(byte[][][]::new);
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
