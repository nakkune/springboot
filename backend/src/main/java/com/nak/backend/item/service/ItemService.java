package com.nak.backend.item.service;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nak.backend.item.dto.ItemDto;
import com.nak.backend.item.dto.ItemValueDto;
import com.nak.backend.item.mapper.ItemMapper;
import com.nak.backend.item.mapper.ItemValueMapper;
import com.nak.backend.user.dto.UserDto;
import com.nak.backend.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

/**
 * 아이템 비즈니스 로직 처리 서비스
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    // [NEW] 10년 차 시니어 설계: EAV 밸류 맵 조인을 위해 ItemValueMapper 주입
    private final ItemValueMapper itemValueMapper;
    // [NEW] created_by DB Not-Null 제약조건 충족 및 안전 방어(Defensive) 코딩을 위한 UserMapper 주입
    private final UserMapper userMapper;

    public List<ItemDto> getItemsByBoardId(String boardId) {
        List<ItemDto> items = itemMapper.findAllByBoardId(boardId);
        if (items.isEmpty()) {
            return items;
        }

        // 보드 내의 모든 EAV 값들을 O(1) 단 1회의 조인 쿼리로 고속 벌크 로드 (성능 극대화)
        List<ItemValueDto> allValues = itemValueMapper.findAllByBoardId(boardId);
        
        // itemId별로 그룹화하여 매핑
        Map<String, List<ItemValueDto>> valuesByItemId = allValues.stream()
                .collect(Collectors.groupingBy(ItemValueDto::getItemId));

        // 각 아이템에 밸류 맵을 바인딩 (columnId -> valueText)
        for (ItemDto item : items) {
            List<ItemValueDto> itemVals = valuesByItemId.get(item.getId());
            if (itemVals != null) {
                Map<String, String> valMap = new HashMap<>();
                for (ItemValueDto iv : itemVals) {
                    valMap.put(iv.getColumnId(), iv.getValueText());
                }
                item.setValues(valMap);
            }
        }
        return items;
    }

    public List<ItemDto> getItemsByGroupId(String groupId) {
        List<ItemDto> items = itemMapper.findAllByGroupId(groupId);
        if (items.isEmpty()) {
            return items;
        }

        // 각 아이템별로 EAV 값들을 세팅 (그룹별 조회 시 안전 바인딩)
        for (ItemDto item : items) {
            List<ItemValueDto> itemVals = itemValueMapper.findAllByItemId(item.getId());
            Map<String, String> valMap = new HashMap<>();
            for (ItemValueDto iv : itemVals) {
                valMap.put(iv.getColumnId(), iv.getValueText());
            }
            item.setValues(valMap);
        }
        return items;
    }

    public ItemDto getItemById(String id) {
        ItemDto item = itemMapper.findById(id);
        if (item == null) {
            throw new IllegalArgumentException("아이템을 찾을 수 없습니다. ID: " + id);
        }

        // 단건 상세 조회 시에도 EAV 밸류 맵을 충실하게 로드 및 바인딩
        List<ItemValueDto> itemVals = itemValueMapper.findAllByItemId(id);
        Map<String, String> valMap = new HashMap<>();
        for (ItemValueDto iv : itemVals) {
            valMap.put(iv.getColumnId(), iv.getValueText());
        }
        item.setValues(valMap);

        return item;
    }

    @Transactional
    public ItemDto createItem(ItemDto item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        if (item.getPosition() == null) item.setPosition(0);
        if (item.getIsArchived() == null) item.setIsArchived(false);

        // [NEW] 10년 차 시니어 방어 코딩: createdBy가 누락되었을 경우 기본 admin 사용자의 ID로 폴백 처리하여 DB Not-Null 에러 방지
        if (item.getCreatedBy() == null || item.getCreatedBy().isEmpty()) {
            UserDto admin = userMapper.findByEmail("admin@example.com");
            if (admin != null) {
                item.setCreatedBy(admin.getId());
            } else {
                List<UserDto> allUsers = userMapper.findAll();
                if (!allUsers.isEmpty()) {
                    item.setCreatedBy(allUsers.get(0).getId());
                }
            }
        }

        itemMapper.insert(item);
        
        // 아이템 생성 후 필요하다면 초깃값(values)도 삽입 가능
        if (item.getValues() != null && !item.getValues().isEmpty()) {
            for (Map.Entry<String, String> entry : item.getValues().entrySet()) {
                ItemValueDto iv = ItemValueDto.builder()
                        .itemId(item.getId())
                        .columnId(entry.getKey())
                        .valueText(entry.getValue())
                        .build();
                itemValueMapper.insert(iv);
            }
        }
        
        return getItemById(item.getId());
    }

    @Transactional
    public ItemDto updateItem(String id, ItemDto item) {
        ItemDto existing = getItemById(id);
        String oldGroupId = existing.getGroupId();
        Integer oldPosition = existing.getPosition();
        
        boolean groupChanged = item.getGroupId() != null && !item.getGroupId().equals(oldGroupId);
        boolean positionChanged = item.getPosition() != null && !item.getPosition().equals(oldPosition);
        
        if (item.getName() != null) existing.setName(item.getName());
        if (item.getParentItemId() != null) existing.setParentItemId(item.getParentItemId());
        if (item.getIsArchived() != null) existing.setIsArchived(item.getIsArchived());
        
        // [NEW] 10년 차 시니어 드래그앤드롭 포지셔닝 무결성 보장(Position Reordering) 알고리즘
        if (groupChanged || positionChanged) {
            String newGroupId = groupChanged ? item.getGroupId() : oldGroupId;
            Integer newPosition = positionChanged ? item.getPosition() : oldPosition;
            
            // 1. 기존 그룹에서 해당 아이템이 빠져나가면서 뒤에 있던 아이템들의 position을 1씩 앞으로 당겨서 빈자리 메꿈
            if (groupChanged) {
                List<ItemDto> oldGroupItems = itemMapper.findAllByGroupId(oldGroupId);
                for (ItemDto og : oldGroupItems) {
                    if (og.getPosition() > oldPosition) {
                        og.setPosition(og.getPosition() - 1);
                        itemMapper.update(og);
                    }
                }
            }
            
            // 2. 신규 그룹의 삽입할 위치(newPosition) 뒤에 있는 아이템들의 position을 1씩 뒤로 밀어 자리를 확보
            List<ItemDto> newGroupItems = itemMapper.findAllByGroupId(newGroupId);
            for (ItemDto ng : newGroupItems) {
                if (!ng.getId().equals(id) && ng.getPosition() >= newPosition) {
                    ng.setPosition(ng.getPosition() + 1);
                    itemMapper.update(ng);
                }
            }
            
            existing.setGroupId(newGroupId);
            existing.setPosition(newPosition);
        }
        
        itemMapper.update(existing);

        // 업데이트 요청 시 전달된 EAV 밸류 맵이 있다면 갱신 또는 저장
        if (item.getValues() != null && !item.getValues().isEmpty()) {
            for (Map.Entry<String, String> entry : item.getValues().entrySet()) {
                ItemValueDto iv = ItemValueDto.builder()
                        .itemId(id)
                        .columnId(entry.getKey())
                        .valueText(entry.getValue())
                        .build();
                
                // 중복 갱신 충돌 방지를 위해 ItemValueService 로직의 우아한 분기 처리 차용
                ItemValueDto existingValue = itemValueMapper.findByItemAndColumn(id, entry.getKey());
                if (existingValue == null) {
                    iv.setId(UUID.randomUUID().toString());
                    itemValueMapper.insert(iv);
                } else {
                    existingValue.setValueText(entry.getValue());
                    itemValueMapper.update(existingValue);
                }
            }
        }
        
        return getItemById(id);
    }

    @Transactional
    public void deleteItem(String id) {
        getItemById(id); // 존재 여부 검증
        
        // 1. 하위 아이템(자식)들을 재귀적으로 찾아서 먼저 모두 삭제 (Cascading Delete)
        List<ItemDto> children = itemMapper.findAllByParentItemId(id);
        for (ItemDto child : children) {
            deleteItem(child.getId());
        }
        
        // 2. 하위 EAV 밸류 테이블 정리
        List<ItemValueDto> itemVals = itemValueMapper.findAllByItemId(id);
        for (ItemValueDto iv : itemVals) {
            itemValueMapper.deleteById(iv.getId());
        }
        
        // 3. 본인 삭제
        itemMapper.deleteById(id);
    }
}
