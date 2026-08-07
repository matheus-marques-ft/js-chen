import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

public class TestSSL {

    public static void main(String[] args) throws Exception {
        // Register BouncyCastle as a security provider
        java.security.Security.addProvider(new BouncyCastleProvider());

        // Read the CA certificate from the PEM file
        Certificate caCert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new FileInputStream("/Users/shenchenyang/Desktop/mysql-ssl/cert/ca.pem"));

        // Read the client certificate from the PEM file
        Certificate clientCert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new FileInputStream("/Users/shenchenyang/Desktop/mysql-ssl/cert/client-cert.pem"));


        // Read the private key from the PEM file
        PEMParser pemParser = new PEMParser(new FileReader("/Users/shenchenyang/Desktop/mysql-ssl/cert/client-key.pem"));

        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey privateKey;

        if (object instanceof PEMKeyPair) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) object;
            privateKey = converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
        } else if (object instanceof PrivateKeyInfo) {
            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
        }

        // Create a JKS keystore instance
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        // Import the private key and certificate into the keystore
        List<Certificate> certChain = new ArrayList<>();
        certChain.add(clientCert);
        certChain.add(caCert);
        keyStore.setKeyEntry("clientalias", privateKey, "123456".toCharArray(), certChain.toArray(new Certificate[0]));

        // Save as a JKS file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream("/Users/shenchenyang/Desktop/mysql-ssl/cert/client-keystore.jks")) {
            keyStore.store(fos, "123456".toCharArray());
        }
    }
}