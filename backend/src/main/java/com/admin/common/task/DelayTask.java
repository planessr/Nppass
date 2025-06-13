package com.admin.common.task;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
/**
 * 延时任务
 */
public class DelayTask implements Delayed {
    //任务参数
    final private TaskBase data;
    //任务的延时时间，单位毫秒
    final private long expire;

    /**
     * 构造延时任务
     *
     * @param data   业务数据
     * @param expire 任务延时时间（ms）
     */
    public DelayTask(TaskBase data, long expire) {
        super();
        this.data = data;
        this.expire = expire + System.currentTimeMillis();
    }

    public TaskBase getData() {
        return data;
    }

    public long getExpire() {
        return expire;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DelayTask) {
            return this.data.getData().equals(((DelayTask) obj).getData().getData());
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + "data:" + data.toString() + "," + "延时时间:"+expire+"}";
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.expire - System.currentTimeMillis(), unit);
    }

    @Override
    public int compareTo(Delayed o) {
        long delta = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        return (int) delta;
    }
}
