package edu.alibaba.mpc4j.s2pc.pir.payable.zlp23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
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
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.payable.AbstractPayablePirServer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirPtoDesc.*;

/**
 * ZLP23 payable PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class Zlp23PayablePirServer extends AbstractPayablePirServer {

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
    private final KwPirServer kwPirServer;

    public Zlp23PayablePirServer(Rpc serverRpc, Party clientParty, Zlp23PayablePirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        kwPirServer = KwPirFactory.createServer(serverRpc, clientParty, config.getKwPirConfig());
        addSubPto(kwPirServer);
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keywordLabelMap, int labelByteLength) throws MpcAbortException {
        setInitInput(keywordLabelMap, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // encode map
        Map<ByteBuffer, byte[]> encodedMap = computeKeywordPrf(keywordLabelMap);
        kwPirServer.init(encodedMap, 1, labelByteLength);
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

        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        MpcAbortPreconditions.checkArgument(blindPayload.size() == 2);

        stopWatch.start();
        boolean output = false;
        int flag = IntUtils.byteArrayToInt(blindPayload.get(0));
        if (flag == 1) {
            byte[] blindPrfPayload = handleBlindPayload(blindPayload.get(1));
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, Collections.singletonList(blindPrfPayload)));
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
        Prg prg = PrgFactory.createInstance(envType, labelByteLength);
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream intStream = IntStream.range(0, keywordSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> encodedLabel = intStream
            .mapToObj(i -> {
                byte[] point = ecc.hashToCurve(keywordList.get(i).array());
                byte[] prf = ecc.mul(point, alpha);
                byte[] digest = hash.digestToBytes(prf);
                byte[] extendedPrf = prg.extendToBytes(digest);
                return BytesUtils.xor(extendedPrf, keywordLabelMap.get(keywordList.get(i)));
            })
            .collect(Collectors.toList());
        return IntStream.range(0, keywordSize)
            .boxed()
            .collect(
                Collectors.toMap(
                    i -> keywordList.get(i), encodedLabel::get, (a, b) -> b, () -> new HashMap<>(keywordSize)
                )
            );
    }
}