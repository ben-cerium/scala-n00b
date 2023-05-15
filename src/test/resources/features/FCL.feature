@FCL
Feature: Finding Case Law Request Judgement

A simple smoke test to see if sending a valid message with a known valid file produces a valid response, idicating the system is working correctly.  Does not check the file generated.

  Scenario: Finding Case Law submit a valid Request Judgment Message and recieve a Valid Message back
    Given I have access to a <bucket> that has captured messages from tre-out and can publish to SNS topic <sns_topic>
    When I send a valid RequestJudgmentParse with <originator> and <reference> and <judgmentUri>
    Then after a <seconds_delay> seconds delay
    Then I recieve a valid JudgmentPackageAvailable message with the status JUDGMENT_PARSE_NO_ERRORS

    Examples:
      | sns_topic | bucket | originator | reference | judgmentUri | seconds_delay |
      | "arn:aws:sns:eu-west-2:882876621099:pte-bs-tre-in" | "pte-bs-tre-tre-out-capture" | "FCL" | "FCL-TEST-999" | "https://tna-caselaw-assets.s3.eu-west-2.amazonaws.com/eat/2022/1/eat_2022_1.docx" | 120 |
