package com.admin.service;

import com.admin.common.dto.ForwardDto;
import com.admin.common.dto.ForwardUpdateDto;
import com.admin.common.lang.R;
import com.admin.entity.Forward;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author QAQ
 * @since 2025-06-03
 */
public interface ForwardService extends IService<Forward> {

    /**
     * 创建端口转发
     * @param forwardDto 转发数据
     * @return 结果
     */
    R createForward(ForwardDto forwardDto);

    /**
     * 获取端口转发列表
     * @return 结果
     */
    R getAllForwards();

    /**
     * 更新端口转发
     * @param forwardUpdateDto 更新数据
     * @return 结果
     */
    R updateForward(ForwardUpdateDto forwardUpdateDto);

    /**
     * 删除端口转发
     * @param id 转发ID
     * @return 结果
     */
    R deleteForward(Long id);

    /**
     * 暂停转发服务
     * @param id 转发ID
     * @return 结果
     */
    R pauseForward(Long id);

    /**
     * 恢复转发服务
     * @param id 转发ID
     * @return 结果
     */
    R resumeForward(Long id);
}
