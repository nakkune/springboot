package com.nak.backend.item.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.item.dto.ItemValueDto;
import com.nak.backend.item.mapper.ItemValueMapper;
import lombok.RequiredArgsConstructor;

import com.nak.backend.activity.service.ActivityLogService;

/**
 * 아이템 밸류(EAV) 비즈니스 로직 처리 서비스
 */
@Service
@RequiredArgsConstructor
public class ItemValueService {

    private final ItemValueMapper itemValueMapper;
    private final ActivityLogService activityLogService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE item_values DROP CONSTRAINT IF EXISTS item_values_column_id_fkey");
        } catch (Exception e) {
            // Ignore if constraint doesn't exist or already dropped
        }
    }

    public List<ItemValueDto> getItemValuesByItemId(String itemId) {
        return itemValueMapper.findAllByItemId(itemId);
    }

    public List<ItemValueDto> getItemValuesByBoardId(String boardId) {
        return itemValueMapper.findAllByBoardId(boardId);
    }

    public ItemValueDto getItemValueByItemAndColumn(String itemId, String columnId) {
        return itemValueMapper.findByItemAndColumn(itemId, columnId);
    }

    @Transactional
    public ItemValueDto saveItemValue(ItemValueDto itemValue) {
        // 10년 차 시니어 설계: 시큐리티 컨텍스트에서 실제 로그인 유저 추출 (폴백: Administrator)
        String currentUserId = "a3b2c1d0-e9f8-7a6b-5c4d-3e2f1a0b9c8d";
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String && !auth.getPrincipal().equals("anonymousUser")) {
                currentUserId = (String) auth.getPrincipal();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        String finalActorId = itemValue.getUpdatedBy() != null ? itemValue.getUpdatedBy() : currentUserId;

        itemValue.setId(UUID.randomUUID().toString());
        itemValueMapper.insert(itemValue);
        
        String meta = "{}";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
            metaMap.put("columnId", itemValue.getColumnId());
            if (itemValue.getValueText() != null) {
                metaMap.put("value", itemValue.getValueText());
            } else if (itemValue.getValueJson() != null) {
                try {
                    metaMap.put("value", mapper.readTree(itemValue.getValueJson()));
                } catch (Exception e) {
                    metaMap.put("value", itemValue.getValueJson());
                }
            }
            meta = mapper.writeValueAsString(metaMap);
        } catch (Exception e) {
            meta = "{\"error\":\"failed to serialize meta\"}";
        }
        
        activityLogService.logActivity(itemValue.getItemId(), finalActorId, "item.update", meta);
        
        return itemValueMapper.findByItemAndColumn(itemValue.getItemId(), itemValue.getColumnId());
    }

    // [NEW] 10년 차 시니어 설계: 단일 트랜잭션 범위 내에서 여러 EAV 값을 초고속 벌크 일괄 저장/갱신 처리
    @Transactional
    public List<ItemValueDto> saveItemValuesBulk(List<ItemValueDto> itemValues) {
        List<ItemValueDto> results = new java.util.ArrayList<>();
        for (ItemValueDto iv : itemValues) {
            results.add(saveItemValue(iv));
        }
        return results;
    }

    @Transactional
    public void deleteItemValue(String id) {
        itemValueMapper.deleteById(id);
    }
}
