package com.pdm.resident.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储服务接口。
 * 当前使用本地文件系统实现，后续可切换到飞书云端存储（实现同一接口即可）。
 */
public interface FileStorageService {

    /**
     * 存储文件
     * @param file   上传文件
     * @param subDir 业务子目录（如 attachment, photo, excel）
     * @return 相对访问路径
     */
    String store(MultipartFile file, String subDir) throws IOException;

    /**
     * 读取文件
     * @param relativePath store() 返回的相对路径
     * @return Spring Resource
     */
    Resource load(String relativePath);

    /**
     * 删除文件
     * @param relativePath store() 返回的相对路径
     */
    void delete(String relativePath) throws IOException;
}
