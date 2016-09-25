package StagAppServer.servlets;

import StagAppServer.DBHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class MyHttpServlet extends HttpServlet{

    public MyHttpServlet(){
        System.out.println("http");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String validationCode = req.getParameter("validCode");
        String responseString;
        System.out.println("GET: " + validationCode);

        resp.setContentType("text/html;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        if (DBHandler.validateEmail(validationCode)){
            responseString = "<p align=\"center\">Congratulation! Your e-mail was confirmed!</p>";
        } else {
            responseString = "<p align=\"center\">Ooops... This link is invalid.</p>";
        }
        resp.getWriter().println(responseString);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }
}
