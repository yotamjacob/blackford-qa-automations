package com.nanoxai.marketplace.tests.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataObject {
    private String studyInstanceUid;
    private Map<String, Object> predictions;
    private List<MarketplaceFileDetails> files;
    private Map<String, Object> metrics;
    private MarketplaceErrorObject error;
}
