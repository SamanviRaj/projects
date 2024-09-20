package com.eqh.application.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ytdResponse {
    private BigDecimal YtdDisbursePeriodicPayout;
    private  BigDecimal YtdDisburseFederalWithholdingAmt;
    private  BigDecimal YtdDisburseStateWithholdingAmt;
    private  BigDecimal YtdDisburseInterest;
}
