package com.nanoxai.marketplace.tests.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceReportOutput {
    private String type;
    private Integer version;
    private List<ReportDataObject> data;
}