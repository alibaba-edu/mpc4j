package edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.osorter.AbstractObSorter;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * bitonic sorter
 *
 * @author Feng Han
 * @date 2024/10/8
 */
public class BitonicSorter extends AbstractObSorter {
    /**
     * Z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit circuit;

    public BitonicSorter(Rpc ownRpc, Party otherParty, BitonicSorterConfig config) {
        super(BitonicSorterPtoDesc.getInstance(), ownRpc, otherParty, config);
        z2cParty = ownRpc.ownParty().getPartyId() == 0
            ? Z2cFactory.createSender(ownRpc, otherParty, config.getZ2cConfig())
            : Z2cFactory.createReceiver(ownRpc, otherParty, config.getZ2cConfig());
        circuit = new Z2IntegerCircuit(z2cParty, config.getZ2CircuitConfig());
        addSubPto(z2cParty);
    }

    @Override
    public void init() throws MpcAbortException {
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cParty.init();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, boolean needPermutation, boolean needStable) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        MpcZ2Vector[] resPerm = circuit.psort(new SquareZ2Vector[][]{xiArray}, null, null, needPermutation, needStable);
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, sortTime);

        logPhaseInfo(PtoState.PTO_END);
        if(needPermutation) {
            return Arrays.stream(resPerm).map(ea -> (SquareZ2Vector) ea).toArray(SquareZ2Vector[]::new);
        }else{
            return null;
        }
    }

    @Override
    public SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, SquareZ2Vector[] payloads, boolean needPermutation, boolean needStable) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        MpcZ2Vector[] resPerm = circuit.psort(new SquareZ2Vector[][]{xiArray}, new MpcZ2Vector[][]{payloads}, null, needPermutation, needStable);
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, sortTime);

        logPhaseInfo(PtoState.PTO_END);
        if(needPermutation) {
            return Arrays.stream(resPerm).map(ea -> (SquareZ2Vector) ea).toArray(SquareZ2Vector[]::new);
        }else{
            return null;
        }
    }
}
