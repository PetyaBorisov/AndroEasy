package com.heckpet.androeasy

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.math.BigInteger
import javax.security.auth.x500.X500Principal
import javax.net.ssl.KeyManagerFactory

// BouncyCastle
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

class HttpsServer(private val context: Context) : NanoHTTPD(8443) {

    init {
        val keyStore = generateKeyStore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, "password".toCharArray())
        makeSecure(makeSSLSocketFactory(keyStore, kmf), null)
        start()
    }

    private fun generateKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, "password".toCharArray())

        val keyPair = generateKeyPair()
        val certificate = generateCertificate(keyPair)

        keyStore.setKeyEntry("selfsigned", keyPair.private, "password".toCharArray(), arrayOf(certificate))
        return keyStore
    }

    private fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048, SecureRandom())
        return gen.generateKeyPair()
    }

    private fun generateCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val notBefore = now
        val notAfter = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 год

        val subject = X500Principal("CN=localhost")

        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val builder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val signerBuilder = JcaContentSignerBuilder("SHA256withRSA")
        val signer = signerBuilder.build(keyPair.private)

        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }
    private fun handleExec(session: IHTTPSession): Response {
        val cmd = session.parameters["cmd"]?.firstOrNull() ?: "id"
        val startTime = System.currentTimeMillis()

        val output = RootShell.exec(cmd)

        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        SubscriptionManager.addDuTime(context, duration)

        return newFixedLengthResponse(Response.Status.OK, "text/plain", output)
    }

    private fun handleStatus(): Response {
        val json = """
            {
                "level": "${SubscriptionManager.getLevel(context)}",
                "prompts_left": ${SubscriptionManager.getRemainingPrompts(context)},
                "du_time_left": "${SubscriptionManager.getRemainingDuTime(context)}",
                "active_connections": ${SubscriptionManager.getActiveConnections(context)}/${SubscriptionManager.getMaxConnections(context)}
            }
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun handleWebInterface(): Response {
        val html = """
            <!DOCTYPE html>
            <html><head><title>AndroEasy</title><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>body{font-family:sans-serif;text-align:center;padding:20px;background:#f0f0f0;}
            input,button{margin:10px;padding:10px;width:90%;font-size:16px;}
            pre{background:#fff;padding:15px;text-align:left;border-radius:8px;}</style>
            </head><body>
            <h1>AndroEasy</h1>
            <input type="text" id="cmd" placeholder="Введите команду" value="id">
            <button onclick="exec()">Выполнить</button>
            <button onclick="clearAll()">Очистить</button>
            <pre id="output">Готов к работе...</pre>
            <script>
                function exec() {
                    const cmd = document.getElementById('cmd').value;
                    fetch('/exec?cmd=' + encodeURIComponent(cmd))
                        .then(r => r.text())
                        .then(t => document.getElementById('output').textContent = t);
                }
                function clearAll() {
                    document.getElementById('cmd').value = '';
                    document.getElementById('output').textContent = '';
                }
            </script>
            </body></html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    override fun serve(session: IHTTPSession): Response {
        // Только для /exec и /status считаем подключение
        val isExec = session.uri == "/exec"
        val isStatus = session.uri == "/status"

        if (isExec || isStatus) {
            if (!SubscriptionManager.incrementConnection(context)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Лимит подключений")
            }
        }

        return try {
            when (session.uri) {
                "/exec" -> handleExec(session)
                "/status" -> handleStatus()
                else -> handleWebInterface()
            }
        } finally {
            if (isExec || isStatus) {
                SubscriptionManager.decrementConnection(context)
            }
        }
    }
}

private fun generateKeyStore(): KeyStore {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
    val subject = X500Principal("CN=AndroEasy")
    val cert = JcaX509v3CertificateBuilder(
        subject,
        BigInteger.ONE,
        Date(),
        Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
        subject,
        keyPair.public
    ).build(JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private))

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null, null)
    val x509: X509Certificate = java.security.cert.CertificateFactory.getInstance("X.509")
        .generateCertificate(cert.encoded.inputStream()) as X509Certificate
    ks.setKeyEntry("key", keyPair.private, "password".toCharArray(), arrayOf(x509))
    return ks
}

// Утилита для IP
fun getLocalIp(): String {
    return try {
        java.net.NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .first { !it.isLoopbackAddress && it is java.net.Inet4Address }
            .hostAddress ?: "127.0.0.1"
    } catch (e: Exception) { "127.0.0.1" }
}