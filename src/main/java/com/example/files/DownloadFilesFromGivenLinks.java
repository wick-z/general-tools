package com.example.files;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @author wick.z
 * @since 2024/6/18
 */
public class DownloadFilesFromGivenLinks {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadFilesFromGivenLinks.class);

    public static void main(String[] args) throws IOException, URISyntaxException {
        String userDir = System.getProperty("user.dir");
        // 当前工作目录，也就是项目目录下的images目录
        Path images = Paths.get(userDir.concat(File.separator).concat("images"));
        deleteFiles(images.toFile());
//        Files.deleteIfExists(images);
        Files.createDirectory(images);

//        Stream<String> lines = Files.lines(Paths.get(userDir.concat(File.separator).concat("links.txt")));
        Stream<String> lines = Files.lines(Paths.get(DownloadFilesFromGivenLinks.class.getClassLoader()
                .getResource("links.txt").toURI()));
        AtomicReference<OutputStream> os = new AtomicReference<>();
        lines.forEach(url -> {
            try (InputStream in = new URL(url).openStream()){
                // 文件 如xxx.jpg
                String file = url.substring(url.lastIndexOf("/") + 1);
                if (file.split("\\.").length != 2) {
                    return;
                }
                // 截取URL中存在的文件目录
                LOG.info("{}下载中", url);
                String fileDirectory = getDirectoriesWithSeparatorFromURL(url);

                if (fileDirectory.contains("/")) {
                    StringBuilder directories = new StringBuilder();
                    String[] fileDirectories = fileDirectory.split("/");
                    for (String directory : fileDirectories) {
                        directories.append(directory);
                        Path dir = Paths.get(images.toFile().getAbsolutePath().concat(File.separator).concat(directories.toString().replace("%20", " ")));
                        if (!Files.exists(dir)) {
                            Files.createDirectory(dir);
                        }
                        directories.append(File.separator);
                    }
                } else {
                    Path dir = Paths.get(images.toFile().getAbsolutePath().concat(File.separator).concat(fileDirectory.replace("%20", " ")));
                    if (!Files.exists(dir)) {
                        Files.createDirectory(dir);
                    }
                }
                Path files = Files.createFile(Paths.get(images.toFile().getAbsolutePath().concat(File.separator).concat(fileDirectory.concat(File.separator).concat(file.replace("%20", " ")))));
                os.set(Files.newOutputStream(files));
                IOUtils.copy(in, os.get());
                LOG.info("{}下载完成", url);
            } catch (IOException e) {
                LOG.error("下载失败：{}", e.getMessage());
            } finally {
                IOUtils.closeQuietly(os.get());
            }
        });
    }

    private static void deleteFiles(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files.length == 0) {
            dir.delete();
        } else {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else {
                    deleteFiles(file);
                }
            }
        }

        dir.delete();
    }


//    private static String getDirectoriesWithSeparatorFromURL(String url, String file) {
//        String fileName = url.substring(url.lastIndexOf("/") + 1).substring(0, file.lastIndexOf("."));
//
//        String filePathOfServer = url.replace("https://mifengcha.oss-cn-beijing.aliyuncs.com/", "")
//                .replace("http://mifengcha.oss-cn-beijing.aliyuncs.com/", "")
//                .replace("https://echo-res.oss-cn-hongkong.aliyuncs.com/", "");
//        return filePathOfServer.substring(0, filePathOfServer.lastIndexOf(fileName) -1);
//    }

    /**
     * 从URL中获取文件所在目录<br>
     * e.g.
     *     <code>
     *     https://mifengcha.oss-cn-beijing.aliyuncs.com/static/user/avatar.jpg<br>
     *     截取之后变成 <b>static/user</b>
     *     </code>
     * @param url 链接
     * @return path
     */
    private static String getDirectoriesWithSeparatorFromURL(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        String path = null;
        try {
//            path = new URI(URLDecoder.decode(url.trim(), "UTF-8").trim()).getPath();
            path = new URI(url.trim()).getPath();
        } catch (URISyntaxException e) {
            // ignore
            LOG.warn("URL parsed error: {}", e.getMessage());
        } /*catch (UnsupportedEncodingException e) {
            // ignore
            LOG.warn("URL decode error: {}", e.getMessage());
        }*/
        if (Objects.nonNull(path) && path.contains(" ")) {
            path = path.replace(" ", "%20");
            return path.substring(1, path.lastIndexOf(fileName) - 1 );
        }
        return "";
    }
}
