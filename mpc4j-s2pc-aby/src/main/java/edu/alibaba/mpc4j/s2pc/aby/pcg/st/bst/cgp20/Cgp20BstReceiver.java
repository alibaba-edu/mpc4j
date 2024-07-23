package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.AbstractBstReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGP20-BST receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
public class Cgp20BstReceiver extends AbstractBstReceiver {
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Cgp20BstReceiver(Rpc receiverRpc, Party senderParty, Cgp20BstConfig config) {
        super(Cgp20BstPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bpRdpprfSender = BpRdpprfFactory.createSender(receiverRpc, senderParty, config.getBpRdpprfConfig());
        addSubPto(bpRdpprfSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bpRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, byteLength);
        return shareTranslate();
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(batchNum, eachNum, byteLength, preSenderOutput);
        this.cotSenderOutput = preSenderOutput;
        return shareTranslate();
    }

    private BstReceiverOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the parties are going to run N executions of OPV protocol to generate N vectors v_1, ... , v_n
        BpRdpprfSenderOutput bpRdpprfSenderOutput = bpRdpprfSender.puncture(
            batchNum * eachNum, eachNum, cotSenderOutput
        );
        cotSenderOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SstReceiverOutput[] receiverOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                int offset = batchIndex * eachNum;
                // P_1 sets elements of a, b to be column- and row-wise sums of the matrix elements
                byte[][][] extendMatrix = IntStream.range(0, eachNum)
                    .map(i -> offset + i)
                    .mapToObj(i -> {
                        SpRdpprfSenderOutput spRdpprfSenderOutput = bpRdpprfSenderOutput.get(i);
                        byte[][] eachMatrix = spRdpprfSenderOutput.getV0Array();
                        byte[][] eachExtendMatrix = new byte[eachNum][];
                        for (int j = 0; j < eachNum; j++) {
                            eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                        }
                        return eachExtendMatrix;
                    })
                    .toArray(byte[][][]::new);
                byte[][] as = IntStream.range(0, eachNum)
                    .mapToObj(i -> {
                        byte[] a = new byte[byteLength];
                        for (int j = 0; j < eachNum; j++) {
                            BytesUtils.xori(a, extendMatrix[j][i]);
                        }
                        return a;
                    })
                    .toArray(byte[][]::new);
                byte[][] bs = IntStream.range(0, eachNum)
                    .mapToObj(j -> {
                        byte[] b = new byte[byteLength];
                        for (int i = 0; i < eachNum; i++) {
                            BytesUtils.xori(b, extendMatrix[j][i]);
                        }
                        return b;
                    })
                    .toArray(byte[][]::new);
                return new SstReceiverOutput(as, bs);
            })
            .toArray(SstReceiverOutput[]::new);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new BstReceiverOutput(receiverOutputs);
    }
}
