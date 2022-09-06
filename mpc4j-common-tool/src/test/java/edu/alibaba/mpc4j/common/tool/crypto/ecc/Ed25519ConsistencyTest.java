package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.bc.Ed25519BcEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

/**
 * ED25519一致性验证，验证计算结果与给定结果相同。
 *
 * @author Weiran Liu
 * @date 2022/5/20
 */
@Ignore
public class Ed25519ConsistencyTest {
    /**
     * ED25519为32字节
     */
    private static final int POINT_BYTES = 32;
    /**
     * 幂指数α
     */
    private static final BigInteger ALPHA = new BigInteger(
        "764998408386434498650063003824440081808611984060012092244320073247301742515"
    );
    /**
     * g^α(x)
     */
    private static final byte[] G_ALPHA_X = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("7876664507159682633132477521255840025315944951227748335322447171232685367533"), POINT_BYTES
    );
    /**
     * g^α(y)
     */
    private static final byte[] G_ALPHA_Y = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("31016816897459880110323999941830720612217319498415592314272079868533821897997"), POINT_BYTES
    );
    /**
     * 幂指数β
     */
    private static final BigInteger BETA = new BigInteger(
        "2598828002408297514336950734774039995576618807096740469211589214623809778206"
    );
    /**
     * h(x)
     */
    private static final byte[] H_X = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("42572429724265472558398334719267503800263655539929302659183110677276162917069"), POINT_BYTES
    );
    /**
     * h(y)
     */
    private static final byte[] H_Y = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("32991777442502047834733154301734439447395950734002722249090623340224220922765"), POINT_BYTES
    );
    /**
     * h2(x)
     */
    private static final byte[] H2_X = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("46003588596552756198183672372580890404670942266204280681100759481710231668299"), POINT_BYTES
    );
    /**
     * h2(y)
     */
    private static final byte[] H2_Y = BigIntegerUtils.nonNegBigIntegerToByteArray(
        new BigInteger("48178738894543566948529880826026135470016267156310225805722646246747392688792"), POINT_BYTES
    );

    @Test
    public void testMultiplyConsistency() {
        Ed25519BcEcc ecc = new Ed25519BcEcc();
        byte[] h1ByteArray = ecc.encode(ecc.multiply(ecc.getG(), ALPHA), false);
        byte[] h1TruthByteArray = ByteBuffer.allocate(POINT_BYTES * 2).put(G_ALPHA_X).put(G_ALPHA_Y).array();
        Assert.assertArrayEquals(h1TruthByteArray, h1ByteArray);

        byte[] hByteArray = ByteBuffer.allocate(POINT_BYTES * 2).put(H_X).put(H_Y).array();
        ECPoint h = ecc.decode(hByteArray);
        Assert.assertTrue(h.isValid());
        byte[] h2ByteArray = ecc.encode(ecc.multiply(h, BETA), false);
        byte[] h2TruthByteArray = ByteBuffer.allocate(POINT_BYTES * 2).put(H2_X).put(H2_Y).array();
        Assert.assertArrayEquals(h2TruthByteArray, h2ByteArray);
    }

    /**
     * h = g^α，h^{SHA256(0)}, ... , h^{SHA256(15)}的计算结果
     */
    private static final BigInteger[][] H_ALPHA_ARRAYS = {
        {
            new BigInteger("22335856391072927105449895647094210937775716372573693974284655384253281580500"),
            new BigInteger("27601540535791630423455511749713626289435170836282113141883656829075177304392")
        },
        {
            new BigInteger("42572429724265472558398334719267503800263655539929302659183110677276162917069"),
            new BigInteger("32991777442502047834733154301734439447395950734002722249090623340224220922765")
        },
        {
            new BigInteger("15081188117796047526400075999382140107007353358536877904763449000337384350765"),
            new BigInteger("39961443324262537480102059364004515747152528018910807139129535033307534509673")
        },
        {
            new BigInteger("16687958960195156203280227059944575967299048566483297738084141734257505479132"),
            new BigInteger("19780523317041251702159889786484959282826250027487324724367879175596159177644")
        },
        {
            new BigInteger("6515217655580621151932155049520422315502717973926582732085761405996644682131"),
            new BigInteger("32826778719041100674034603300506297112067325374895711939764834290186195784232")
        },
        {
            new BigInteger("40343374909370523780789897632031322382098627369566673626292890158732799463986"),
            new BigInteger("12027591641790446240871367494051639366618881419696596552643298563181336361295")
        },
        {
            new BigInteger("31013255948760089706175797005708170529353057372732288289854362836574728277497"),
            new BigInteger("22018462180176383046494662707325030588212305257889358188280994329524286587449")
        },
        {
            new BigInteger("8933987511747861047340782263360712647612856390660336974329903258346859990694"),
            new BigInteger("40884994138753612583359409821725657811483692749740072844545276248033307283384")
        },
        {
            new BigInteger("25813562785511449560446152951083230696039007636608920390544563375256898506447"),
            new BigInteger("50260197224034585484559196821741033965885001012002924532579099497145956793177")
        },
        {
            new BigInteger("48936482626270341832934043573367489872580273730665185856040479950072785160087"),
            new BigInteger("57232510019792949495541511231214033241236451308798537604642733744887207939464")
        },
        {
            new BigInteger("45406585042743254302233681225484899118219325363005246698361931030551170506090"),
            new BigInteger("33746365853000154456216673495905103676025987073319741691337855834258800764605")
        },
        {
            new BigInteger("47611676293183961825605826570100606938072940207511442385015806699855607657330"),
            new BigInteger("32626211697756607352504136833505475773660270919487049181210733577132189341868")
        },
        {
            new BigInteger("38299873179876657268199949525825683540231984762909707247804584786795962785989"),
            new BigInteger("15981071943758352632388550953166805798931514085699890113090420410810332319118")
        },
        {
            new BigInteger("48663350462185543224758913950733943773378066891534778287159639880508964013452"),
            new BigInteger("40929182895626329333282643258614985295728828215934500405821059612548806940457")
        },
        {
            new BigInteger("17681518467911691046178304837222457907066950919929529133480871564438334870968"),
            new BigInteger("15338277010606213176868958182402048097356909894042570933028036881948000642578")
        }
    };

    @Test
    public void testScalarMultiply() {
        Ed25519BcEcc ecc = new Ed25519BcEcc();
        Hash hash = HashFactory.createInstance(HashFactory.HashType.NATIVE_SHA256, POINT_BYTES);
        // h = g^α
        ECPoint h = ecc.multiply(ecc.getG(), ALPHA);
        IntStream.range(0, H_ALPHA_ARRAYS.length).forEach(index -> {
            // UTF8编码
            byte[] message = String.valueOf(index).getBytes(StandardCharsets.UTF_8);
            // SHA256(m)
            BigInteger digestMessage = BigIntegerUtils.byteArrayToNonNegBigInteger(hash.digestToBytes(message));
            // h^m
            ECPoint multiplication = ecc.multiply(h, digestMessage);
            // 编码
            byte[] encode = ecc.encode(multiplication, false);
            byte[] correctEncode = ByteBuffer.allocate(POINT_BYTES * 2)
                .put(BigIntegerUtils.nonNegBigIntegerToByteArray(H_ALPHA_ARRAYS[index][0], POINT_BYTES))
                .put(BigIntegerUtils.nonNegBigIntegerToByteArray(H_ALPHA_ARRAYS[index][1], POINT_BYTES))
                .array();
            Assert.assertArrayEquals(correctEncode, encode);
        });
    }
}
