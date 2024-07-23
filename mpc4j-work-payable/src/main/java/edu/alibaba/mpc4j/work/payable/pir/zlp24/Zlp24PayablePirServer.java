package edu.alibaba.mpc4j.work.payable.pir.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.KeyPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.work.payable.pir.AbstractPayablePirServer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirPtoDesc.getInstance;

/**
 * ZLP24 payable PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class Zlp24PayablePirServer extends AbstractPayablePirServer {

    /**
     * PRF key
     */
    private BigInteger alpha;
    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * keyword PIR server
     */
    private final KeyPirServer<ByteBuffer> kwPirServer;

    public Zlp24PayablePirServer(Rpc serverRpc, Party clientParty, Zlp24PayablePirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        kwPirServer = StdKsPirFactory.createServer(serverRpc, clientParty, config.getKsPirConfig());
        addSubPto(kwPirServer);
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keywordLabelMap, int byteL) throws MpcAbortException {
        setInitInput(keywordLabelMap, byteL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // encode map
        Map<ByteBuffer, byte[]> encodedMap = computeKeywordPrf(keywordLabelMap);
        kwPirServer.init(encodedMap, byteL * Byte.SIZE, 1);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        kwPirServer.pir();
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, kwPirTime, "Server executes keyword PIR");

        List<byte[]> blindPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_BLIND.ordinal());
        MpcAbortPreconditions.checkArgument(blindPayload.size() == 2);

        stopWatch.start();
        boolean output = false;
        int flag = IntUtils.byteArrayToInt(blindPayload.get(0));
        if (flag == 1) {
            byte[] blindPrfPayload = handleBlindPayload(blindPayload.get(1));
            sendOtherPartyPayload(PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), Collections.singletonList(blindPrfPayload));
            output = true;
        } else if (flag != 0) {
            MpcAbortPreconditions.checkArgument(false, "signal is incorrect");
        }
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, oprfTime, "Server executes OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }

    /**
     * handle blind element.
     *
     * @param blindElement blind element.
     * @return blind element prf.
     */
    private byte[] handleBlindPayload(byte[] blindElement) {
        return ecc.mul(blindElement, alpha);
    }

    /**
     * compute keyword prf.
     *
     * @return keyword prf.
     */
    private Map<ByteBuffer, byte[]> computeKeywordPrf(Map<ByteBuffer, byte[]> keywordLabelMap) {
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream intStream = parallel ? IntStream.range(0, n).parallel() : IntStream.range(0, n);
        List<byte[]> encodedLabel = intStream
            .mapToObj(i -> {
                byte[] point = ecc.hashToCurve(keywordList.get(i).array());
                byte[] prf = ecc.mul(point, alpha);
                byte[] digest = hash.digestToBytes(prf);
                byte[] extendedPrf = prg.extendToBytes(digest);
                return BytesUtils.xor(extendedPrf, keywordLabelMap.get(keywordList.get(i)));
            })
            .toList();
        return IntStream.range(0, n)
            .boxed()
            .collect(
                Collectors.toMap(
                    i -> keywordList.get(i), encodedLabel::get, (a, b) -> b, () -> new HashMap<>(n)
                )
            );
    }
}
