package com.eqh.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ProductInfo {
    private  String managementCode;
    private  String policyStatus;
    private  String productCode;
    private  String qualPlanType;
}
