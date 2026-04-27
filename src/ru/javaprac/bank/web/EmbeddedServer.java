package ru.javaprac.bank.web;

import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class EmbeddedServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("web.port", "8080"));
        String contextPath = System.getProperty("web.contextPath", "/bank");
        File webappDir = new File(System.getProperty("webapp.dir", "webapp")).getAbsoluteFile();

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        tomcat.addWebapp(contextPath, webappDir.getAbsolutePath());

        tomcat.start();
        System.out.println("Bank web application started at http://localhost:" + port + contextPath + "/");
        tomcat.getServer().await();
    }
}
