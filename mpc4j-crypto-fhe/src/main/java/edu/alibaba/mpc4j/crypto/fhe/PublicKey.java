package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class to store a public key.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/publickey.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class PublicKey implements SealCloneable {
    /**
     * public key
     */
    private final Ciphertext pk;

    /**
     * Creates an empty public key.
     */
    public PublicKey() {
        pk = new Ciphertext();
    }

    /**
     * Returns a reference to the underlying data.
     *
     * @return a reference to the underlying data.
     */
    public Ciphertext data() {
        return pk;
    }

    /**
     * Returns a reference to parms_id.
     *
     * @return a reference to parms_id.
     */
    public ParmsId parmsId() {
        return pk.parmsId();
    }

    /**
     * Sets given parms_id.
     *
     * @param parmsId given parms_id.
     */
    public void setParmsId(ParmsId parmsId) {
        pk.setParmsId(parmsId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicKey)) {
            return false;
        }
        PublicKey publicKey = (PublicKey) o;
        return new EqualsBuilder()
            .append(pk, publicKey.pk)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(pk)
            .toHashCode();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        pk.saveMembers(outputStream);
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        pk.loadMembers(context, inputStream, version);
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        int inSize = pk.unsafeLoad(context, inputStream);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("PublicKey data is invalid");
        }
        return inSize;
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        pk.unsafeLoad(context, in);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("PublicKey data is invalid");
        }
    }
}
