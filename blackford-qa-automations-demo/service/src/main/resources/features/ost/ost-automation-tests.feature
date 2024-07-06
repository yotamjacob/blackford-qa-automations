Feature: OST container automation tests

  Background:
    Given I configure mock server
    Given I configure wiremock with endpoint /result with status_code 200
    Given I configure wiremock with endpoint /result/error with status_code 200

  Scenario: QAE-2858 Study flow for OST Mild result
    Given I upload the input directory: inputs/OstMild/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.503392905.51952.22243.41514.148536297145021
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ost_mild.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ost_mild.json
    Then Validate dicom file 214.503392905.51952.22243.41514.148536297145021.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Mild and algorithm HealthOST_1.0.2

  Scenario: QAE-2859 Study flow for OST Moderate result
    Given I upload the input directory: inputs/OstModerate/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.1203323646.28391.22594.43764.134780588049190
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ost_moderate.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ost_moderate.json
    Then Validate dicom file 214.1203323646.28391.22594.43764.134780588049190.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Moderate and algorithm HealthOST_1.0.2

  Scenario: QAE-2860 Study flow for OST Severe result
    Given I upload the input directory: inputs/OstSevere/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.3642112113.22071.22882.37738.229067079074208
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ost_severe.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ost_severe.json
    Then Validate dicom file 214.3642112113.22071.22882.37738.229067079074208.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Severe and algorithm HealthOST_1.0.2

  Scenario: QAE-2861 Study flow for OST Normal result
    Given I upload the input directory: inputs/OstNormal/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.68189906.1963.21895.49060.640243135712
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ost_normal.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ost_normal.json
    Then Validate dicom file 214.68189906.1963.21895.49060.640243135712.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Normal and algorithm HealthOST_1.0.2

  Scenario: QAE-2862 Study flow for OST Non-compliant result
    Given I upload the input directory: inputs/OstNonCompliant/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.3061003311.24717.22762.35837.59811549598743
    And I wait till /result called
    Then I Validate NON-compliant /result for study id :214.3061003311.24717.22762.35837.59811549598743 and error code:XER_ST_010

  Scenario: QAE-2857 Batch Processing for 3 positive + 1 Normal
    Given I upload the input directory: inputs/OstBatch4Studies/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze/all for all studies in input folder
    And I wait till /result called
    And I verify wiremock was called on path /result with payload batch_ost_4_studies.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:batch_ost_4_studies.json

  Scenario: QAE-2167 AI Container flow on batch of studies, which is mix of: compliant + non-compliant
    Given I upload the input directory: inputs/ost_mixed_compliant_non_compliant/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze studies
      | 214.1203323646.28391.22594.43764.134780588049190  |
      | 214.3061003311.24717.22762.35837.59811549598743  |
    And I wait till /result called
    And I verify wiremock was called on path /result with payload resultMultiOst.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:resultMultiOst.json

  Scenario: QAE-2163 AI Container failed over entire input studies
    Given I upload the input directory: inputs/OstNonCompliant/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze studies
      | 214.3061003311.24717.22762.35837.59811549598743 |
    And I wait till /result called
    And I verify wiremock was called on path /result with payload allErrorsOst.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:allErrorsOst.json

  Scenario: QAE-2175 AI Container failed on missing file under: /marketplace/input/
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze bad study id :2
    And I wait till /result/error called
    And I verify wiremock was called on path /result/error with error payload resultErrorMissingFile.json

  Scenario: QAE-2176 AI Container failed on unexpected file structure under: /marketplace/input Scenario #1
    Given I upload the input directory: inputs/wrong_folder_structure to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze bad structure for study id :214.1537738950.24610.22417.38331.274769846145442
    And I wait till /result/error called
    And I verify wiremock was called on path /result/error with error payload resultWrongStructure#1.json

  Scenario: QAE-2883 AI Container failed on unexpected file structure under: /marketplace/input Scenario #2
    Given I upload the input directory: inputs/wrong_folder_structure to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze bad structure for study id :214.3334084477.14391.22970.40183.38730267874965
    And I wait till /result/error called
    And I verify wiremock was called on path /result/error with error payload resultWrongStructure#2.json

  Scenario: QAE-2935 AI Container failed when "nanoxai" folder is missing under: /marketplace/input/organization/
    Given I upload the input directory: inputs/nanoxai_folder_missing to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study:214.3061003311.24717.22762.35837.59811549598743 and expect bad request

  Scenario: QAE-2936 AI Container failed when "organization" folder is missing under: /marketplace/input/
    Given I upload the input directory: inputs/organization_folder_missing to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study:214.68189906.1963.21895.49060.640243135712 and expect bad request