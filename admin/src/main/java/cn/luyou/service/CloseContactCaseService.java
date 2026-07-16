package cn.luyou.service;

import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ImportResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloseContactCaseService extends IService<CloseContactCase> {

    ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile);

    default ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid) {
        return uploadAndParse(file, confirmSkipInvalid, false);
    }

    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    IPage<CloseContactCase> queryPage(int page, int size, String name, String idNumber,
                                      String district, String phone, String creatorUsername,
                                      String diagnosisResult, String sourcePatientBacteriologyResult,
                                      String reportQuarter,
                                      String createTimeFrom, String createTimeTo,
                                      String columnFilters, String formatIssue);

    default IPage<CloseContactCase> queryPage(int page, int size, String name, String idNumber,
                                              String district, String phone, String creatorUsername,
                                              String diagnosisResult, String createTimeFrom, String createTimeTo) {
        return queryPage(page, size, name, idNumber, district, phone, creatorUsername,
                diagnosisResult, null, null, createTimeFrom, createTimeTo, null, null);
    }

    void createCase(CloseContactCase data);

    void updateCase(CloseContactCase data);

    void deleteCase(Long id);

    void batchDelete(List<Long> ids);

    /** 按筛选条件删除（与 list/export 同参，含 reportQuarter），返回删除条数 */
    int deleteByFilter(String name, String idNumber, String district, String phone,
                       String creatorUsername, String diagnosisResult,
                       String sourcePatientBacteriologyResult, String reportQuarter,
                       String createTimeFrom, String createTimeTo, String columnFilters, String formatIssue);

    /** 删除权限范围内全部密接个案，返回删除条数 */
    int deleteAll();

    List<CloseContactCase> listForExport(String name, String idNumber, String district,
                                         String phone, String creatorUsername, String diagnosisResult,
                                         String sourcePatientBacteriologyResult, String reportQuarter,
                                         List<Long> ids,
                                         String createTimeFrom, String createTimeTo,
                                         String columnFilters, String formatIssue);

    CloseContactCase getAccessibleById(Long id);
}
