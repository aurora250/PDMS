package com.pdm.resident.controller;

import com.pdm.common.core.result.Result;
import com.pdm.resident.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传/下载/删除控制器。
 * 统一文件存储入口，业务模块通过此接口管理附件、照片等文件。
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传文件
     * @param file 文件
     * @param type 业务类型: attachment | photo | excel
     * @return 文件访问相对路径
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "attachment") String type) {
        try {
            String path = fileStorageService.store(file, type);
            return Result.success("上传成功", path);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载/预览文件
     * @param path 相对路径（支持子路径，使用 ** 匹配）
     */
    @GetMapping("/{*path}")
    public ResponseEntity<Resource> download(@PathVariable String path) {
        Resource resource = fileStorageService.load(path);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 删除文件
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam String path) {
        try {
            fileStorageService.delete(path);
            return Result.success();
        } catch (IOException e) {
            log.error("文件删除失败: {}", path, e);
            return Result.fail("文件删除失败: " + e.getMessage());
        }
    }
}
