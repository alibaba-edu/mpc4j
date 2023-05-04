package edu.alibaba.mpc4j.common.tool.crypto.commit;

import org.bouncycastle.crypto.digests.SHA256Digest;

import java.security.SecureRandom;

/**
 * Random Oracle model, using SHA256 in Bouncy castle.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
class RoBcSha256Commit extends AbstractRoBcCommit {

    RoBcSha256Commit() {
        super(CommitFactory.CommitType.RO_BC_SHA256, new SHA256Digest());
    }

    RoBcSha256Commit(SecureRandom secureRandom) {
        super(CommitFactory.CommitType.RO_BC_SHA256, new SHA256Digest(), secureRandom);
    }
}
