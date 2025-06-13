package com.admin.common.task;


import lombok.Data;

@Data
public class TaskBase {
    private String data;
    private String type;

    public TaskBase(String data) {
        this.data = data;
    }
}
