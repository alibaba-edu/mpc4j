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
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.payable.AbstractPayablePirClient;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirPtoDesc.*;

/**
 * ZLP23 payable PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class Zlp23PayablePirClient extends AbstractPayablePirClient {

    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * keyword PIR client
     */
    private final KwPirClient kwPirClient;
    /**
     * β^{-1}
     */
    private BigInteger inverseBeta;

    public Zlp23PayablePirClient(Rpc clientRpc, Party serverParty, Zlp23PayablePirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        kwPirClient = KwPirFactory.createClient(clientRpc, serverParty, config.getKwPirConfig());
        addSubPto(kwPirClient);
    }

    @Override
    public void init(int serverElementSize, int valueByteLength) throws MpcAbortException {
        setInitInput(serverElementSize, valueByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        kwPirClient.init(1, serverElementSize, valueByteLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(ByteBuffer retrievalKey) throws MpcAbortException {
        setPtoInput(retrievalKey);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run MP-OPRF
        stopWatch.start();
        Map<ByteBuffer, byte[]> pirResult = kwPirClient.pir(Collections.singleton(retrievalKey));
        MpcAbortPreconditions.checkArgument(pirResult.size() <= 1);
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, kwPirTime, "Client executes keyword PIR");

        stopWatch.start();
        List<byte[]> blindPayload = new ArrayList<>();
        blindPayload.add(pirResult.size() == 0 ? IntUtils.intToByteArray(0) : IntUtils.intToByteArray(1));
        blindPayload.add(generateBlindPayload());
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client executes OPRF");

        stopWatch.start();
        byte[] result = null;
        if (pirResult.size() == 1) {
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
            MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == 1);
            result = handleBlindPrf(blindPrfPayload.get(0), pirResult.get(retrievalKey));
        }
        stopWatch.stop();
        long decodeResultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResultTime, "Client decodes result");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    /**
     * generate blind element.
     *
     * @return blind element.
     */
    private byte[] generateBlindPayload() {
        BigInteger n = ecc.getN();
        BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
        // generate blind factor
        inverseBeta = beta.modInverse(n);
        // hash to point
        byte[] element = ecc.hashToCurve(retrievalKey.array());
        // blinding
        return ecc.mul(element, beta);
    }

    /**
     * handle blind element PRF.
     *
     * @param blindPrf blind element PRF.
     * @return element PRF.
     */
    private byte[] handleBlindPrf(byte[] blindPrf, byte[] encryptedLabel) {
        byte[] elementPrf = ecc.mul(blindPrf, inverseBeta);
        Prg prg = PrgFactory.createInstance(envType, valueByteLength);
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] digest = hash.digestToBytes(elementPrf);
        byte[] extendedPrf = prg.extendToBytes(digest);
        return BytesUtils.xor(extendedPrf, encryptedLabel);
    }
}