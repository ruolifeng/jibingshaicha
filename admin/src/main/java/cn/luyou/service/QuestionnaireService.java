package cn.luyou.service;

import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.vo.QuestionnaireConfigVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface QuestionnaireService {

    /** 获取问卷配置（不存在则初始化默认配置） */
    QuestionnaireConfigVO getConfig(String code);

    /** 更新问卷配置 */
    void updateConfig(String code, QuestionnaireConfigVO vo);

    /** 切换问卷开关 */
    void updateEnabled(String code, boolean enabled);

    /** 公开接口：获取问卷（仅返回开启状态与字段） */
    QuestionnaireConfigVO getPublicConfig(String code);

    /** 公开接口：提交问卷 */
    void submit(String code, Map<String, Object> formData);

    /** 分页查询问卷提交记录 */
    IPage<ScreeningSchool> listSubmissions(String code, int page, int size, String name, String idNumber);

    /** 导出问卷提交记录 */
    List<ScreeningSchool> listSubmissionsForExport(String code, String name, String idNumber);

    /** 导出问卷提交记录（中文表头） */
    void exportSubmissions(String code, String name, String idNumber, OutputStream outputStream);
}
