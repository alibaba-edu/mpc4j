package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.AbstractSstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24-SST sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24SstSender extends AbstractSstSender {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfReceiver bpCdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Lll24SstSender(Rpc senderRpc, Party receiverParty, Lll24SstConfig config) {
        super(Lll24SstPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
    public SstSenderOutput shareTranslate(int[] pi, int byteLength) throws MpcAbortException {
        setPtoInput(pi, byteLength);
        return shareTranslate();
    }

    @Override
    public SstSenderOutput shareTranslate(int[] pi, int byteLength, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(pi, byteLength, preReceiverOutput);
        this.cotReceiverOutput = preReceiverOutput;
        return shareTranslate();
    }

    private SstSenderOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P_0’s input in execution i is π(i)
        int paddingLogNum = LongUtils.ceilLog2(num);
        int paddingNum = (1 << paddingLogNum);
        int[] alphaArray = Arrays.copyOf(pi, pi.length > 2 ? pi.length - 1 : pi.length);
        BpCdpprfReceiverOutput receiverOutput = bpCdpprfReceiver.puncture(alphaArray, paddingNum, cotReceiverOutput);
        cotReceiverOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        // P_0 computes ∆[i] by taking the sum of column π(i) (except the element v_i[π(i)] which it doesn't know)
        // and adding the sum row i (again, except the element v_i[π(i)] which it doesn't know).
        byte[][][] extendMatrix = new byte[num][][];
        if (num > 2) {
            IntStream intStream = parallel ? IntStream.range(0, num - 1).parallel() : IntStream.range(0, num - 1);
            intStream.forEach(i -> {
                int pos = pi[i];
                SpCdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(i);
                byte[][] eachMatrix = eachReceiverOutput.getV1Array();
                eachMatrix[pos] = BlockUtils.zeroBlock();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    if (j != eachReceiverOutput.getAlpha()) {
                        eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                    }
                }
                for (int j = 0; j < paddingNum; j++) {
                    if (j != pos) {
                        BlockUtils.xori(eachMatrix[pos], eachMatrix[j]);
                    }
                }
                extendMatrix[i] = eachExtendMatrix;
            });
            intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            extendMatrix[num - 1] = intStream.mapToObj(i -> {
                if (i != pi[num - 1]) {
                    byte[] xorColumn = BlockUtils.zeroBlock();
                    BlockUtils.xori(xorColumn, receiverOutput.get(0).getV1(i));
                    for (int j = 1; j < num - 1; j++) {
                        BlockUtils.xori(xorColumn, receiverOutput.get(j).getV1(i));
                    }
                    return prg.extendToBytes(xorColumn);
                } else {
                    return null;
                }
            }).toArray(byte[][]::new);
        } else {
            IntStream.range(0, num).forEach(i -> {
                SpCdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(i);
                byte[][] eachMatrix = eachReceiverOutput.getV1Array();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    if (j != eachReceiverOutput.getAlpha()) {
                        eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                    }
                }
                extendMatrix[i] = eachExtendMatrix;
            });
        }
        IntStream matrixIntStream = IntStream.range(0, num);
        matrixIntStream = parallel ? matrixIntStream.parallel() : matrixIntStream;
        byte[][] deltas = matrixIntStream
            .mapToObj(i -> {
                byte[] delta = new byte[byteLength];
                // ⊕_{j ≠ i} v[j][π(i)]
                for (int j = 0; j < num; j++) {
                    if (j != i) {
                        BytesUtils.xori(delta, extendMatrix[j][pi[i]]);
                    }
                }
                // ⊕_{j ≠ π(i)} v[i][j]
                for (int j = 0; j < num; j++) {
                    if (j != pi[i]) {
                        BytesUtils.xori(delta, extendMatrix[i][j]);
                    }
                }
                return delta;
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new SstSenderOutput(pi, deltas);
    }
}
