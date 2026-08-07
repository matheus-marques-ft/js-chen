package mysql;

import com.alibaba.druid.pool.DruidDataSource;
import org.jumpserver.chen.modules.base.ssl.JKSGenerator;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Properties;

public class TestMysqlDriver {

    public static void main(String[] args) {
        try {


            ClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:/Users/shenchenyang/IdeaProjects/chen/drivers/mysql/mysql-connector-j-8.4.0.jar")});

            Driver driver = (Driver) classLoader.loadClass("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            DruidDataSource ds = new DruidDataSource();
            ds.setDriver(driver);

            Properties properties = new Properties();

            properties.setProperty("user", "ssluser");
            properties.setProperty("useSSL", "true");


            // Validate the client certificate; a CA is required if this option is set
            properties.setProperty("verifyServerCertificate", "true");

            // Client certificate; required when SSL is mandatory
            properties.setProperty("clientCertificateKeyStoreUrl", "file:/var/folders/3w/jyp02p1n57zfkvr5wwxmw9nc0000gn/T/jks17552071077163108422/client.jks");
            properties.setProperty("clientCertificateKeyStorePassword", JKSGenerator.JSK_PASS);
            properties.setProperty("clientKeyPassword", JKSGenerator.JSK_PASS);


            // CA certificate; required when verifyServerCertificate is true
            properties.setProperty("trustCertificateKeyStoreUrl", "file:/var/folders/3w/jyp02p1n57zfkvr5wwxmw9nc0000gn/T/jks17552071077163108422/ca.jks");
            properties.setProperty("trustCertificateKeyStorePassword", JKSGenerator.JSK_PASS);


            Connection conn = driver.connect("jdbc:mysql://localhost:3380/mysql", properties);

            System.out.println(conn);
            Statement stmt = conn.createStatement();


            stmt.execute("create database jms");

            System.out.println(stmt.getUpdateCount());


            ResultSet rs = stmt.getResultSet();

            int columns = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columns; i++) {
                System.out.print(rs.getMetaData().getColumnName(i));
                System.out.print("\t");
            }

            while (rs.next()) {
                System.out.println();
                for (int i = 1; i <= columns; i++) {
                    System.out.print(rs.getObject(i));
                    System.out.print("\t");
                }
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
