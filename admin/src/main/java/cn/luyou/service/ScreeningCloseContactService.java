package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningCloseContactService extends IService<ScreeningCloseContact> {

    ImportResult uploadAndParse(MultipartFile file);

    IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                            String district, Integer isLatent);
}
