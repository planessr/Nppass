package com.admin.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class NodeDto {

    @NotBlank(message = "节点名称不能为空")
    private String name;

    @NotNull(message = "控制端口不能为空")
    @Min(value = 1, message = "端口号必须在1-65535之间")
    @Max(value = 65535, message = "端口号必须在1-65535之间")
    private Integer port;
} 