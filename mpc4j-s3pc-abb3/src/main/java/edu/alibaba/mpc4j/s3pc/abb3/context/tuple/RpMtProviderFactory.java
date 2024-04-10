package edu.alibaba.mpc4j.s3pc.abb3.context.tuple;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2MtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer.RpZ2BufferMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer.RpZ2BufferMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file.RpZ2FileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file.RpZ2FileMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer.RpLongBufferMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer.RpLongBufferMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file.RpLongFileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file.RpLongFileMtpConfig;

/**
 * factory of mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpMtProviderFactory {
    /**
     * Z2 multiplication tuple provider type
     */
    public enum MtProviderType {
        /**
         * reading from file
         */
        FILE,
        /**
         * generate mt online and store in buffer
         */
        BUFFER,
    }

    public enum FilePtoWorkType {
        /**
         * only read tuples from files
         */
        ONLY_READ,
        /**
         * only write tuples into files
         */
        READ_WRITE,
        /**
         * testing, directly using the pre-generated tuples without deletion
         */
        TEST
    }

    public static RpZ2Mtp createRpZ2MtParty(Rpc rpc, RpZ2MtpConfig config, S3pcCrProvider crProvider){
        MtProviderType type = config.getProviderType();
        switch (type){
            case FILE:
                return new RpZ2FileMtp(rpc, (RpZ2FileMtpConfig) config, crProvider);
            case BUFFER:
                return new RpZ2BufferMtp(rpc, (RpZ2BufferMtpConfig) config, crProvider);
            default:
                throw new IllegalArgumentException("Invalid " + MtProviderType.class.getSimpleName() + ": " + type.name());
        }
    }

    public static RpLongMtp createRpZl64MtParty(Rpc rpc, RpLongMtpConfig config, S3pcCrProvider crProvider){
        MtProviderType type = config.getProviderType();
        switch (type){
            case FILE:
                return new RpLongFileMtp(rpc, (RpLongFileMtpConfig) config, crProvider);
            case BUFFER:
                return new RpLongBufferMtp(rpc, (RpLongBufferMtpConfig) config, crProvider);
            default:
                throw new IllegalArgumentException("Invalid " + MtProviderType.class.getSimpleName() + ": " + type.name());
        }
    }

    public static RpZ2MtpConfig createZ2MtpConfigTestMode(String fileDir){
        return new RpZ2FileMtpConfig.Builder(FilePtoWorkType.TEST, fileDir).build();
    }

    public static RpLongMtpConfig createZl64MtpConfigTestMode(String fileDir){
        return new RpLongFileMtpConfig.Builder(FilePtoWorkType.TEST, fileDir).build();
    }

    public static RpZ2MtpConfig createDefaultZ2MtpConfig(){
        return new RpZ2BufferMtpConfig.Builder().build();
    }

    public static RpLongMtpConfig createDefaultZl64MtpConfig(){
        return new RpLongBufferMtpConfig.Builder().build();
    }
}
