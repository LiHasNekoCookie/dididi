package com.example.qbot.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ActivityStatus {

    PROCESSING(1, "进行中"),
    FINISH(2, "完成"),
    CANCEL(3, "取消");

    @EnumValue
    private final int key;
    private final String value;

    ActivityStatus(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
