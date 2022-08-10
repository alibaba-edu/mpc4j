package edu.alibaba.mpc4j.crypto.phe;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
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
     * 半同态加密类型
     */
    public enum PheType {
        /**
         * OU98半同态加密
         */
        OU98,
        /**
         * Pai99半同态加密
         */
        PAI99,
    }

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
            switch (pheSecLevel) {
                case LAMBDA_40:
                    return 256;
                case LAMBDA_80:
                    return 512;
                case LAMBDA_112:
                    return 1024;
                case LAMBDA_128:
                    return 1536;
                case LAMBDA_192:
                    return 3840;
                default:
                    throw new IllegalArgumentException("Invalid PheSecLevel: " + pheSecLevel.name());
            }
        }
        if (pheType.equals(PheType.PAI99)) {
            switch (pheSecLevel) {
                case LAMBDA_40:
                    return 512;
                case LAMBDA_80:
                    return 1024;
                case LAMBDA_112:
                    return 2048;
                case LAMBDA_128:
                    return 3072;
                case LAMBDA_192:
                    return 7680;
                default:
                    throw new IllegalArgumentException("Invalid PheSecLevel: " + pheSecLevel.name());
            }
        }
        throw new IllegalArgumentException("Invalid PheType:" + pheType);
    }

    public static PheEngine createInstance(PheType pheType, SecureRandom secureRandom) {
        switch (pheType) {
            case OU98:
                return new Ou98PheEngine(secureRandom);
            case PAI99:
                return new Pai99PheEngine(secureRandom);
            default:
                throw new IllegalArgumentException("Invalid PheType: " + pheType.name());
        }
    }

    public static PhePrivateKey phasePhePrivateKey(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.get(0));
        PheType pheType = PheType.values()[typeIndex];
        switch (pheType) {
            case OU98:
                return Ou98PhePrivateKey.fromByteArrayList(byteArrayList);
            case PAI99:
                return Pai99PhePrivateKey.fromByteArrayList(byteArrayList);
            default:
                throw new IllegalArgumentException("Invalid PheType: " + pheType.name());
        }
    }

    public static PhePublicKey phasePhePublicKey(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.get(0));
        PheType pheType = PheType.values()[typeIndex];
        switch (pheType) {
            case OU98:
                return Ou98PhePublicKey.fromByteArrayList(byteArrayList);
            case PAI99:
                return Pai99PhePublicKey.fromByteArrayList(byteArrayList);
            default:
                throw new IllegalArgumentException("Invalid PheType: " + pheType.name());
        }
    }
}
