package com.checkmarx.flow.cucumber.integration.cli.ast;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.cli.IntegrationTestContext;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.AstProperties;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class AstCliSteps {

    private static final String GITHUB_REPO_ARGS = " --repo-url=https://github.com/cxflowtestuser/CLI-Integration-Tests --repo-name=CLI-Integration-Tests --github --branch=master --blocksysexit";
    private static final String JIRA_PROJECT = "CIT";

    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final IntegrationTestContext testContext;
    private final AstProperties astProperties;

    private String commandlineConstantArgs;
    private int expectedHigh;
    private int expectedMedium;
    private int expectedLow;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Before("@AST_CLI_SCAN")
    public void beforeEachScenario() throws IOException {
        log.info("Setting bugTracker: Jira");
        flowProperties.setBugTracker("JIRA");

        initAstConfig();
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(AstProperties.CONFIG_PREFIX));
        
        log.info("Jira project key: {}", JIRA_PROJECT);
        jiraProperties.setProject(JIRA_PROJECT);
        // try {
        //     initJiraBugTracker();
        // } catch (Throwable e) {
        //     log.error("not running AST CLI scan, can not connect to Jira - {}", e.getMessage());
        //     assumeNoException("not running AST CLI scan, can not connect to Jira", e);
        // }
    }

    private void initAstConfig() {
        astProperties.setApiUrl("http://10.32.4.33");
        astProperties.setToken("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrdkdrM1k5ODBrM29VV0VxM2NscjlOeEIwbTlCbE90N3k0TlRWbHBRRFFNIn0.eyJleHAiOjE1OTc3ODAyMDIsImlhdCI6MTU5Nzc0NDIwMiwiYXV0aF90aW1lIjoxNTk3NzQ0MjAyLCJqdGkiOiI4ZmE1NzNiMC0xYmY0LTQxOGYtYjA5Yy1iYjcwOTMzOTdmNjUiLCJpc3MiOiJodHRwOi8vMTAuMzIuNC4zMy9hdXRoL3JlYWxtcy9vcmdhbml6YXRpb24iLCJhdWQiOiJhc3QtYXBwIiwic3ViIjoiMTg4MzE1MWUtNzI5Yi00MTQ4LThjYmUtYzcxOTUwMWEzNDRkIiwidHlwIjoiSUQiLCJhenAiOiJhc3QtYXBwIiwibm9uY2UiOiJhODAxZDRiOC1hNWI4LTRiNTctOWM2Ni04Mzc0Y2VjYjg2ZTQiLCJzZXNzaW9uX3N0YXRlIjoiN2M3OGM0ZGUtZDExNy00MzRhLThiMDctM2VjNDBiNjg4MWM1IiwiYXRfaGFzaCI6IjdkUnRKWGxXdGNvbGZfTnZmTEdSY0EiLCJjX2hhc2giOiI2RXFqSkFLeE9peUlnaXhrRkR0NGJRIiwiYWNyIjoiMSIsInNfaGFzaCI6Ims5YVlISHZDT2RRdlJmTGMwdFFLNmciLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJ1c2VyIiwiaWFtLWFkbWluIl0sInJvbGVzX2FzdCI6WyJjcmVhdGUtcHJvamVjdCIsInZpZXctcHJvamVjdHMiLCJkZWxldGUtYXBwbGljYXRpb24iLCJ2aWV3LWFwcGxpY2F0aW9ucyIsIm1hbmFnZS13b3Jrc3BhY2UiLCJjcmVhdGUtYXBwbGljYXRpb24iLCJtYW5hZ2UtcmVzdWx0IiwibWFuYWdlLXByb2plY3QiLCJ2aWV3LXJlc3VsdHMiLCJjcmVhdGUtd29ya3NwYWNlIiwiZGVsZXRlLXByb2plY3QiLCJ2aWV3LXNjYW5zIiwiZGVsZXRlLXdvcmtzcGFjZSIsImFzdC1hZG1pbiIsImNyZWF0ZS1zY2FuIiwiZGVsZXRlLXNjYW4iLCJtYW5hZ2UtYXBwbGljYXRpb24iLCJ2aWV3LXdvcmtzcGFjZXMiLCJ2aWV3LWhlYWx0aGNoZWNrIiwidmlldy1lbmdpbmXigItzIl0sInByZWZlcnJlZF91c2VybmFtZSI6Im9yZ19hZG1pbiJ9.GmGBy8heYk05HQbAAUy6s2LjyzeNZxrbny8ZR_UCFIZOAci7MatRL6POWs9Z9BxQB7yNxeEYR_8eu1gFDoUEOltjcw_0ehspmDe1BIIJFODmjxNodtIoybGJIeIYJz9qAdvYEgjz9YntKDS0ZaSNFTOmTy0ZpHoQc2CoPJPnrBa1-j0Vx7cCgZ7ZNluPOuWyj9ljhHeXnSSdAKsVZ1tbiqY4vrOjJVKYVEC1R2YSYcom6ACc063vkUKoz6SV9Kxzy_YVH5gXAzyOMDdn1uW-5ql_vy0dqtj4BK0LDlUcNcVMcuZ868oI8XvFNELDQQaAlFVL2ciRs2RNQ0j3PSZIrw&access_token=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrdkdrM1k5ODBrM29VV0VxM2NscjlOeEIwbTlCbE90N3k0TlRWbHBRRFFNIn0.eyJleHAiOjE1OTc3ODAyMDIsImlhdCI6MTU5Nzc0NDIwMiwiYXV0aF90aW1lIjoxNTk3NzQ0MjAyLCJqdGkiOiJkM2QzMTFjYy1hM2UxLTRhNzMtOTJiYS1lZDBmOTc1OGIzZGIiLCJpc3MiOiJodHRwOi8vMTAuMzIuNC4zMy9hdXRoL3JlYWxtcy9vcmdhbml6YXRpb24iLCJhdWQiOlsicmVhbG0tbWFuYWdlbWVudCIsImFjY291bnQiXSwic3ViIjoiMTg4MzE1MWUtNzI5Yi00MTQ4LThjYmUtYzcxOTUwMWEzNDRkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYXN0LWFwcCIsIm5vbmNlIjoiYTgwMWQ0YjgtYTViOC00YjU3LTljNjYtODM3NGNlY2I4NmU0Iiwic2Vzc2lvbl9zdGF0ZSI6IjdjNzhjNGRlLWQxMTctNDM0YS04YjA3LTNlYzQwYjY4ODFjNSIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiKiJdLCJzY29wZSI6Im9wZW5pZCBpYW0tYXBpIHByb2ZpbGUgZW1haWwgYXN0LWFwaSByb2xlcyIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiLCJpYW0tYWRtaW4iXSwicm9sZXNfYXN0IjpbImNyZWF0ZS1wcm9qZWN0Iiwidmlldy1wcm9qZWN0cyIsImRlbGV0ZS1hcHBsaWNhdGlvbiIsInZpZXctYXBwbGljYXRpb25zIiwibWFuYWdlLXdvcmtzcGFjZSIsImNyZWF0ZS1hcHBsaWNhdGlvbiIsIm1hbmFnZS1yZXN1bHQiLCJtYW5hZ2UtcHJvamVjdCIsInZpZXctcmVzdWx0cyIsImNyZWF0ZS13b3Jrc3BhY2UiLCJkZWxldGUtcHJvamVjdCIsInZpZXctc2NhbnMiLCJkZWxldGUtd29ya3NwYWNlIiwiYXN0LWFkbWluIiwiY3JlYXRlLXNjYW4iLCJkZWxldGUtc2NhbiIsIm1hbmFnZS1hcHBsaWNhdGlvbiIsInZpZXctd29ya3NwYWNlcyIsInZpZXctaGVhbHRoY2hlY2siLCJ2aWV3LWVuZ2luZeKAi3MiXSwicHJlZmVycmVkX3VzZXJuYW1lIjoib3JnX2FkbWluIn0.yk9VJTXpJ21NQ0gKadWy-SqB7Ht8SGwwJGQGwMROOaOKaLnRtXAFqkAyrx-OhRRyNNfCruKGvDdFeRZr8Nbpy34ZJR1-w2KjQSuesLzj7IHF9ou13VZQm28OyGHj1XORN23RbgkrYZCuAaFpGk8Zba4N6ZnVdY-L_cawkXiJMNqSitkLH1iHKRHRKOys1j6hXNj07YKtY6acq47TwaYRzs7BmS3VEyuVPzfICqM7qDQhS0BRapHzJB9cN4EAfs-dhq4BEb8OuO_zFUFdDqUf1Fss7qP-1fUSH_UekLSW_n-UC-0Xi_J_5tkrdN8TBwG3YDiZaehwFXMIcZ4mdLoSzg");
        astProperties.setPreset("Checkmarx Default");
        astProperties.setIncremental("false");
    }

    @After()
    public void afterEachScenario() {
        log.info("Cleaning JIRA project: {}", jiraProperties.getProject());
        try {
            jiraUtils.cleanProject(jiraProperties.getProject());
        } catch (RestClientException e) {
            log.error("could not clean project ({}) after Scenario", jiraProperties.getProject());
            // fail("could not clean project after Scenario");
        }
    }

    @Given("repository is github-ast")
    public void setRepo() {
        commandlineConstantArgs = GITHUB_REPO_ARGS;
    }

    @When("running with break-build on {word}")
    public void runningWithBreakBuild(String issueType) {
        StringBuilder commandBuilder = new StringBuilder();


        switch (issueType) {
            case "success":
                commandBuilder.append("--scan --scanner=ast --severity=High --app=MyApp").append(commandlineConstantArgs);
                break;
            case "missing-mandatory-parameter":
                commandBuilder.append("--scanner=ast --severity=High --severity=Medium").append(commandlineConstantArgs);
                break;
            case "error-processing-request":
                commandBuilder.append("--scan --scanner=ast --severity=High --severity=Medium").append(commandlineConstantArgs);
                break;

            default:
                throw new PendingException("Issues type " + issueType + " isn't supported");
        }

        log.info("Running CxFlow scan with command line: {}", commandBuilder.toString());
        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    @Then("run should exit with exit code {int}")
    public void validateExitCode(int expectedExitCode) {
        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exist code did not match",
                expectedExitCode, actualExitCode);
    }

    @Given("code has x High, y Medium and z low issues")
    public void setIssues() {
        expectedHigh = 2;
        expectedMedium = 4;
        expectedLow = 7;
    }

    @When("running ast scan {word}")
    public void runnningScanWithFilter(String filter) {
        StringBuilder commandBuilder = new StringBuilder();

        switch (filter) {
            case "no-filter":
                commandBuilder.append(" --scan  --scanner=ast --severity=High --severity=Medium --severity=Low --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-High-and-Medium":
                commandBuilder.append(" --scan  --scanner=ast --severity=High --severity=Medium --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-only-Medium":
                commandBuilder.append(" --scan  --scanner=ast --severity=Medium --app=MyApp").append(commandlineConstantArgs);
                break;
            case "filter-invalid-cwe":
                commandBuilder.append(" --scan  --scanner=ast --cwe=1 --app=MyApp").append(commandlineConstantArgs);
                break;
            default:
                throw new PendingException("Filter " + filter + " isn't supported");
        }

        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), commandBuilder.toString());
        } catch (Throwable e) {
        }
    }

    @Then("bugTracker contains {word} issues")
    public void validateBugTrackerIssues(String numberOfIssues) {
        int expectedIssuesNumber;

        switch (numberOfIssues) {
            case "x+y+z":
                expectedIssuesNumber = expectedHigh + expectedMedium + expectedLow;
                break;
            case "x+y":
                expectedIssuesNumber = expectedHigh + expectedMedium;
                break;
            case "y":
                expectedIssuesNumber = expectedMedium;
                break;
            case "0":
                expectedIssuesNumber = 0;
                break;

            default:
                throw new PendingException("Number of issues parameter " + numberOfIssues + " isn't supported");
        }

        int actualOfJiraIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Assert.assertEquals(expectedIssuesNumber, actualOfJiraIssues);
    }

    private void initJiraBugTracker() throws IOException {
        log.info("Cleaning jira project before test: {}", jiraProperties.getProject());
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }
}