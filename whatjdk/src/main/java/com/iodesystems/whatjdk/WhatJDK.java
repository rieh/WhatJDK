package com.iodesystems.whatjdk;

import org.docopt.Docopt;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WhatJDK {
    private static final String doc =
        "whatjdk\n"
            + "\n"
            + "Usage:\n"
            + "  whatjdk [JARFILE...]\n"
            + "  whatjdk (-h | --help)\n"
            + "  whatjdk (-v | --version)\n"
            + "\n"
            + "Options:\n"
            + "  -h --help          Show this screen.\n"
            + "  -v --version       Show version.\n"
            + "\n";

    public static void main(String[] args) throws Exception {
        Map<String, Object> opts = new Docopt(doc)
            .withVersion("1.0-SNAPSHOT")
            .parse(args);

        WhatJDK whatJDK = new WhatJDK();

        @SuppressWarnings("unchecked")
        List<String> jarfiles = (List<String>) opts.get("JARFILE");
        if (jarfiles != null) {
            for (String fileName : jarfiles) {
                whatJDK.checkFileExtensionCompatibility(fileName);
                whatJDK.extractClassVersionInfoForJar(fileName, new Handler() {
                    public void handle(String fileName, Set<String> versions) {
                        StringBuilder versionsString = new StringBuilder();
                        for (String version : versions) {
                            versionsString.append(version);
                            versionsString.append(", ");
                        }
                        System.out.println(fileName + " contains classes compatible with " + versionsString.substring(0, versionsString.length() - 2));
                    }
                });
            }
            System.exit(0);
        }
    }

    private void extractClassVersionInfo(String fileName,
                                         ZipInputStream zipInputStream,
                                         Set<String> versions) throws IOException {
        DataInputStream data = new DataInputStream(zipInputStream);
        if (0xCAFEBABE != data.readInt()) {
            System.err.println("Corrupt class file: " + fileName);
            return;
        }
        int minor = data.readUnsignedShort();
        int major = data.readUnsignedShort();
        switch (major) {
            case 45:
                versions.add("Java1.1");
                break;
            case 46:
                versions.add("Java1.2");
                break;
            case 47:
                versions.add("Java1.3");
                break;
            case 48:
                versions.add("Java1.4");
                break;
            case 49:
                versions.add("Java1.5");
                break;
            case 50:
                versions.add("Java1.6");
                break;
            case 51:
                versions.add("Java1.7");
                break;
            case 52:
                versions.add("Java1.8");
                break;
            default:
                versions.add(major + "." + minor);
        }
    }

    private void extractInternalClassOrLibVersion(String fileName, InputStream fileInputStream, Handler handler) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        ZipEntry entry;
        Set<String> versions = new HashSet<String>();
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (entry.getName().endsWith(".class")) {
                extractClassVersionInfo(fileName + ":" + entry.getName(), zipInputStream, versions);
            } else if (entry.getName().endsWith(".jar")) {
                extractInternalClassOrLibVersion(fileName + ":" + entry.getName(), zipInputStream, handler);
            }
        }
        if (!versions.isEmpty()) {
            handler.handle(fileName, versions);
        }
    }

    public void extractClassVersionInfoForJar(String fileName, Handler handler) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(fileName);
        extractInternalClassOrLibVersion(fileName, fileInputStream, handler);
        handler.onFinish();
    }

    public File checkFileExtensionCompatibility(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("File does not exist: " + fileName);
            System.exit(-1);
        }

        if (!file.isFile()) {
            System.err.println("Is not a file: " + fileName);
            System.exit(-1);
        }

        return file;
    }

    public static abstract class Handler {
        public abstract void handle(String fileName, Set<String> versions);

        public void onFinish() throws Exception {
        }
    }
}