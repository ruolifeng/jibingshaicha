package cn.luyou.utils;

import cn.luyou.model.LatentInfection;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 潜伏感染与筛查表关联校验：排除筛查已删除但 latent 未清理的孤儿记录。
 */
public final class LatentScreeningLinkSupport {

    private LatentScreeningLinkSupport() {
    }

    /**
     * screening_id 为空（手动新增）保留；有关联时必须存在未删除的筛查主记录。
     */
    public static void applyLinkedScreeningExistsFilter(LambdaQueryWrapper<LatentInfection> wrapper) {
        wrapper.and(w -> w.isNull(LatentInfection::getScreeningId)
                .or(wSchool -> wSchool.eq(LatentInfection::getPopulationType, "school")
                        .inSql(LatentInfection::getScreeningId,
                                "SELECT id FROM screening_school WHERE deleted = 0"))
                .or(wKey -> wKey.in(LatentInfection::getPopulationType, "keyPopulation", "regular")
                        .inSql(LatentInfection::getScreeningId,
                                "SELECT id FROM screening_key_population WHERE deleted = 0"))
                .or(wClose -> wClose.eq(LatentInfection::getPopulationType, "closeContact")
                        .inSql(LatentInfection::getScreeningId,
                                "SELECT id FROM screening_close_contact WHERE deleted = 0")));
    }
}
