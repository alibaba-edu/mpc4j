package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.AbstractPdsmSender;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 OPPRF-based PDSM sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22OpprfPdsmSender extends AbstractPdsmSender {
    /**
     * batched OPPRF sender
     */
    private final BopprfSender bopprfSender;
    /**
     * PEQT sender
     */
    private final PeqtParty peqtSender;

    public Cgs22OpprfPdsmSender(Rpc senderRpc, Party receiverParty, Cgs22OpprfPdsmConfig config) {
        super(Cgs22OpprfPdsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bopprfSender = BopprfFactory.createSender(senderRpc, receiverParty, config.getBopprfConfig());
        addSubPto(bopprfSender);
        peqtSender = PeqtFactory.createSender(senderRpc, receiverParty, config.getPeqtConfig());
        addSubPto(peqtSender);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bopprfSender.init(maxNum, maxNum * maxD);
        peqtSender.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pdsm(int l, byte[][][] inputArrays) throws MpcAbortException {
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
