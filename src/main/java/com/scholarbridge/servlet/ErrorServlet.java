package com.scholarbridge.servlet;

import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/error")
public class ErrorServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException{
        JsonUtil.error(response,500,"An unexpected server error occurred.");
    }
    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException{
        JsonUtil.error(response,500,"An unexpected server error occurred.");
    }
}
