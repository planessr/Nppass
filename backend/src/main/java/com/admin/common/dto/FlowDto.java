package com.admin.common.dto;


import lombok.Data;

@Data
public class FlowDto {
    // [{n=41_tcp, t=cc, u=73225, d=35043}, {n=41_tcp, t=conn, u=35043, d=73225}]
    // 转发id_类型
    private String n;

    // 是请求还是接收
    private String t;

    // 上传流量
    private Long u;

    // 下载流量
    private Long d;
}
