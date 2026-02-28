package com.example.exchange.mapper;

import com.example.exchange.dto.response.BalanceResponse;
import com.example.exchange.model.UserBalance;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BalanceMapper {

    BalanceResponse toResponse(UserBalance userBalance);

    List<BalanceResponse> toResponseList(List<UserBalance> userBalances);
}
