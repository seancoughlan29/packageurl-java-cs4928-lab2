package com.github.packageurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * CS4928 Lab 2 independent behaviour checks.
 *
 * Copy to:
 *   src/test/java/com/github/packageurl/CS4928Lab2CheckTest.java
 *
 * Run with:
 *   mvn -Dtest=CS4928Lab2CheckTest test
 */
class CS4928Lab2CheckTest {

    @Test
    void typeSpecificCanonicalization() throws Exception {
        PackageURL github = new PackageURL("github", "Acme-Org", "RepoName", "V1", null, null);
        assertEquals("acme-org", github.getNamespace());
        assertEquals("reponame", github.getName());
        assertEquals("V1", github.getVersion());

        PackageURL pypi = new PackageURL("pypi", null, "My_Package", "2.0", null, null);
        assertEquals("my-package", pypi.getName());

        PackageURL pub = new PackageURL("pub", null, "My-Package", "3.0", null, null);
        assertEquals("my_package", pub.getName());

        PackageURL luarocks = new PackageURL("luarocks", "Org", "MyRock", "RC1", null, null);
        assertEquals("org", luarocks.getNamespace());
        assertEquals("myrock", luarocks.getName());
        assertEquals("rc1", luarocks.getVersion());

        PackageURL huggingFace = new PackageURL("huggingface", "OpenAI", "ModelName", "MAIN", null, null);
        assertEquals("OpenAI", huggingFace.getNamespace());
        assertEquals("ModelName", huggingFace.getName());
        assertEquals("main", huggingFace.getVersion());

        PackageURL oci = new PackageURL("oci", null, "MyImage", "LATEST", null, null);
        assertEquals("myimage", oci.getName());
        assertEquals("latest", oci.getVersion());
    }

    @Test
    void rpmNamespaceAndNameSemantics() throws Exception {
        PackageURL rpm = new PackageURL("rpm", "FEDORA", "PkgName", "1.0", null, null);
        assertEquals("fedora", rpm.getNamespace());
        assertEquals("PkgName", rpm.getName(),
                "RPM package name must be preserved; only the namespace is lowercased");
    }

    @Test
    void qualifierCanonicalizationFromMap() throws Exception {
        Map<String, String> qualifiers = new LinkedHashMap<>();
        qualifiers.put("Distro", "Fedora-40");
        qualifiers.put("Arch", "x86_64");
        qualifiers.put("Ignored", "");

        PackageURL purl = new PackageURL("generic", null, "Example", null, qualifiers, null);

        Map<String, String> expected = new TreeMap<>();
        expected.put("arch", "x86_64");
        expected.put("distro", "Fedora-40");
        assertEquals(expected, purl.getQualifiers());
        assertEquals("pkg:generic/Example?arch=x86_64&distro=Fedora-40", purl.canonicalize());
    }

    @Test
    void caseInsensitiveDuplicateQualifiersAreRejected() {
        assertThrows(
                MalformedPackageURLException.class,
                () -> new PackageURL("pkg:generic/example?Arch=x86_64&arch=arm64"));
    }

    @Test
    void typeConstraintsRemainEnforced() throws Exception {
        assertThrows(
                MalformedPackageURLException.class,
                () -> new PackageURL("maven", null, "artifact", "1.0", null, null));

        assertThrows(
                MalformedPackageURLException.class,
                () -> new PackageURL("oci", "namespace-not-allowed", "image", "latest", null, null));

        PackageURL validMaven = new PackageURL("maven", "org.example", "artifact", "1.0", null, null);
        assertEquals("org.example", validMaven.getNamespace());
        assertEquals("artifact", validMaven.getName());
    }
}
