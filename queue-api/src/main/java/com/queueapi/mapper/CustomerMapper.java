package com.queueapi.mapper;
import com.queueapi.dto.response.CustomerResponse;
import com.queueapi.entity.CustomerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerResponse toResponse(CustomerEntity entity);
}