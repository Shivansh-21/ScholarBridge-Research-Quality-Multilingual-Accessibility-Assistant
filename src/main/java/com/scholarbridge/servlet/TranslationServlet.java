package com.scholarbridge.servlet;

import com.scholarbridge.dao.DocumentDAO;
import com.scholarbridge.dao.TranslationDAO;
import com.scholarbridge.model.Document;
import com.scholarbridge.model.Translation;
import com.scholarbridge.service.TranslationService;
import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@WebServlet("/api/translations/*")
public class TranslationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
        try{
            long id=parseId(request.getPathInfo());
            List<Translation> rows=new TranslationDAO().findByDocumentId(id);
            StringBuilder json=new StringBuilder("[");
            for(int i=0;i<rows.size();i++){
                if(i>0)json.append(',');
                Translation row=rows.get(i);
                json.append("{\"language\":\"").append(JsonUtil.escape(row.getLanguage()))
                    .append("\",\"contentType\":\"").append(JsonUtil.escape(row.getContentType()))
                    .append("\",\"content\":\"").append(JsonUtil.escape(row.getContent()))
                    .append("\",\"aiGenerated\":").append(row.isAiGenerated()).append("}");
            }
            json.append("]");
            JsonUtil.json(response,200,json.toString());
        }catch(NumberFormatException exception){JsonUtil.error(response,400,"Invalid document ID.");}
        catch(SQLException exception){JsonUtil.error(response,503,"Database unavailable.");}
    }

    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        try{
            long id=parseId(request.getPathInfo());
            Optional<Document> found=new DocumentDAO().findById(id);
            if(found.isEmpty()){JsonUtil.error(response,404,"Document not found.");return;}
            Document document=found.get();
            if(document.getAbstractText()==null||document.getAbstractText().isBlank()){
                JsonUtil.error(response,422,"No abstract was detected in the uploaded PDF.");
                return;
            }
            String body=readBody(request);
            String language=readString(body,"language");
            boolean simplify="simple_english".equalsIgnoreCase(language);
            if(!simplify&&!language.matches("(?i)(hindi|punjabi|Hindi|Punjabi)")){
                JsonUtil.error(response,400,"Language must be Hindi, Punjabi or simple_english.");
                return;
            }
            TranslationService service=new TranslationService();
            TranslationService.TranslationResult result=simplify
                    ? service.simplify(document.getAbstractText())
                    : service.translate(document.getAbstractText(),language);
            if(!result.success()){
                JsonUtil.json(response,503,"{\"success\":false,\"message\":\""+JsonUtil.escape(result.content())+"\"}");
                return;
            }
            Translation saved=new Translation();
            saved.setDocumentId(id);
            saved.setLanguage(simplify?"English (Simplified)":language);
            saved.setContentType(simplify?"simplified":"translation");
            saved.setContent(result.content());
            saved.setAiGenerated(true);
            new TranslationDAO().upsert(saved);
            JsonUtil.json(response,200,"{\"success\":true,\"aiGenerated\":true,\"content\":\""
                +JsonUtil.escape(result.content())+"\"}");
        }catch(NumberFormatException exception){JsonUtil.error(response,400,"Invalid document ID.");}
        catch(SQLException exception){JsonUtil.error(response,503,"Database unavailable.");}
    }

    private String readBody(HttpServletRequest request)throws IOException{
        StringBuilder body=new StringBuilder();
        try(BufferedReader reader=request.getReader()){
            String line;
            while((line=reader.readLine())!=null)body.append(line);
        }
        return body.toString();
    }

    private String readString(String json,String key){
        String marker="\""+key+"\"";
        int start=json.indexOf(marker);
        if(start<0)return "";
        start=json.indexOf(':',start)+1;
        while(start<json.length()&&Character.isWhitespace(json.charAt(start)))start++;
        if(start>=json.length()||json.charAt(start)!='"')return "";
        int end=json.indexOf('"',start+1);
        return end<0?"":json.substring(start+1,end);
    }

    private long parseId(String path){
        if(path==null||path.length()<2)throw new NumberFormatException();
        return Long.parseLong(path.substring(1).split("/")[0]);
    }
}
