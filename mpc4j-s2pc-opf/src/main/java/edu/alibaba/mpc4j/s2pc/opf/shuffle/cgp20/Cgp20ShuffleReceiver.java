package edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.AbstractShuffleReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGP20 shuffle receiver
 *
 * @author Feng Han
 * @date 2024/9/27
 */
public class Cgp20ShuffleReceiver extends AbstractShuffleReceiver {
    /**
     * the osn receiver
     */
    private final DosnReceiver dosnReceiver;
    /**
     * the osn sender
     */
    private final DosnSender dosnSender;

    public Cgp20ShuffleReceiver(Rpc receiverRpc, Party senderParty, Cgp20ShuffleConfig config) {
        super(Cgp20ShufflePtoDesc.getInstance(), receiverRpc, senderParty, config);
        dosnReceiver = DosnFactory.createReceiver(receiverRpc, senderParty, config.getDosnConfig());
        dosnSender = DosnFactory.createSender(receiverRpc, senderParty, config.getDosnConfig());
        addSubPto(dosnReceiver);
        addSubPto(dosnSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        dosnReceiver.init();
        dosnSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] shuffle(MpcZ2Vector[] xiArray, int dataNum, int dimNum) throws MpcAbortException {
        setPtoInput(xiArray, dataNum, dimNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[] pi = PermutationNetworkUtils.randomPermutation(dataNum, secureRandom);
        if(rowData == null){
            rowData = dosnReceiver.dosn(pi, CommonUtils.getByteLength(dimNum)).getShareVector();
        }else{
            byte[][] res = dosnReceiver.dosn(pi, rowData[0].length).getShareVector();
            rowData = PermutationNetworkUtils.permutation(pi, rowData);
            IntStream intStream = parallel ? IntStream.range(0, dataNum).parallel() : IntStream.range(0, dataNum);
            intStream.forEach(i -> BytesUtils.xori(rowData[i], res[i]));
        }
        stopWatch.stop();
        long firstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, firstTime);

        stopWatch.start();
        rowData = dosnSender.dosn(rowData, rowData[0].length).getShareVector();
        SquareZ2Vector[] res = getResultVectors();
        stopWatch.stop();
        long secondTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, secondTime);

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
