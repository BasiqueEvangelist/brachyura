package io.github.coolcrabs.brachyura.minecraft;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.dependency.Dependency;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.dependency.NativesJarDependency;
import io.github.coolcrabs.brachyura.exception.IncorrectHashException;
import io.github.coolcrabs.brachyura.minecraft.LauncherMeta.Version;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta.VMDependency;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta.VMDownload;
import io.github.coolcrabs.brachyura.util.MessageDigestUtil;
import io.github.coolcrabs.brachyura.util.NetUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;
import io.github.coolcrabs.brachyura.util.StreamUtil;
import io.github.coolcrabs.brachyura.util.Util;

public class Minecraft {
    private Minecraft() { }

    public static VersionMeta getVersion(String version) {
        try {
            Path versionJsonPath = mcCache().resolve(version).resolve("version.json");
            if (!Files.isRegularFile(versionJsonPath)) {
                for (Version metaVersion : LauncherMetaDownloader.getLauncherMeta().versions) {
                    if (metaVersion.id.equals(version)) {
                        Path tempPath = PathUtil.tempFile(versionJsonPath);
                        try {
                            try (InputStream inputStream = NetUtil.inputStream(NetUtil.url(metaVersion.url))) {
                                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception e) {
                            Files.delete(tempPath);
                            throw e;
                        }
                        PathUtil.moveAtoB(tempPath, versionJsonPath);
                        break;
                    }
                }
            }
            return new VersionMeta(PathUtil.inputStream(versionJsonPath));
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    public static Path getDownload(String version, VersionMeta meta, String download) {
        try {
            Path downloadPath = mcCache().resolve(version).resolve(download);
            if (!Files.isRegularFile(downloadPath)) {
                VMDownload downloadDownload = meta.getDownload(download);
                Path tempPath = PathUtil.tempFile(downloadPath);
                try {
                    MessageDigest messageDigest = MessageDigestUtil.messageDigest(MessageDigestUtil.SHA1);
                    try (DigestInputStream inputStream = new DigestInputStream(NetUtil.inputStream(NetUtil.url(downloadDownload.url)), messageDigest)) {
                        Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    String hash = MessageDigestUtil.toHexHash(messageDigest.digest());
                    if (!hash.equalsIgnoreCase(downloadDownload.sha1)) {
                        throw new IncorrectHashException(downloadDownload.sha1, hash);
                    }
                } catch (Exception e) {
                    Files.delete(tempPath);
                    throw e;
                }
                PathUtil.moveAtoB(tempPath, downloadPath);
            }
            return downloadPath;
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    public static List<Dependency> getDependencies(VersionMeta meta) {
        try {
            List<VMDependency> dependencyDownloads = meta.getDependencies();
            ArrayList<Dependency> result = new ArrayList<>();
            for (VMDependency dependency : dependencyDownloads) {
                Path artifactPath = null;
                Path nativesPath = null;
                Path sourcesPath = null;
                if (dependency.artifact != null) {
                    artifactPath = mcLibCache().resolve(dependency.artifact.path);
                    if (!Files.isRegularFile(artifactPath)) {
                        downloadDep(artifactPath, new URL(dependency.artifact.url), dependency.artifact.sha1);
                    }
                    Path noSourcesPath = mcLibCache().resolve(dependency.artifact.path + ".nosources");
                    if (!Files.isRegularFile(noSourcesPath)) {
                        String sourcesPath2 = dependency.artifact.path.replace(".jar", "-sources.jar");
                        String sourcesUrl = dependency.artifact.url.replace(".jar", "-sources.jar");
                        URL sourcesHashUrl = new URL(sourcesUrl + ".sha1");
                        String targetHash;
                        try {
                            try (InputStream hashStream = sourcesHashUrl.openStream()) {
                                targetHash = StreamUtil.readFullyAsString(hashStream);
                            }
                            // If we got this far sources exist
                            sourcesPath = mcLibCache().resolve(sourcesPath2);
                            downloadDep(sourcesPath, new URL(sourcesUrl), targetHash);
                        } catch (FileNotFoundException e) {
                            try {
                                sourcesUrl = sourcesUrl.replace("https://libraries.minecraft.net/", "https://repo.maven.apache.org/maven2/"); // WHY ???
                                sourcesHashUrl = new URL(sourcesUrl + ".sha1");
                                try (InputStream hashStream = sourcesHashUrl.openStream()) {
                                    targetHash = StreamUtil.readFullyAsString(hashStream);
                                }
                                // If we got this far sources exist
                                sourcesPath = mcLibCache().resolve(sourcesPath2);
                                downloadDep(sourcesPath, new URL(sourcesUrl), targetHash);
                            } catch (FileNotFoundException e2) {
                                Logger.info("No sources found for " + dependency.name + " (" + dependency.artifact.url + ")");
                                Files.createFile(noSourcesPath);
                            }
                        }
                        
                    }
                }
                if (dependency.natives != null) {
                    nativesPath = mcLibCache().resolve(dependency.natives.path);
                    if (!Files.isRegularFile(nativesPath)) {
                        downloadDep(nativesPath, new URL(dependency.natives.url), dependency.natives.sha1);
                    }
                }
                if (artifactPath != null) {
                    result.add(new JavaJarDependency(artifactPath, sourcesPath));
                }
                if (nativesPath != null) {
                    result.add(new NativesJarDependency(nativesPath));
                }
            }
            return result;
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    private static void downloadDep(Path downloadPath, URL url, String sha1) throws IOException {
        Path tempPath = PathUtil.tempFile(downloadPath);
        try {
            MessageDigest messageDigest = MessageDigestUtil.messageDigest(MessageDigestUtil.SHA1);
            try (DigestInputStream inputStream = new DigestInputStream(NetUtil.inputStream(url), messageDigest)) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String hash = MessageDigestUtil.toHexHash(messageDigest.digest());
            if (!hash.equalsIgnoreCase(sha1)) {
                throw new IncorrectHashException(sha1, hash);
            }
        } catch (Exception e) {
            Files.delete(tempPath);
            throw e;
        }
        PathUtil.moveAtoB(tempPath, downloadPath);
    }

    private static Path mcLibCache() {
        return mcCache().resolve("libraries");
    }

    private static Path mcCache() {
        return PathUtil.cachePath().resolve("minecraft");
    }
}
