package com.example.springboot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.system.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

}
