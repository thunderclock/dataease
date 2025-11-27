package io.dataease.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.dataease.auth.entity.AccessKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * AccessKey Mapper
 */
@Mapper
public interface AccessKeyMapper extends BaseMapper<AccessKey> {
}

