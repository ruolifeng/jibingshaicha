package cn.luyou.service;

import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ImportResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloseContactCaseService extends IService<CloseContactCase> {

    ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid);

    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    IPage<CloseContactCase> queryPage(int page, int size, String name, String idNumber,
                                      String district, String phone, String creatorUsername,
                                      String diagnosisResult, String createTimeFrom, String createTimeTo);

    void createCase(CloseContactCase data);

    void updateCase(CloseContactCase data);

    void deleteCase(Long id);

    void batchDelete(List<Long> ids);

    List<CloseContactCase> listForExport(String name, String idNumber, String district,
                                         String phone, String creatorUsername, String diagnosisResult,
                                         List<Long> ids, String createTimeFrom, String createTimeTo);

    CloseContactCase getAccessibleById(Long id);
}
