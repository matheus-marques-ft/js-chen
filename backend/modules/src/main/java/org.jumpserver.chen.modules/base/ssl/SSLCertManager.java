package org.jumpserver.chen.modules.base.ssl;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SSLCertManager {

    @Setter
    private String caCert;          // CA certificate
    @Setter
    private String clientCertKey;   // Client private key (PEM format)
    @Setter
    private String clientCert;      // Client certificate


    private File caCertFile;
    private File clientCertKeyFile;
    private File clientCertFile;

    // Get the path to the CA certificate
    public String getCaCertPath() throws IOException {
        if (StringUtils.isEmpty(caCert)) {
            return null;
        }

        if (caCertFile == null) {
            caCertFile = createTempFile("ca-cert", caCert);
        }
        return caCertFile.getAbsolutePath();
    }

    // Get the path to the client private key, converting it from PEM format to DER format
    public String getClientCertKeyPath() throws Exception {
        if (StringUtils.isEmpty(clientCertKey)) {
            return null;
        }

        if (clientCertKeyFile == null) {
            // Check whether clientCertKey is in PEM format and convert it to DER
            clientCertKeyFile = createTempFile("client-cert-key", convertPEMToDER(clientCertKey));
        }
        return clientCertKeyFile.getAbsolutePath();
    }

    // Get the path to the client certificate
    public String getClientCertPath() throws IOException {

        if (StringUtils.isEmpty(clientCert)) {
            return null;
        }

        if (clientCertFile == null) {
            clientCertFile = createTempFile("client-cert", clientCert);
        }
        return clientCertFile.getAbsolutePath();
    }

    // Destroy resources; delete temp files if autoDestroy is true
    public void Destroy() {
        deleteTempFile(caCertFile);
        deleteTempFile(clientCertKeyFile);
        deleteTempFile(clientCertFile);
    }

    // Helper method: create a temp file and write content to it
    private File createTempFile(String prefix, byte[] content) throws IOException {
        File tempFile = File.createTempFile(prefix, ".der");
        Files.write(tempFile.toPath(), content);  // Write binary data directly
        tempFile.deleteOnExit(); // Automatically deleted when the JVM exits
        return tempFile;
    }

    // Helper method: create a temp file and write content to it (for plain string content)
    private File createTempFile(String prefix, String content) throws IOException {
        File tempFile = File.createTempFile(prefix, ".pem");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        tempFile.deleteOnExit(); // Automatically deleted when the JVM exits
        return tempFile;
    }

    // Helper method: delete a temp file
    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
                System.out.println("Deleted file: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + file.getAbsolutePath());
            }
        }
    }

    // Convert a private key from PEM format to DER format
    private byte[] convertPEMToDER(String pemContent) throws Exception {
        // Strip the PEM header/footer markers to get the Base64-encoded content
        pemContent = pemContent.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");  // Strip whitespace and newlines

        // Base64 decode
        byte[] keyBytes = Base64.getDecoder().decode(pemContent);

        // Use PKCS8EncodedKeySpec to generate the PrivateKey object
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");  // Assume an RSA private key
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        // Return the private key byte array in DER format
        return privateKey.getEncoded();
    }
}
