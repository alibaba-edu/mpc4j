package edu.alibaba.mpc4j.common.tool.crypto.commit;

import org.bouncycastle.crypto.digests.SM3Digest;

import java.security.SecureRandom;

/**
 * Random Oracle model, using SM3 in Bouncy Castle.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
class RoBcSm3Commit extends AbstractRoBcCommit {

    RoBcSm3Commit() {
        super(CommitFactory.CommitType.RO_BC_SM3, new SM3Digest());
    }

    RoBcSm3Commit(SecureRandom secureRandom) {
        super(CommitFactory.CommitType.RO_BC_SM3, new SM3Digest(), secureRandom);
    }
}
