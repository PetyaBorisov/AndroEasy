package com.heckpet.androeasy

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import okio.ByteString.Companion.toByteString

object DuManager {
    private val provider = BouncyCastleProvider()

    fun generateCert(): Pair<ByteArray, ByteArray> {
        Security.addProvider(provider)
        val keyPair = KeyPairGenerator.getInstance("RSA", provider).apply { initialize(2048) }.genKeyPair()

        val subject = X500Name("CN=AndroEasy")
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            Date(),
            Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
            subject,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").setProvider(provider).build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider(provider).getCertificate(certHolder)

        return cert.encoded to keyPair.private.encoded
    }

    fun parseCert(certData: ByteArray): java.security.cert.X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(certData.inputStream()) as java.security.cert.X509Certificate
    }

    fun parseKey(keyData: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(keyData)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    fun byteArrayToBase64Url(bytes: ByteArray): String {
        return bytes.toByteString().base64Url()
    }
}