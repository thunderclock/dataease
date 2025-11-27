package io.dataease.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.dataease.auth.entity.AccessToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * AccessToken Mapper
 */
@Mapper
public interface AccessTokenMapper extends BaseMapper<AccessToken> {
}

