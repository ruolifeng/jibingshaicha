package cn.luyou.service;

import cn.luyou.model.OperationLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * 操作日志 Service（V13）
 */
public interface OperationLogService extends IService<OperationLog> {

    /** 异步保存日志（切面 / 拦截器调用） */
    void saveAsync(OperationLog log);

    /** 同步保存日志（用于登录失败等关键事件，要确保落库） */
    void saveSync(OperationLog log);

    /**
     * 分页查询
     *
     * @param page      当前页
     * @param size      每页条数
     * @param opType    操作类型（可空）
     * @param opModule  业务模块（可空）
     * @param userName  操作人用户名（模糊，可空）
     * @param keyword   关键词（动作/URL模糊，可空）
     * @param startTime 起始时间（可空）
     * @param endTime   结束时间（可空）
     */
    IPage<OperationLog> queryPage(int page, int size,
                                  String opType, String opModule,
                                  String userName, String keyword,
                                  LocalDateTime startTime, LocalDateTime endTime);

    /** 导出操作日志（按筛选条件全量导出，返回 Excel 字节流通过 controller 处理） */
    void exportLogs(String opType, String opModule,
                    String userName, String keyword,
                    LocalDateTime startTime, LocalDateTime endTime,
                    java.io.OutputStream outputStream);
}
