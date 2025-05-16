package edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Frodo client-preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/7/24
 */
public class FrodoCpIdxPirServer extends AbstractCpIdxPirServer implements HintCpIdxPirServer {
    /**
     * database
     */
    private IntMatrix db;

    public FrodoCpIdxPirServer(Rpc serverRpc, Party clientParty, FrodoCpIdxPirConfig config) {
        super(FrodoCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server derives a matrix A ∈ Z_q^{n×m}, where m is the num, n is the LWE dimension.
        byte[] seed = BlockUtils.randomBlock(secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime, "Server generates seed");

        stopWatch.start();
        // server runs D ← parse(DB, ρ), where parse encodes the DB into a matrix D ∈ Z_q^{m×ω}, where ω = w/ρ.
        // here we set ρ = 8, so that ω = byteL
        db = IntMatrix.createZeros(n, byteL);
        for (int i = 0; i < database.rows(); i++) {
            byte[] entry = database.getBytesData(i);
            assert entry.length == byteL;
            for (int j = 0; j < byteL; j++) {
                db.set(i, j, entry[j] & 0xFF);
            }
        }
        // server runs M ← A · D, recall that A ∈ Z_q^{n×m}
        IntMatrix matrixA = IntMatrix.createRandom(FrodoCpIdxPirPtoDesc.N, n, seed);
        IntMatrix matrixM = matrixA.mul(db);
        List<byte[]> hintPayload = IntStream.range(0, FrodoCpIdxPirPtoDesc.N)
            .mapToObj(i -> IntUtils.intArrayToByteArray(matrixM.getRow(i).getElements()))
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void answer() throws MpcAbortException {
        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == n);
        // generate response
        IntVector ans = db.leftMul(qu);
        List<byte[]> responsePayload = Collections.singletonList(IntUtils.intArrayToByteArray(ans.getElements()));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
