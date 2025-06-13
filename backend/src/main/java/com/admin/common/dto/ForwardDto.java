package com.admin.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ForwardDto {

    @NotBlank(message = "转发名称不能为空")
    private String name;
    
    @NotNull(message = "隧道ID不能为空")
    private Integer tunnelId;
    
    @NotBlank(message = "远程地址不能为空")
    private String remoteAddr;
} 