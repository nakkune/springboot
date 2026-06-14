package com.nak.backend.automation.mapper;

import com.nak.backend.automation.dto.AutomationDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AutomationMapper {
    List<AutomationDto> findByBoardId(String boardId);
    AutomationDto findById(String id);
    void insertAutomation(AutomationDto automation);
    void updateAutomation(AutomationDto automation);
    void updateRunStatus(String id);
    void deleteAutomation(String id);
}
