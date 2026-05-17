package cn.luyou.service;

import cn.luyou.model.DataCleaningResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface DataCleaningService {
    DataCleaningResult clean(String populationType, MultipartFile file);

    Resource getResultFile(String fileId, Long currentUserId, boolean isSuperAdmin);
}
