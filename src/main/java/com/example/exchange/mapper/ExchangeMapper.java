package com.example.exchange.mapper;

import com.example.exchange.dto.response.ExchangeResponse;
import com.example.exchange.model.Exchange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExchangeMapper {

    @Mapping(target = "status", expression = "java(exchange.getStatus().name())")
    ExchangeResponse toResponse(Exchange exchange);

    List<ExchangeResponse> toResponseList(List<Exchange> exchanges);
}
