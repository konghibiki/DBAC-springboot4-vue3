package com.example.springboot.system.service;

import com.example.springboot.system.config.ProjectConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * 头像存储服务
 * */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarStorageService {

    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    private final ProjectConfig projectConfig;

    /**
     * 头像由前端 Canvas 统一裁剪为 PNG 后保存到配置的本地资源目录。
     */
    public String storePngAvatar(MultipartFile file) {
        validateImage(file);

        String fileName = UUID.randomUUID() + ".png";
        Path avatarDirectory = getAvatarDirectory();
        Path target = avatarDirectory.resolve(fileName).normalize();

        if (!target.startsWith(avatarDirectory)) {
            throw new IllegalArgumentException("头像文件路径无效");
        }

        try {
            Files.createDirectories(avatarDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw new IllegalStateException("头像保存失败", error);
        }

        return "/profile/avatar/" + fileName;
    }

    public void deleteStoredAvatar(String avatarUrl) {
        String prefix = "/profile/avatar/";
        if (avatarUrl == null || !avatarUrl.startsWith(prefix)) {
            return;
        }

        String fileName = avatarUrl.substring(prefix.length());
        if (fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        String uuid = fileName.endsWith(".png")
                ? fileName.substring(0, fileName.length() - 4)
                : "";
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException invalidName) {
            return;
        }

        Path avatarDirectory = getAvatarDirectory();
        Path target = avatarDirectory.resolve(fileName).normalize();
        if (!target.startsWith(avatarDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException error) {
            log.warn("无法清理头像文件 {}", target, error);
        }
    }

    private Path getAvatarDirectory() {
        return Paths.get(projectConfig.getProfile())
                .toAbsolutePath()
                .normalize()
                .resolve("avatar")
                .normalize();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像图片不能超过2MB");
        }
        if (file.getContentType() == null
                || !file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/png")) {
            throw new IllegalArgumentException("头像必须是裁剪后的PNG图片");
        }

        try (
                InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)
        ) {
            if (imageInput == null) {
                throw new IllegalArgumentException("头像图片格式无效");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("无法读取头像图片");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                if (!"png".equalsIgnoreCase(reader.getFormatName())) {
                    throw new IllegalArgumentException("头像图片内容必须为PNG格式");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION) {
                    throw new IllegalArgumentException("头像图片尺寸无效");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("头像图片内容无效");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("无法读取头像图片", error);
        }
    }
}
