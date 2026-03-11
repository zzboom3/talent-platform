package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final OssService ossService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "files") String directory) {
        try {
            String url = ossService.uploadFile(file, directory);
            return Result.ok(Map.of("url", url));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam String url) {
        try {
            ossService.deleteFile(url);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
