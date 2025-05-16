package edu.alibaba.mpc4j.crypto.phe;

import edu.alibaba.mpc4j.crypto.phe.impl.ou98.Ou98PheEngine;
import edu.alibaba.mpc4j.crypto.phe.impl.ou98.Ou98PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.impl.ou98.Ou98PhePublicKey;
import edu.alibaba.mpc4j.crypto.phe.impl.pai99.Pai99PheEngine;
import edu.alibaba.mpc4j.crypto.phe.impl.pai99.Pai99PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.impl.pai99.Pai99PhePublicKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;

import java.security.SecureRandom;
import java.util.List;

/**
 * 半同态加密算法工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class PheFactory {
    /**
     * 私有构造函数。
     */
    private PheFactory() {
        // empty
    }

    /**
     * 根据给定的半同态加密安全等级，返回模比特长度。
     *
     * @param pheType 半同态加密类型。
     * @param pheSecLevel 半同态加密安全等级。
     * @return 半同态加密模比特长度。
     */
    public static int getModulusBitLength(PheType pheType, PheSecLevel pheSecLevel) {
        if (pheType.equals(PheType.OU98)) {
            return switch (pheSecLevel) {
                case LAMBDA_40 -> 256;
                case LAMBDA_80 -> 512;
                case LAMBDA_112 -> 1024;
                case LAMBDA_128 -> 1536;
                case LAMBDA_192 -> 3840;
            };
        }
        if (pheType.equals(PheType.PAI99)) {
            return switch (pheSecLevel) {
                case LAMBDA_40 -> 512;
                case LAMBDA_80 -> 1024;
                case LAMBDA_112 -> 2048;
                case LAMBDA_128 -> 3072;
                case LAMBDA_192 -> 7680;
            };
        }
        throw new IllegalArgumentException("Invalid PheType:" + pheType);
    }

    public static PheEngine createInstance(PheType pheType, SecureRandom secureRandom) {
        return switch (pheType) {
            case OU98 -> new Ou98PheEngine(secureRandom);
            case PAI99 -> new Pai99PheEngine(secureRandom);
        };
    }

    public static PhePrivateKey phasePhePrivateKey(List<byte[]> byteArrayList) {
        int typeIndex = PheMathUtils.byteArrayToInt(byteArrayList.get(0));
        PheType pheType = PheType.values()[typeIndex];
        return switch (pheType) {
            case OU98 -> Ou98PhePrivateKey.deserialize(byteArrayList);
            case PAI99 -> Pai99PhePrivateKey.deserialize(byteArrayList);
        };
    }

    public static PhePublicKey phasePhePublicKey(List<byte[]> byteArrayList) {
        int typeIndex = PheMathUtils.byteArrayToInt(byteArrayList.get(0));
        PheType pheType = PheType.values()[typeIndex];
        return switch (pheType) {
            case OU98 -> Ou98PhePublicKey.deserialize(byteArrayList);
            case PAI99 -> Pai99PhePublicKey.deserialize(byteArrayList);
        };
    }
}
