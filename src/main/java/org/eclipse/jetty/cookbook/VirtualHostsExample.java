//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.cookbook;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.IO;

@SuppressWarnings("Duplicates")
public class VirtualHostsExample
{
    public static void main(String[] args)
    {
        VirtualHostsExample example = new VirtualHostsExample();
        try
        {
            example.startServer();
            example.testRequest("a.company.com","/hello");
            example.testRequest("b.company.com","/hello");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            example.stopServer();
        }
    }
    
    private Server server;

    private void stopServer()
    {
        try { server.stop(); }
        catch (Exception ignore) { }
    }

    private void startServer() throws Exception
    {
        server = new Server(8080);
        HandlerCollection handlers = new HandlerCollection();
        server.setHandler(handlers);
        
        handlers.addHandler(createContext("/", "a.company.com"));
        handlers.addHandler(createContext("/", "b.company.com"));
        
        server.start();
    }

    private ContextHandler createContext(String contextPath, final String host)
    {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(contextPath);
        @SuppressWarnings("serial")
        ServletHolder helloholder = new ServletHolder(new HttpServlet()
        {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                resp.setContentType("text/plain");
                resp.getWriter().printf("Hello from [%s] context%n",host);
            }
        });
        context.addServlet(helloholder, "/hello");
        context.addServlet(DefaultServlet.class,"/");
        ContextHandler vhwrapper = new ContextHandler();
        vhwrapper.setHandler(context);
        vhwrapper.setVirtualHosts(new String[]{host});
        return vhwrapper;
    }

    private void testRequest(String host, String path)
    {
        try(Socket client = new Socket("localhost",8080);)
        {
            System.out.printf("-- testRequest [%s] [%s] --%n",host,path);
            String req = String.format("GET %s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n",path,host);
            System.out.print(req);
            client.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            String response = IO.toString(client.getInputStream());
            System.out.print(response);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
