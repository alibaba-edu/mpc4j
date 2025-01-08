package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.ccot;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.ccot.CcotConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using core COT receiver.
 *
 * @author Weiran Liu
 * @date 2024/10/10
 */
public class CcotConv32Receiver extends AbstractConv32Party {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;

    public CcotConv32Receiver(Rpc receiverRpc, Party senderParty, CcotConv32Config config) {
        super(CcotConv32PtoDesc.getInstance(), receiverRpc, senderParty, config);
        CoreCotConfig coreCotConfig = config.getCoreCotConfig();
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, coreCotConfig);
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init() throws MpcAbortException {
        // each conversion needs 2 COTs
        init(1 << 29);
    }

    @Override
    public byte[] conv(byte[] w1) throws MpcAbortException {
        setPtoInput(w1);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // two parties generate (x_0, x_1, y_0, y_1), (x′_0, x′_1, y′_0, y'_1), where y_1 = w_{1,0} and y'_1 = w_{1,1}
        // we follow f^{ab} of [ALSZ13]
        stopWatch.start();
        boolean[] w10Binary = new boolean[num];
        boolean[] w11Binary = new boolean[num];
        BitVector y1 = BitVectorFactory.createZeros(num);
        BitVector y1p = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            w10Binary[i] = (w1[i] & 0b00000001) != 0;
            y1.set(i, w10Binary[i]);
            w11Binary[i] = (w1[i] & 0b00000010) != 0;
            y1p.set(i, w11Binary[i]);
        });
        // run first COT
        BitVector x1 = BitVectorFactory.createZeros(num);
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(w10Binary);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        byte[][] rbArray = rotReceiverOutput.getRbArray();
        for (int i = 0; i < num; i++) {
            x1.set(i, (rbArray[i][0] & 0b00000001) != 0);
        }
        // run second COT
        BitVector x1p = BitVectorFactory.createZeros(num);
        cotReceiverOutput = coreCotReceiver.receive(w11Binary);
        rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        rbArray = rotReceiverOutput.getRbArray();
        for (int i = 0; i < num; i++) {
            x1p.set(i, (rbArray[i][0] & 0b00000001) != 0);
        }
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate 1-out-of-2 bit ROT");

        stopWatch.start();
        // P0 sends (t_0, t_1) to P1.
        List<byte[]> t0t1Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_T0_T1.ordinal());
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == 2);
        BitVector t0 = BitVectorFactory.create(num, t0t1Payload.get(0));
        BitVector t1 = BitVectorFactory.create(num, t0t1Payload.get(1));
        // P1 compute v_1 = (w_{1,0} · t_0) ⊕ (w_{1,1} · t_1) ⊕ x_{1,0} ⊕ x_{1,1}
        BitVector v1 = y1.and(t0);
        v1.xori(y1p.and(t1));
        v1.xori(x1);
        v1.xori(x1p);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, shareTime);

        logPhaseInfo(PtoState.PTO_END);
        // P1 outputs v1
        return v1.getBytes();
    }
}
