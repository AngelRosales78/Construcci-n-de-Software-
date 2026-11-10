package com.example.sp500auth.sprint3

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.sprint3.RecentCompany
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.time.Instant

class ContractsTest {
    private fun conclusion()=ComparisonConclusion("comparison-conclusion-v1","1","AAPL","MSFT",2024,"rules","2026-11-02T00:00:00Z","v1",listOf("Síntesis"),listOf("Liquidez"),listOf("Rentabilidad"),listOf(com.example.sp500auth.data.api.models.AnalysisSource("SEC","https://www.sec.gov/")))
    @Test fun conclusionRequiresCorrectPairYearOriginAndSources(){
        val value=conclusion();assertTrue(validConclusion(value,"1","AAPL","MSFT",2024))
        for(bad in listOf(value.copy(a="NVDA"),value.copy(owner_id="2"),value.copy(fiscal_year=2023),value.copy(generation_mode="demo"),value.copy(sources=emptyList()),value.copy(paragraphs=listOf(null))))assertFalse(validConclusion(bad,"1","AAPL","MSFT",2024))
    }
    @Test fun accessNeverInfersProFromRoleOrMissingPermission(){
        val value=AccessInfo("subscription-v1","1","pro",true,listOf("compare"),Instant.now().plusSeconds(300).toString())
        assertTrue(canCompare(value));assertFalse(canCompare(value.copy(plan="free")));assertFalse(canCompare(value.copy(features=emptyList())))
        assertFalse(canCompare(value.copy(valid_until="2020-01-01T00:00:00Z")));assertFalse(canCompare(value.copy(active=false)))
    }
    @Test fun pdfMustMatchHashLengthHeaderAndTrailer(){
        val bytes="%PDF-1.4\nfixture\n%%EOF".toByteArray();val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        assertTrue(verifyPdf(bytes,hash,bytes.size.toLong()))
        assertFalse(verifyPdf(bytes,hash,bytes.size.toLong()+1));assertFalse(verifyPdf(bytes,"0".repeat(64),bytes.size.toLong()))
        assertFalse(verifyPdf("<html>error</html>".toByteArray(),hash,17));assertFalse(verifyPdf(bytes.dropLast(5).toByteArray(),hash,(bytes.size-5).toLong()))
    }
    @Test fun reportMetadataCannotChangeRequestedCompanyOptionsOrOrigin(){
        val request=ReportRequest(a="AAPL",fiscal_year=2024,include_charts=true,include_analysis=true,analysis_version="v1",analysis_origin="mixed")
        val value=ReportJob("report-job-v1","1","job1","ready",request,"v1","0".repeat(64),100,chart_format="vector",chart_range="1Y",analysis_version="v1",analysis_origin="mixed")
        assertTrue(validReportRequest(request));assertTrue(validReportJob(value,"1",request))
        assertFalse(validReportJob(value.copy(chart_format="png"),"1",request));assertFalse(validReportJob(value.copy(analysis_origin="ai"),"1",request))
        assertFalse(validReportJob(value.copy(request=request.copy(a="MSFT")),"1",request));assertFalse(validReportJob(value,"2",request))
        assertFalse(validReportRequest(request.copy(b="AAPL",kind="comparison")));assertFalse(validReportRequest(request.copy(analysis_version=null)))
    }
    @Test fun comparisonReportRequiresAtMostTwoLandscapePages(){
        val request=ReportRequest(a="AAPL",b="MSFT",kind="comparison",fiscal_year=2024)
        val value=ReportJob("report-job-v1","1","job1","ready",request,"v1","0".repeat(64),100,page_count=2,orientation="landscape")
        assertTrue(validReportJob(value,"1",request));assertFalse(validReportJob(value.copy(page_count=3),"1",request));assertFalse(validReportJob(value.copy(orientation="portrait"),"1",request))
    }
    @Test fun recentHistoryIsPrivateUniqueSortedAndLimitedToTen(){
        val rows=listOf(RecentCompany("AAPL","Apple","2026-11-02T10:00:00Z"),RecentCompany("MSFT","Microsoft","2026-11-01T10:00:00Z"))
        val value=RecentPage("recent-companies-v1","1",rows)
        assertTrue(validRecent(value,"1"));assertFalse(validRecent(value,"2"));assertFalse(validRecent(value.copy(results=rows.reversed()),"1"))
        assertFalse(validRecent(value.copy(results=listOf(rows.first(),rows.first())),"1"));assertFalse(validRecent(value.copy(results=(1..11).map{RecentCompany("A$it",null,"2026-11-02T00:00:00Z")}),"1"))
    }
    @Test fun tagsValidateNamesAssignmentsOwnersAndVersions(){
        val value=TagsSnapshot("tags-v1","1","v1",listOf(TagItem(1,"Tecnología","#36F5A0"),TagItem(2,"Dividendos","#60CDF9")),listOf(TagAssignment(7,listOf(1,2))))
        assertTrue(validTags(value,"1"));assertFalse(validTags(value,"2"));assertFalse(validTags(value.copy(version=""),"1"))
        assertFalse(validTags(value.copy(tags=value.tags!!+TagItem(3,"  TECNOLOGÍA ","#36F5A0")),"1"))
        assertFalse(validTags(value.copy(assignments=listOf(TagAssignment(7,listOf(99)))),"1"))
        assertEquals(normalizedTag("  Tecnología  "),normalizedTag("TECNOLOGI\u0301A"));assertFalse(validTagName("\n"))
    }
}
