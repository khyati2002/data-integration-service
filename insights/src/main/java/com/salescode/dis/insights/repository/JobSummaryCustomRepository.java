package com.salescode.dis.insights.repository;

import java.util.List;
import java.util.Map;

public interface JobSummaryCustomRepository {
    List<Map<String, Object>> getFilteredLobSummary(List<String> lobs, Map<String, String> jobFilters);
    List<Map<String, Object>> getFilteredLobDetails(List<String> lobs, Map<String, String> jobFilters);
}
