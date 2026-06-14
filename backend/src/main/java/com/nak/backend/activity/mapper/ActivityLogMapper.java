package com.nak.backend.activity.mapper;

import com.nak.backend.activity.dto.ActivityLogDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ActivityLogMapper {
    List<ActivityLogDto> findByItemId(String itemId);
    void insertActivityLog(ActivityLogDto activityLog);
}
