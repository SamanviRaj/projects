package com.eqh.application.dto;

import lombok.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Component
public class ytdResponse {
    private BigDecimal YtdDisbursePeriodicPayout;
    private  BigDecimal YtdDisburseFederalWithholdingAmt;
    private  BigDecimal YtdDisburseStateWithholdingAmt;
    private  BigDecimal YtdDisburseInterest;
}
