package com.admin.common.dto;

import lombok.Data;

@Data
public class PageDto {
    
    /**
     * 当前页码，默认为1
     */
    private Long current = 1L;
    
    /**
     * 每页显示条数，默认为10
     */
    private Long size = 10L;
    
    /**
     * 搜索关键字（可选）
     */
    private String keyword;
} 