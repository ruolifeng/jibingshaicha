package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.MedicationPickupMapper;
import cn.luyou.model.MedicationPickup;
import cn.luyou.model.Patient;
import cn.luyou.service.MedicationPickupService;
import cn.luyou.service.PatientService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationPickupServiceImpl extends ServiceImpl<MedicationPickupMapper, MedicationPickup>
        implements MedicationPickupService {

    private static final int PICKUP_EDIT_DAYS_LEVEL5 = 10;

    private final PatientService patientService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePickup(MedicationPickup pickup) {
        if (pickup.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        validatePickupFields(pickup);

        Patient patient = patientService.getById(pickup.getPatientId());
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者记录不存在");
        }
        if (Integer.valueOf(1).equals(patient.getArchived()) && pickup.getId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者已归档，无法新增领药记录");
        }

        if (pickup.getId() != null) {
            MedicationPickup existing = getById(pickup.getId());
            if (existing == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "领药记录不存在");
            }
            if (!existing.getPatientId().equals(pickup.getPatientId())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "患者与领药记录不匹配");
            }
            assertPickupEditable(existing);
            pickup.setPickupSeq(existing.getPickupSeq());
            pickup.setPopulationType(existing.getPopulationType());
            pickup.setPatientId(existing.getPatientId());
            if (pickup.getFilledBy() == null) {
                pickup.setFilledBy(existing.getFilledBy());
            }
            updateById(pickup);
            return;
        }

        pickup.setPopulationType(patient.getPopulationType());
        pickup.setPickupSeq(nextPickupSeq(pickup.getPatientId()));
        pickup.setFilledBy(BaseContext.getCurrentId());
        save(pickup);
    }

    @Override
    public List<MedicationPickup> listByPatientId(Long patientId) {
        LambdaQueryWrapper<MedicationPickup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationPickup::getPatientId, patientId)
                .orderByAsc(MedicationPickup::getCreateTime);
        List<MedicationPickup> list = list(wrapper);
        Integer role = BaseContext.getCurrentRole();
        list.forEach(item -> item.setEditable(isPickupEditable(role, item)));
        return list;
    }

    public boolean isPickupEditable(Integer role, MedicationPickup pickup) {
        if (pickup == null) {
            return true;
        }
        if (role == null || role != 6) {
            return true;
        }
        if (pickup.getCreateTime() == null) {
            return true;
        }
        return !pickup.getCreateTime().plusDays(PICKUP_EDIT_DAYS_LEVEL5).isBefore(LocalDateTime.now());
    }

    private void assertPickupEditable(MedicationPickup existing) {
        if (!isPickupEditable(BaseContext.getCurrentRole(), existing)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "领药记录已超过 " + PICKUP_EDIT_DAYS_LEVEL5 + " 天修改期限，请联系上级管理员");
        }
    }

    private int nextPickupSeq(Long patientId) {
        long count = lambdaQuery().eq(MedicationPickup::getPatientId, patientId).count();
        return (int) count + 1;
    }

    private void validatePickupFields(MedicationPickup pickup) {
        if (pickup.getPickupTime() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择领取时间");
        }
        if (pickup.getQuantity() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写领取数量");
        }
        if (pickup.getQuantity().signum() <= 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "领取数量必须大于 0");
        }
        if (StrUtil.isBlank(pickup.getQuantityUnit())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择领取数量单位");
        }
        if (StrUtil.isBlank(pickup.getDispensingUnit())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写发药单位");
        }
        validateDrugsJson(pickup.getDrugs());
    }

    private void validateDrugsJson(String drugsJson) {
        if (StrUtil.isBlank(drugsJson)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请至少添加一种药品");
        }
        JSONArray array;
        try {
            array = JSONUtil.parseArray(drugsJson);
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "药品数据格式有误");
        }
        if (array.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请至少添加一种药品");
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "药品数据格式有误");
            }
            if (StrUtil.isBlank(item.getStr("name"))) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写药品名称");
            }
            if (StrUtil.isBlank(item.getStr("dosage"))) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写药品用量");
            }
        }
    }
}
