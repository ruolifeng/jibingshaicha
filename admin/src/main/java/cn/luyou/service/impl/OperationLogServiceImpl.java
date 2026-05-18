package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.OperationLogMapper;
import cn.luyou.model.OperationLog;
import cn.luyou.service.OperationLogService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Service 实现（V13）
 *
 * 异步落库通过 {@link Async}，默认走 Spring 自带 SimpleAsyncTaskExecutor。
 * 如需精细控制线程池，可在 config 包下新增 {@code @EnableAsync} 配置类自定义线程池。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    /** 操作类型 → 中文映射（仅用于导出列） */
    private static final Map<String, String> OP_TYPE_LABEL = Map.of(
            "login", "登录",
            "import", "导入",
            "delete", "删除",
            "update", "修改",
            "export", "导出",
            "create", "新增",
            "logout", "登出"
    );

    @Async
    @Override
    public void saveAsync(OperationLog log) {
        try {
            save(log);
        } catch (Exception e) {
            OperationLogServiceImpl.log.warn("save operation log failed: {}", e.getMessage());
        }
    }

    @Override
    public void saveSync(OperationLog log) {
        try {
            save(log);
        } catch (Exception e) {
            OperationLogServiceImpl.log.warn("save operation log (sync) failed: {}", e.getMessage());
        }
    }

    @Override
    public IPage<OperationLog> queryPage(int page, int size,
                                         String opType, String opModule,
                                         String userName, String keyword,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        w.eq(StrUtil.isNotBlank(opType), OperationLog::getOpType, opType)
         .eq(StrUtil.isNotBlank(opModule), OperationLog::getOpModule, opModule)
         .like(StrUtil.isNotBlank(userName), OperationLog::getUserName, userName)
         .and(StrUtil.isNotBlank(keyword), q -> q
                 .like(OperationLog::getOpAction, keyword).or()
                 .like(OperationLog::getRequestUrl, keyword))
         .ge(startTime != null, OperationLog::getCreateTime, startTime)
         .le(endTime != null, OperationLog::getCreateTime, endTime)
         .orderByDesc(OperationLog::getCreateTime);
        return page(new Page<>(page, size), w);
    }

    @Override
    public void exportLogs(String opType, String opModule,
                           String userName, String keyword,
                           LocalDateTime startTime, LocalDateTime endTime,
                           OutputStream outputStream) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        w.eq(StrUtil.isNotBlank(opType), OperationLog::getOpType, opType)
         .eq(StrUtil.isNotBlank(opModule), OperationLog::getOpModule, opModule)
         .like(StrUtil.isNotBlank(userName), OperationLog::getUserName, userName)
         .and(StrUtil.isNotBlank(keyword), q -> q
                 .like(OperationLog::getOpAction, keyword).or()
                 .like(OperationLog::getRequestUrl, keyword))
         .ge(startTime != null, OperationLog::getCreateTime, startTime)
         .le(endTime != null, OperationLog::getCreateTime, endTime)
         .orderByDesc(OperationLog::getCreateTime);
        List<OperationLog> records = list(w);

        // 转为简单 List<List<Object>> 写出，避免给实体加 Excel 注解
        List<List<String>> head = Arrays.asList(
                Arrays.asList("时间"), Arrays.asList("操作人"), Arrays.asList("部门ID"),
                Arrays.asList("角色"), Arrays.asList("类型"), Arrays.asList("模块"),
                Arrays.asList("动作描述"), Arrays.asList("HTTP方法"), Arrays.asList("URL"),
                Arrays.asList("IP"), Arrays.asList("结果"), Arrays.asList("错误信息"),
                Arrays.asList("耗时(ms)")
        );
        List<List<Object>> rows = new ArrayList<>(records.size());
        for (OperationLog r : records) {
            List<Object> row = new ArrayList<>(head.size());
            row.add(r.getCreateTime());
            row.add(StrUtil.isNotBlank(r.getRealName()) ? r.getRealName() + "(" + r.getUserName() + ")" : r.getUserName());
            row.add(r.getDepartmentId());
            row.add(r.getRole());
            row.add(OP_TYPE_LABEL.getOrDefault(r.getOpType(), r.getOpType()));
            row.add(r.getOpModule());
            row.add(r.getOpAction());
            row.add(r.getRequestMethod());
            row.add(r.getRequestUrl());
            row.add(r.getIp());
            row.add(r.getResultStatus() != null && r.getResultStatus() == 1 ? "成功" : "失败");
            row.add(r.getErrorMessage());
            row.add(r.getCostMs());
            rows.add(row);
        }

        EasyExcel.write(outputStream)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(head)
                .sheet("操作日志")
                .doWrite(rows);
    }
}
