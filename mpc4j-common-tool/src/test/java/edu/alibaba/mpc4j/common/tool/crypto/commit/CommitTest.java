package edu.alibaba.mpc4j.common.tool.crypto.commit;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory.CommitType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.Commitment;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * commitment scheme tests. The test cases come form:
 * <p>
 * https://github.com/bcgit/bc-java/blob/master/core/src/test/java/org/bouncycastle/crypto/test/HashCommitmentTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
@RunWith(Parameterized.class)
public class CommitTest {
    /**
     * data
     */
    private static final byte[] DATA = Hex.decode("4e6f77206973207468652074696d6520666f7220616c6c20");

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RO_JDK_SHA256
        configurations.add(new Object[] {CommitType.RO_JDK_SHA256.name(), CommitType.RO_JDK_SHA256, });
        // RO_BC_SHA256
        configurations.add(new Object[] {CommitType.RO_BC_SHA256.name(), CommitType.RO_BC_SHA256, });
        // RO_BC_SM3
        configurations.add(new Object[] {CommitType.RO_BC_SM3.name(), CommitType.RO_BC_SM3, });

        return configurations;
    }

    /**
     * the type
     */
    public final CommitType type;

    public CommitTest(String name, CommitType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        Commit commit = CommitFactory.createInstance(type);
        Assert.assertEquals(type, commit.getType());
    }

    @Test
    public void basicTest() {
        Commit commit = CommitFactory.createInstance(type);
        // correct commitment
        Commitment correctCommitment = commit.commit(DATA);
        Assert.assertTrue(commit.isRevealed(DATA, correctCommitment));
        // try and fool it.
        Commitment foolCommitment = commit.commit(DATA);
        byte[] secret = foolCommitment.getSecret();
        byte[] newSecret = Arrays.copyOfRange(secret, 0, secret.length - 1);
        byte[] newData = new byte[DATA.length + 1];
        newData[0] = secret[secret.length - 1];
        System.arraycopy(DATA, 0, newData, 1, DATA.length);
        foolCommitment = new Commitment(newSecret, foolCommitment.getCommitment());
        Assert.assertFalse(commit.isRevealed(newData, foolCommitment));
        // try a message that's too big
        Assert.assertThrows(DataLengthException.class, () -> commit.commit(new byte[commit.maxMessageByteLength() + 1]));
    }

    @Test
    public void seedRandomTest() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // the first commitment
        SecureRandom seedSecureRandom0 = CommonUtils.createSeedSecureRandom();
        seedSecureRandom0.setSeed(seed);
        Commit commit0 = CommitFactory.createInstance(type, seedSecureRandom0);
        Commitment commitment0 = commit0.commit(DATA);
        // the second commitment
        SecureRandom seedSecureRandom1 = CommonUtils.createSeedSecureRandom();
        seedSecureRandom1.setSeed(seed);
        Commit commit1 = CommitFactory.createInstance(type, seedSecureRandom1);
        Commitment commitment1 = commit1.commit(DATA);
        // verify equality
        Assert.assertArrayEquals(commitment0.getSecret(), commitment1.getSecret());
        Assert.assertArrayEquals(commitment0.getCommitment(), commitment1.getCommitment());
    }
}
