package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.DataCleaningResult;
import cn.luyou.service.DataCleaningService;
import cn.luyou.utils.BaseContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "数据清洗")
@RestController
@RequestMapping("/data-cleaning")
@RequiredArgsConstructor
public class DataCleaningController {
    private final DataCleaningService dataCleaningService;

    @Operation(summary = "上传筛查数据进行清洗校验")
    @PostMapping("/clean")
    public ResultResponse<DataCleaningResult> clean(
            @RequestParam String populationType,
            @RequestParam("file") MultipartFile file) {
        return ResultRes.success(dataCleaningService.clean(populationType, file));
    }

    @Operation(summary = "下载清洗结果文件")
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        Resource resource = dataCleaningService.getResultFile(fileId, BaseContext.getCurrentId(), BaseContext.isSuperAdmin());
        String fileName = resource.getFilename() == null ? "数据清洗结果.xlsx" : resource.getFilename();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        try (InputStream inputStream = resource.getInputStream()) {
            inputStream.transferTo(response.getOutputStream());
        }
    }
}
