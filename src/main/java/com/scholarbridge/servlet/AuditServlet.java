package com.scholarbridge.servlet;

import com.scholarbridge.dao.AuditResultDAO;
import com.scholarbridge.dao.IssueDAO;
import com.scholarbridge.model.AuditResult;
import com.scholarbridge.model.Issue;
import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@WebServlet("/api/audit/*")
public class AuditServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long id=parseId(request.getPathInfo());
            Optional<AuditResult> found=new AuditResultDAO().findByDocumentId(id);
            if(found.isEmpty()){JsonUtil.error(response,404,"No audit result found.");return;}
            AuditResult audit=found.get();
            List<Issue> issues=new IssueDAO().findByDocumentId(id);
            StringBuilder json=new StringBuilder("{\"qualityScore\":").append(audit.getQualityScore())
                .append(",\"totalIssues\":").append(audit.getTotalIssues())
                .append(",\"criticalCount\":").append(audit.getCriticalCount())
                .append(",\"mediumCount\":").append(audit.getMediumCount())
                .append(",\"lowCount\":").append(audit.getLowCount())
                .append(",\"scoreBreakdown\":\"").append(JsonUtil.escape(audit.getScoreBreakdown())).append("\"")
                .append(",\"issues\":[");
            for(int i=0;i<issues.size();i++){
                if(i>0)json.append(',');
                Issue issue=issues.get(i);
                json.append("{\"severity\":\"").append(JsonUtil.escape(issue.getSeverity()))
                    .append("\",\"category\":\"").append(JsonUtil.escape(issue.getCategory()))
                    .append("\",\"recommendation\":\"").append(JsonUtil.escape(issue.getRecommendation())).append("\"}");
            }
            json.append("]}");
            JsonUtil.json(response,200,json.toString());
        } catch(NumberFormatException exception){JsonUtil.error(response,400,"Invalid document ID.");}
        catch(SQLException exception){JsonUtil.error(response,503,"Database unavailable.");}
    }

    private long parseId(String path){
        if(path==null||path.length()<2)throw new NumberFormatException();
        return Long.parseLong(path.substring(1).split("/")[0]);
    }
}
