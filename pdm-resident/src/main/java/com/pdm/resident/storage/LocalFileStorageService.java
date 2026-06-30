package com.pdm.resident.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储实现。按日期分层存储，文件名使用UUID避免冲突。
 * 后续切换到飞书云存储时，实现 FileStorageService 接口并替换此 @Component 即可。
 */
@Slf4j
@Component
public class LocalFileStorageService implements FileStorageService {

    @Value("${pdm.storage.root-dir:/data/pdm-files}")
    private String rootDir;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String store(MultipartFile file, String subDir) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DATE_FMT);
        String uuidName = UUID.randomUUID().toString().replace("-", "") + extension;
        String relativePath = subDir + "/" + datePath + "/" + uuidName;

        Path targetPath = Paths.get(rootDir, relativePath);
        Files.createDirectories(targetPath.getParent());

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("文件已存储: {}", relativePath);
        return relativePath;
    }

    @Override
    public Resource load(String relativePath) {
        Path filePath = Paths.get(rootDir, relativePath).normalize();
        try {
            UrlResource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (IOException e) {
            log.warn("读取文件失败: {}", relativePath, e);
        }
        return null;
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Path filePath = Paths.get(rootDir, relativePath).normalize();
        Files.deleteIfExists(filePath);
        log.info("文件已删除: {}", relativePath);
    }
}
