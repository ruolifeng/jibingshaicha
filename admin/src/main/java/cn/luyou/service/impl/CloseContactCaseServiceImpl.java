package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.CloseContactCaseMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ImportResult;
import cn.luyou.model.User;
import cn.luyou.service.CloseContactCaseService;
import cn.luyou.service.DepartmentService;
import cn.luyou.utils.BaseContext;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloseContactCaseServiceImpl extends ServiceImpl<CloseContactCaseMapper, CloseContactCase>
        implements CloseContactCaseService {

    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final DepartmentService departmentService;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        String creatorUsername = resolveCurrentUsername();
        List<CloseContactCase> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        AtomicInteger rowNum = new AtomicInteger(3);

        try {
            EasyExcel.read(file.getInputStream(), CloseContactCase.class, new ReadListener<CloseContactCase>() {
                @Override
                public void invoke(CloseContactCase data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "接触者身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "接触者手机号格式不正确");
                    }
                    if (data.getRegistrationDate() != null) {
                        data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
                    }
                    data.setUploadBatch(batchId);
                    data.setDepartmentId(BaseContext.getCurrentDepartmentId());
                    data.setCreatorUsername(creatorUsername);
                    dataList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("密接个案表数据解析完成，共 {} 条", dataList.size());
                }
            }).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        List<CloseContactCase> toInsert = new ArrayList<>();
        List<CloseContactCase> toUpdate = new ArrayList<>();

        for (CloseContactCase d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            CloseContactCase existing = lambdaQuery()
                    .eq(CloseContactCase::getIdNumber, d.getIdNumber())
                    .last("LIMIT 1")
                    .one();
            if (existing != null) {
                mergeCaseData(existing, d);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setSuccessCount(dataList.size());
        return result;
    }

    private void mergeCaseData(CloseContactCase existing, CloseContactCase incoming) {
        if (StrUtil.isNotBlank(incoming.getSourcePatientName())) existing.setSourcePatientName(incoming.getSourcePatientName());
        if (StrUtil.isNotBlank(incoming.getName())) existing.setName(incoming.getName());
        if (StrUtil.isNotBlank(incoming.getPhone())) existing.setPhone(incoming.getPhone());
        if (StrUtil.isNotBlank(incoming.getDistrict())) existing.setDistrict(incoming.getDistrict());
        if (StrUtil.isNotBlank(incoming.getCity())) existing.setCity(incoming.getCity());
        if (StrUtil.isNotBlank(incoming.getFinalScreeningResult())) existing.setFinalScreeningResult(incoming.getFinalScreeningResult());
        if (StrUtil.isNotBlank(incoming.getInfectionCheckResult())) existing.setInfectionCheckResult(incoming.getInfectionCheckResult());
        if (StrUtil.isNotBlank(incoming.getImagingResult())) existing.setImagingResult(incoming.getImagingResult());
        if (StrUtil.isNotBlank(incoming.getSputumCheckResult())) existing.setSputumCheckResult(incoming.getSputumCheckResult());
        if (StrUtil.isNotBlank(incoming.getHasPreventiveTreatment())) existing.setHasPreventiveTreatment(incoming.getHasPreventiveTreatment());
        if (StrUtil.isNotBlank(incoming.getPreventivePlan())) existing.setPreventivePlan(incoming.getPreventivePlan());
        if (StrUtil.isNotBlank(incoming.getTreatmentCompleted())) existing.setTreatmentCompleted(incoming.getTreatmentCompleted());
        if (StrUtil.isNotBlank(incoming.getFollowup6Result())) {
            existing.setFollowup6DueDate(incoming.getFollowup6DueDate());
            existing.setFollowup6ScreenDate(incoming.getFollowup6ScreenDate());
            existing.setFollowup6Result(incoming.getFollowup6Result());
        }
        if (StrUtil.isNotBlank(incoming.getFollowup12Result())) {
            existing.setFollowup12DueDate(incoming.getFollowup12DueDate());
            existing.setFollowup12ScreenDate(incoming.getFollowup12ScreenDate());
            existing.setFollowup12Result(incoming.getFollowup12Result());
        }
        if (StrUtil.isNotBlank(incoming.getFollowup24Result())) {
            existing.setFollowup24DueDate(incoming.getFollowup24DueDate());
            existing.setFollowup24ScreenDate(incoming.getFollowup24ScreenDate());
            existing.setFollowup24Result(incoming.getFollowup24Result());
        }
        if (StrUtil.isNotBlank(incoming.getRemark())) existing.setRemark(incoming.getRemark());
        if (incoming.getRegistrationDate() != null) {
            existing.setRegistrationDate(incoming.getRegistrationDate());
            existing.setYear(String.valueOf(incoming.getRegistrationDate().getYear()));
        }
        existing.setUploadBatch(incoming.getUploadBatch());
        existing.setCreatorUsername(incoming.getCreatorUsername());
    }

    @Override
    public IPage<CloseContactCase> queryPage(int page, int size, String name, String idNumber,
                                              String district, String phone, String creatorUsername,
                                              String diagnosisResult) {
        LambdaQueryWrapper<CloseContactCase> wrapper = buildQueryWrapper(
                name, idNumber, district, phone, creatorUsername, diagnosisResult);
        wrapper.orderByDesc(CloseContactCase::getCreateTime);
        applyDepartmentFilter(wrapper);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCase(CloseContactCase data) {
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        data.setDepartmentId(BaseContext.getCurrentDepartmentId());
        data.setCreatorUsername(resolveCurrentUsername());
        save(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(CloseContactCase data) {
        CloseContactCase existing = requireAccessibleCase(data.getId());
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        // 录入用户与部门不可被前端覆盖
        data.setCreatorUsername(existing.getCreatorUsername());
        data.setDepartmentId(existing.getDepartmentId());
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        requireAccessibleCase(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            requireAccessibleCase(id);
        }
        removeByIds(ids);
    }

    @Override
    public List<CloseContactCase> listForExport(String name, String idNumber, String district,
                                                 String phone, String creatorUsername, String diagnosisResult,
                                                 List<Long> ids) {
        LambdaQueryWrapper<CloseContactCase> wrapper;
        if (ids != null && !ids.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CloseContactCase::getId, ids)
                    .eq(StrUtil.isNotBlank(diagnosisResult), CloseContactCase::getFinalScreeningResult, diagnosisResult);
        } else {
            wrapper = buildQueryWrapper(name, idNumber, district, phone, creatorUsername, diagnosisResult);
        }
        wrapper.orderByDesc(CloseContactCase::getCreateTime);
        applyDepartmentFilter(wrapper);
        return list(wrapper);
    }

    private LambdaQueryWrapper<CloseContactCase> buildQueryWrapper(String name, String idNumber,
                                                                    String district, String phone,
                                                                    String creatorUsername, String diagnosisResult) {
        LambdaQueryWrapper<CloseContactCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), CloseContactCase::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), CloseContactCase::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), CloseContactCase::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), CloseContactCase::getPhone, phone)
                .like(StrUtil.isNotBlank(creatorUsername), CloseContactCase::getCreatorUsername, creatorUsername)
                .eq(StrUtil.isNotBlank(diagnosisResult), CloseContactCase::getFinalScreeningResult, diagnosisResult);
        return wrapper;
    }

    private void applyDepartmentFilter(LambdaQueryWrapper<CloseContactCase> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        List<Long> deptIds = departmentService.getDescendantIds(currentDeptId);
        if (deptIds.isEmpty()) {
            // 未绑定部门时，仅可见本人录入的数据，避免 IN () 导致 SQL 异常
            String username = resolveCurrentUsername();
            if (StrUtil.isBlank(username)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.eq(CloseContactCase::getCreatorUsername, username);
            }
            return;
        }
        wrapper.in(CloseContactCase::getDepartmentId, deptIds);
    }

    @Override
    public CloseContactCase getAccessibleById(Long id) {
        return requireAccessibleCase(id);
    }

    private CloseContactCase requireAccessibleCase(Long id) {
        CloseContactCase existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "个案记录不存在");
        }
        if (!BaseContext.isSuperAdmin()) {
            Long currentDeptId = BaseContext.getCurrentDepartmentId();
            List<Long> deptIds = departmentService.getDescendantIds(currentDeptId);
            if (deptIds.isEmpty()) {
                String username = resolveCurrentUsername();
                if (StrUtil.isBlank(username)
                        || !username.equals(existing.getCreatorUsername())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "无权操作该个案记录");
                }
                return existing;
            }
            if (existing.getDepartmentId() == null || !deptIds.contains(existing.getDepartmentId())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "无权操作该个案记录");
            }
        }
        return existing;
    }

    private String resolveCurrentUsername() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }

    private boolean isValidIdCard(String id) {
        return ID_CARD_PATTERN.matcher(id.trim()).matches();
    }

    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
}
