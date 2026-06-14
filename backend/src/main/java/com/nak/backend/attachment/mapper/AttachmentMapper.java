package com.nak.backend.attachment.mapper;

import com.nak.backend.attachment.dto.AttachmentDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    List<AttachmentDto> findByItemId(String itemId);
    void insertAttachment(AttachmentDto attachment);
}
