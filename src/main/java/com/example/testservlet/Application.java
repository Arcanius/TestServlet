package com.example.testservlet;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Application {
    public static void main(String[] args) throws IOException {
        String webAppDir;
        String resourcePath = Application.class.getResource("").getPath();
        if (resourcePath.contains("jar!")) {
            String jarPath = resourcePath.substring(resourcePath.indexOf('/') + 1, resourcePath.indexOf('!'));
            webAppDir = extractWebapp(jarPath);
            Runtime.getRuntime().addShutdownHook(new WebappDeleter(webAppDir));
        } else {
            webAppDir = "src/main/resources/webapp";
        }
        Tomcat tomcat = new Tomcat();
        tomcat.getConnector();
        tomcat.addWebapp("", new File(webAppDir).getAbsolutePath());
        try {
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }

    private static String extractWebapp(String jarPath) throws IOException {
        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        Path workingDir = Paths.get(jarPath.substring(0, jarPath.lastIndexOf('/')));
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("webapp")) {
                if (entry.isDirectory()) {
                    Path dir = workingDir.resolve(name);
                    Files.createDirectory(dir);
                } else {
                    Path file = workingDir.resolve(name);
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return workingDir.toString() + "/webapp";
    }

    private static class WebappDeleter extends Thread {
        private final String webappDir;

        private WebappDeleter(String webappDir) {
            this.webappDir = webappDir;
        }

        @Override
        public void run() {
            String workingDir = webappDir.substring(0, webappDir.lastIndexOf('/'));
            deleteFile(new File(webappDir));
            deleteFile(new File(workingDir + "/tomcat.8080"));
        }

        private void deleteFile(File dir) {
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        deleteFile(file);
                    }
                }
            }
            dir.delete();
        }
    }
}
