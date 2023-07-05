package edu.alibaba.mpc4j.crypto.matrix.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.basic.MegaBinBasicOkvs;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MegaBin OKVS.
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
class MegaBinOkvs<T> implements Okvs<T> {
    /**
     * MegaBin field OKVS
     */
    private final MegaBinBasicOkvs megaBinFieldOkvs;
    /**
     * the prf used to hash the input to {0, 1}^l
     */
    private final Prf prf;

    MegaBinOkvs(EnvType envType, int n, int l, byte[][] keys) {
        megaBinFieldOkvs = new MegaBinBasicOkvs(envType, n, l, keys[0]);
        int byteL = megaBinFieldOkvs.getByteL();
        prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(keys[1]);
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        megaBinFieldOkvs.setParallelEncode(parallelEncode);
    }

    @Override
    public boolean getParallelEncode() {
        return megaBinFieldOkvs.getParallelEncode();
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        boolean parallelEncode = megaBinFieldOkvs.getParallelEncode();
        int l = megaBinFieldOkvs.getL();
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
        return megaBinFieldOkvs.encode(encodeKeyValueMap);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        int l = megaBinFieldOkvs.getL();
        byte[] mapKey = prf.getBytes(ObjectUtils.objectToByteArray(key));
        BytesUtils.reduceByteArray(mapKey, l);
        return megaBinFieldOkvs.decode(storage, ByteBuffer.wrap(mapKey));
    }

    @Override
    public int getN() {
        return megaBinFieldOkvs.getN();
    }

    @Override
    public int getL() {
        return megaBinFieldOkvs.getL();
    }

    @Override
    public int getByteL() {
        return megaBinFieldOkvs.getByteL();
    }

    @Override
    public int getM() {
        return megaBinFieldOkvs.getM();
    }

    @Override
    public OkvsFactory.OkvsType getOkvsType() {
        return OkvsFactory.OkvsType.MEGA_BIN;
    }

    @Override
    public int getNegLogFailureProbability() {
        return megaBinFieldOkvs.getNegLogFailureProbability();
    }
}
