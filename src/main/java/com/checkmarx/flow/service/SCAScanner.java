package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.cx.restclient.ScaClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class SCAScanner extends AbstractASTScanner {
    public SCAScanner(ScaClientImpl scaClient, FlowProperties flowProperties, BugTrackerEventTrigger bugTrackerEventTrigger) {
        super(scaClient, flowProperties, ScaProperties.CONFIG_PREFIX, bugTrackerEventTrigger);
    }

    @Override
    protected ScanResults toScanResults(ASTResultsWrapper internalResults) {
        return ScanResults.builder()
                .scaResults(internalResults.getScaResults())
                .build();
    }

    @Override
    protected String getScanId(ASTResultsWrapper internalResults) {
        return Optional.ofNullable(internalResults.getScaResults().getScanId()).orElse("");
    }
}