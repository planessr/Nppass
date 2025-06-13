package com.admin.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Data
public class TunnelDto {
    
    @NotBlank(message = "隧道名称不能为空")
    private String name;
    
    @NotNull(message = "入口节点不能为空")
    private Long inNodeId;
    
    @NotNull(message = "入口端口开始不能为空")
    @Min(value = 1, message = "入口端口开始必须大于0")
    @Max(value = 65535, message = "入口端口开始不能超过65535")
    private Integer inPortSta;
    
    @NotNull(message = "入口端口结束不能为空")
    @Min(value = 1, message = "入口端口结束必须大于0")
    @Max(value = 65535, message = "入口端口结束不能超过65535")
    private Integer inPortEnd;
    
    // 出口节点ID，当type=1时可以为空，会自动设置为入口节点ID
    private Long outNodeId;
    
    // 出口端口开始，当type=1时可以为空，会自动设置为入口端口
    private Integer outIpSta;
    
    // 出口端口结束，当type=1时可以为空，会自动设置为入口端口  
    private Integer outIpEnd;
    
    @NotNull(message = "隧道类型不能为空")
    private Integer type;
    
    @NotNull(message = "流量计算类型不能为空")
    private Integer flow;
} 