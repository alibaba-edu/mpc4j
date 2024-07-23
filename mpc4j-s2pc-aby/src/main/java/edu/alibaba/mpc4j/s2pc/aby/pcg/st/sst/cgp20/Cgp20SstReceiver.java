package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.AbstractSstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGP20-SST receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public class Cgp20SstReceiver extends AbstractSstReceiver {
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Cgp20SstReceiver(Rpc receiverRpc, Party senderParty, Cgp20SstConfig config) {
        super(Cgp20SstPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public SstReceiverOutput shareTranslate(int num, int byteLength) throws MpcAbortException {
        setPtoInput(num, byteLength);
        return shareTranslate();
    }

    @Override
    public SstReceiverOutput shareTranslate(int num, int byteLength, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, byteLength, preSenderOutput);
        this.cotSenderOutput = preSenderOutput;
        return shareTranslate();
    }

    private SstReceiverOutput shareTranslate() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the parties are going to run N executions of OPV protocol to generate N vectors v_1, ... , v_n
        BpRdpprfSenderOutput senderOutput = bpRdpprfSender.puncture(num, num, cotSenderOutput);
        cotSenderOutput = null;
        stopWatch.stop();
        long opvTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opvTime);

        stopWatch.start();
        Prg prg = PrgFactory.createInstance(envType, byteLength);
        // P_1 sets elements of a, b to be column- and row-wise sums of the matrix elements
        byte[][][] extendMatrix = IntStream.range(0, num)
            .mapToObj(i -> {
                SpRdpprfSenderOutput eachSenderOutput = senderOutput.get(i);
                byte[][] eachMatrix = eachSenderOutput.getV0Array();
                byte[][] eachExtendMatrix = new byte[num][];
                for (int j = 0; j < num; j++) {
                    eachExtendMatrix[j] = prg.extendToBytes(eachMatrix[j]);
                }
                return eachExtendMatrix;
            })
            .toArray(byte[][][]::new);
        IntStream aIntStream = IntStream.range(0, num);
        aIntStream = parallel ? aIntStream.parallel() : aIntStream;
        byte[][] as = aIntStream
            .mapToObj(i -> {
                byte[] a = new byte[byteLength];
                for (int j = 0; j < num; j++) {
                    BytesUtils.xori(a, extendMatrix[j][i]);
                }
                return a;
            })
            .toArray(byte[][]::new);
        IntStream bIntStream = IntStream.range(0, num);
        bIntStream = parallel ? bIntStream.parallel() : bIntStream;
        byte[][] bs = bIntStream
            .mapToObj(j -> {
                byte[] b = new byte[byteLength];
                for (int i = 0; i < num; i++) {
                    BytesUtils.xori(b, extendMatrix[j][i]);
                }
                return b;
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new SstReceiverOutput(as, bs);
    }
}
