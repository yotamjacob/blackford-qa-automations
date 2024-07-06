Feature: CCS container automation tests

  Background:
    Given I configure mock server
    Given I configure wiremock with endpoint /result with status_code 200
    Given I configure wiremock with endpoint /result/error with status_code 200

  Scenario: QAE-2162 Study flow for CCS Non-Compliant result
    Given I upload the input directory: inputs/CcsNCresult/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.295952550.24230.21553.37296.227313667703977
    And I wait till /result called
    Then I Validate NON-compliant /result for study id :214.295952550.24230.21553.37296.227313667703977 and error code:XER_ST_0002

  Scenario: QAE-2155 Batch Processing for 2 positive studies from same algorithm
    Given I upload the input directory: inputs/batch_2_studies/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze/all for all studies in input folder
    And I wait till /result called
    And I verify wiremock was called on path /result with payload batch_2_studies.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:batch_2_studies.json

  Scenario: QAE-2167 AI Container flow on batch of studies, which is mix of: compliant + non-compliant
    Given I upload the input directory: inputs/mixed_compliant_non_compliant/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze studies
      | 214.295952550.24230.21553.37296.227313667703977  |
      | 214.1490261363.64900.21705.42107.90776995818651  |
    And I wait till /result called
    And I verify wiremock was called on path /result with payload resultMulti.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:resultMulti.json

  Scenario: QAE-2163 AI Container failed over entire input studies
    Given I upload the input directory: inputs/all_errors/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze studies
      | 1.3.51.0.7.1200180407.50022.6473.35580.283.32007.61064  |
      | 214.356375561.30934.21638.36839.192964739987315  |
    And I wait till /result called
    And I verify wiremock was called on path /result with payload allErrors.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:allErrors.json

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

  Scenario: QAE-2176 AI Container failed on unexpected file structure under: /marketplace/input Scenario #2
    Given I upload the input directory: inputs/wrong_folder_structure to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze bad structure for study id :214.3334084477.14391.22970.40183.38730267874965
    And I wait till /result/error called
    And I verify wiremock was called on path /result/error with error payload resultWrongStructure#2.json

  Scenario: Verify CCS High SC Dicoms
    Given I upload the input directory: inputs/scCCsHighStudy/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.3334084477.14391.22970.40183.38730267874965
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ccs_high.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ccs_high.json
    Then Validate dicom file 214.3334084477.14391.22970.40183.38730267874965.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category High and algorithm HealthCCSng_1.0.12

  Scenario: Verify CCS Medium SC Dicoms
    Given I upload the input directory: inputs/scCCsMediumStudy/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.1537738950.24610.22417.38331.274769846145442
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ccs_medium.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ccs_medium.json
    Then Validate dicom file 214.1537738950.24610.22417.38331.274769846145442.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Medium and algorithm HealthCCSng_1.0.12

  Scenario: Verify CCS Low SC Dicoms
    Given I upload the input directory: inputs/scCCsLowStudy/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.725652907.30706.22764.48663.16532264514316
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ccs_low.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ccs_low.json
    Then Validate dicom file 214.725652907.30706.22764.48663.16532264514316.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category Low and algorithm HealthCCSng_1.0.12

  Scenario: Verify CCS MetalArtifact SC Dicoms
    Given I upload the input directory: inputs/scCCsMetalArtifactStudy/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.3612073671.27179.21307.45122.224373892839243
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ccs_metal_artifact.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ccs_metal_artifact.json
    Then Validate dicom file 214.3612073671.27179.21307.45122.224373892839243.dcm
    And I download the SC dicom files from remote output path
    And I convert all the SC dicoms to JPEG
    Then I compare the SC dicoms to the expected SC dicoms for category MetalArtifact and algorithm HealthCCSng_1.0.12

  Scenario: Verify CCS Heavy Study
    Given I upload the input directory: inputs/CcsHeavyStudy/organization to the marketplace container workspace
    Given I call Marketplace on path /health to check healthiness
    Given I call Marketplace on path /v1/analyze to analyze study id :214.4174629899.49112.22340.48402.138157463567204
    And I wait till /result called
    And I verify wiremock was called on path /result with payload result_ccs_heavy_study.json
    And I download the report marketplace_data.json from the remote output path, check that second capture dicom file was created and compare it to:result_ccs_heavy_study.json
    Then Validate dicom file 214.4174629899.49112.22340.48402.138157463567204.dcm