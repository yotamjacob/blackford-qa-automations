package com.nanoxai.marketplace.tests.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceFileDetails {
    private String path;
    private Boolean pushToPACS;
}