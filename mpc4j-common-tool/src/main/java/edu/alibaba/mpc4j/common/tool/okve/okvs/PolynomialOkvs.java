package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.basic.PolyBasicOkvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Polynomial OKVS.
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
class PolynomialOkvs<T> implements Okvs<T> {
    /**
     * polynomial basic OKVS
     */
    private final PolyBasicOkvs polyBasicOkvs;
    /**
     * the prf used to hash the input to {0, 1}^l
     */
    private final Prf prf;

    PolynomialOkvs(EnvType envType, int n, int l, byte[] key) {
        polyBasicOkvs = new PolyBasicOkvs(envType, n, l);
        int byteL = polyBasicOkvs.getByteL();
        prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(key);
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        polyBasicOkvs.setParallelEncode(parallelEncode);
    }

    @Override
    public boolean getParallelEncode() {
        return polyBasicOkvs.getParallelEncode();
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        boolean parallelEncode = polyBasicOkvs.getParallelEncode();
        int l = polyBasicOkvs.getL();
        Stream<Map.Entry<T, byte[]>> entryStream = keyValueMap.entrySet().stream();
        entryStream = parallelEncode ? entryStream.parallel() : entryStream;
        Map<ByteBuffer, byte[]> encodeKeyValueMap = entryStream
            .collect(Collectors.toMap(
                entry -> {
                    T key = entry.getKey();
                    byte[] mapKey = prf.getBytes(ObjectUtils.objectToByteArray(key));
                    BytesUtils.reduceByteArray(mapKey, l);
                    return ByteBuffer.wrap(mapKey);
                },
                Map.Entry::getValue
            ));
        return polyBasicOkvs.encode(encodeKeyValueMap);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        int l = polyBasicOkvs.getL();
        byte[] mapKey = prf.getBytes(ObjectUtils.objectToByteArray(key));
        BytesUtils.reduceByteArray(mapKey, l);
        return polyBasicOkvs.decode(storage, ByteBuffer.wrap(mapKey));
    }

    @Override
    public int getN() {
        return polyBasicOkvs.getN();
    }

    @Override
    public int getL() {
        return polyBasicOkvs.getL();
    }

    @Override
    public int getByteL() {
        return polyBasicOkvs.getByteL();
    }

    @Override
    public int getM() {
        return polyBasicOkvs.getM();
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.POLYNOMIAL;
    }

    @Override
    public int getNegLogFailureProbability() {
        return polyBasicOkvs.getNegLogFailureProbability();
    }
}
