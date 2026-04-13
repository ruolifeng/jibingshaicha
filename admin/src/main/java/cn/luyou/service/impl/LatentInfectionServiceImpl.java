package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LatentInfectionServiceImpl extends ServiceImpl<LatentInfectionMapper, LatentInfection>
        implements LatentInfectionService {

    private final PatientService patientService;

    @Override
    public IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                             String name, String idNumber, Integer trackingStatus, Integer archived) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), LatentInfection::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                .eq(archived != null, LatentInfection::getArchived, archived)
                .orderByDesc(LatentInfection::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }

        switch (status) {
            case 1 -> {
                // 到位
                entity.setTrackingStatus(1);
            }
            case 2 -> {
                // 未到位
                int count = entity.getNotInPlaceCount() + 1;
                entity.setNotInPlaceCount(count);
                if (count >= 3) {
                    entity.setTrackingStatus(4); // 强制结束
                    entity.setTrackingRemark(remark);
                    entity.setArchived(1);
                } else {
                    entity.setTrackingStatus(2);
                }
            }
            case 3 -> {
                // 其他
                entity.setTrackingStatus(3);
                entity.setTrackingRemark(remark);
                entity.setArchived(1);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的追踪状态");
        }

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void referral(Long id, String result, String remark) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        // 必须追踪到位（trackingStatus=1）才允许转诊
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先完成追踪到位操作后再进行转诊");
        }
        // 已有转诊结果则不允许重复操作
        if (StrUtil.isNotBlank(entity.getReferralResult())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已完成转诊，不可重复操作");
        }

        entity.setReferralResult(result);
        entity.setReferralRemark(remark);

        switch (result) {
            case "excluded" -> {
                entity.setDiagnosisResult("排除");
                entity.setArchived(1);
            }
            case "other" -> {
                entity.setDiagnosisResult("其他");
                entity.setArchived(1);
            }
            case "confirmed" -> {
                entity.setDiagnosisResult("确诊");
                // 自动创建患者记录
                Patient patient = Patient.builder()
                        .screeningId(entity.getScreeningId())
                        .latentInfectionId(entity.getId())
                        .populationType(entity.getPopulationType())
                        .name(entity.getName())
                        .gender(entity.getGender())
                        .age(entity.getAge())
                        .idNumber(entity.getIdNumber())
                        .phone(entity.getPhone())
                        .diagnosisResult("确诊")
                        .source("confirmed")
                        .archived(0)
                        .build();
                patientService.save(patient);
            }
            case "latent" -> {
                entity.setDiagnosisResult("潜伏感染者");
                // 进入发送通知单流程
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的转诊结果");
        }

        updateById(entity);
    }
}
