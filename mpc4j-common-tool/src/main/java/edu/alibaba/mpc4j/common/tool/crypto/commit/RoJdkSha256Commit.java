package edu.alibaba.mpc4j.common.tool.crypto.commit;

import java.security.SecureRandom;

/**
 * Random Oracle model, using SHA256 in JDK.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
class RoJdkSha256Commit extends AbstractRoJdkCommit {
    /**
     * JDK SHA256 hash algorithm name
     */
    private static final String JDK_HASH_NAME = "SHA-256";
    /**
     * SHA256 belongs to MD4 family style digest, which hash byte length 64
     */
    private static final int BYTE_LENGTH = 64;

    RoJdkSha256Commit() {
        super(CommitFactory.CommitType.RO_JDK_SHA256, JDK_HASH_NAME, BYTE_LENGTH);
    }

    RoJdkSha256Commit(SecureRandom secureRandom) {
        super(CommitFactory.CommitType.RO_BC_SHA256, JDK_HASH_NAME, BYTE_LENGTH, secureRandom);
    }
}
