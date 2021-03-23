package com.example.qbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qbot.eo.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ActivityMapper extends BaseMapper<Activity> {
}
