package cn.luyou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCleaningResult {
    /** 本次文件总数据行数 */
    private int totalCount;
    /** 异常数据行数 */
    private int abnormalCount;
    /** 异常条目总数（一行可能有多条校验问题） */
    private int errorItemCount;
    /** 下载清洗结果文件ID */
    private String fileId;
    /** 清洗结果文件名 */
    private String fileName;
    /** 异常摘要（最多返回前200条） */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
