package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.config.JiraProperties;

import io.atlassian.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
enum BugTracker {
    JIRA {
        private JiraProperties jiraProperties;
        private JiraRestClient client;
        private SearchRestClient searchClient;
        private String jqlQuery;
        private Map<String, BiFunction<String,String,String>>  jqlQueryByEngine = new HashMap<>();


        @Override
        void init(GenericEndToEndSteps genericEndToEndSteps) {
            BiFunction<String, String, String> sastJqlFormat = (project , severities) -> String.format("project = %s and priority  in %s", project, severities);
            jqlQueryByEngine.put("sast", sastJqlFormat);
            jqlQueryByEngine.put("sca" , (project , severities) -> String.format("project = %s and summary ~\"CVE-?\"", jiraProperties.getProject()));
            jqlQueryByEngine.put("ast" , sastJqlFormat);

            jiraProperties = genericEndToEndSteps.jiraProperties;
            CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
            URI jiraURI;
            try {
                jiraURI = new URI(jiraProperties.getUrl());
            } catch (URISyntaxException e) {
                fail("Error constructing URI for JIRA");
                jiraURI = null;
            }
            client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(),
                    jiraProperties.getToken(), jiraProperties.getHttpTimeout());
            searchClient = client.getSearchClient();
        }

        @Override
        void verifyIssueCreated(String severities, String engine) {
            
            assertTrue(jqlQueryByEngine.containsKey(engine) , "verifyIssueCreated does not support engine: " + engine);
            jqlQuery = jqlQueryByEngine.get(engine).apply(jiraProperties.getProject(), severities);

            log.info("filtering issue with jql: {}", jqlQuery);
            Set<String> fields = Stream.of("key", "project", "issuetype", "summary", "labels", "created", "updated", "status")
                .collect(Collectors.toSet());
            SearchResult result = null;
            boolean found = false;
            for (int retries = 0; retries < 20; retries++) {
                Promise<SearchResult> temp = searchClient.searchJql(jqlQuery, 10, 0, fields);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e) {
                    log.info("starting attempt {}", retries + 1);
                }
                try {
                    result = temp.get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.info("failed attempt {}", retries + 1);
                }

                if (result != null && result.getTotal() > 0) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                String msg = "failed to find update in Jira after expected time";
                log.error(msg);
                fail(msg);
            }
        }

        @Override
        void deleteIssues() {
            try {
                Set<String> fields = Stream.of("key", "project", "issuetype", "summary", "labels", "created", "updated", "status")
                    .collect(Collectors.toSet());
                Promise<SearchResult> temp = searchClient.searchJql(jqlQuery, 10, 0, fields);
                SearchResult result = temp.get(500, TimeUnit.MILLISECONDS);

                IssueRestClient issueClient = client.getIssueClient();
                boolean isfound = false;
                for (Issue currentIssue : result.getIssues()) {
                    isfound = true;
                    issueClient.deleteIssue(currentIssue.getKey(), false);
                }
                if (isfound) {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                    deleteIssues();
                }
            } catch (Exception e) {
                log.warn("Failed to clean tickets from Jira");
            }
        }
    };

    static BugTracker setTo(String bugTracker, GenericEndToEndSteps genericEndToEndSteps) {
        log.info("setting bug-tracker to {}", bugTracker);
        BugTracker bt = valueOf(bugTracker);
        bt.init(genericEndToEndSteps);
        return bt;
    }

    abstract void verifyIssueCreated(String severities, String engine);

    abstract void deleteIssues();

    abstract void init(GenericEndToEndSteps genericEndToEndSteps);
}