package cn.luyou.service;

import cn.luyou.model.EpidemicImport;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface EpidemicImportService extends IService<EpidemicImport> {

    Map<String, Object> importData(MultipartFile file);

    IPage<EpidemicImport> queryPage(
            int page,
            int size,
            String name,
            String idNumber,
            Integer trackingStatus,
            Integer archived
    );

    void track(Long id, Integer status, String remark);

    void saveXray(Long id, Map<String, Object> data);

    void saveDiagnosis(Long id, String diagnosisResult);
}

