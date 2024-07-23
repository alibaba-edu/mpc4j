package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.AbstractSstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGP20-SST sender.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public class Cgp20SstSender extends AbstractSstSender {
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfReceiver bpRdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Cgp20SstSender(Rpc senderRpc, Party receiverParty, Cgp20SstConfig config) {
        super(Cgp20SstPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        int[] alphaArray = IntUtils.clone(pi);
        BpRdpprfReceiverOutput receiverOutput = bpRdpprfReceiver.puncture(alphaArray, num, cotReceiverOutput);
        cotReceiverOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        // P_0 computes ∆[i] by taking the sum of column π(i) (except the element v_i[π(i)] which it doesn't know)
        // and adding the sum row i (again, except the element v_i[π(i)] which it doesn't know).
        byte[][][] extendMatrix = IntStream.range(0, num)
            .mapToObj(i -> {
                SpRdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(i);
                byte[][] eachMatrix = eachReceiverOutput.getV1Array();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    if (j != eachReceiverOutput.getAlpha()) {
                        eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                    }
                }
                return eachExtendMatrix;
            })
            .toArray(byte[][][]::new);
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
