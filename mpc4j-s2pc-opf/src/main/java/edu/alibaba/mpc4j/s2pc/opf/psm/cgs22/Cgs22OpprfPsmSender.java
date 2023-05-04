package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.AbstractPsmSender;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 OPPRF-based PSM sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22OpprfPsmSender extends AbstractPsmSender {
    /**
     * batched OPPRF sender
     */
    private final BopprfSender bopprfSender;
    /**
     * PEQT sender
     */
    private final PeqtParty peqtSender;

    public Cgs22OpprfPsmSender(Rpc senderRpc, Party receiverParty, Cgs22OpprfPsmConfig config) {
        super(Cgs22OpprfPsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bopprfSender = BopprfFactory.createSender(senderRpc, receiverParty, config.getBopprfConfig());
        addSubPtos(bopprfSender);
        peqtSender = PeqtFactory.createSender(senderRpc, receiverParty, config.getPeqtConfig());
        addSubPtos(peqtSender);
    }

    @Override
    public void init(int maxL, int d, int maxNum) throws MpcAbortException {
        setInitInput(maxL, d, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bopprfSender.init(maxNum, maxNum * d);
        peqtSender.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psm(int l, byte[][][] inputArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P0 samples a random target value t and prepares a set T such that it has d elements all equal to t.
        byte[][] targetArray = new byte[num][];
        for (int index = 0; index < num; index++) {
            targetArray[index] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        byte[][][] targetArrays = new byte[num][d][byteL];
        for (int index = 0; index < num; index++) {
            for (int i = 0; i < d; i++) {
                targetArrays[index][i] = BytesUtils.clone(targetArray[index]);
            }
        }
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 and P1 invoke F_{OPPRF} in which P0 plays the role of sender with input set B and target multi-set T
        bopprfSender.opprf(l, inputArrays, targetArrays);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        // P0 and P1 call Feq with inputs t and w and receive bits y0 and y1 respectively

        SquareZ2Vector z0 = peqtSender.peqt(l, targetArray);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }
}
