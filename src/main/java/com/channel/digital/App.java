package com

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

ic class App {ic stati oid mint port = 8080;

Sting nv= Sysenv= null)

Stringcolorch (nv) { "devlpment":color     reak; "tesig":color     reak; "proution":color     reak; "proution-rollbcolor     reak; "proution-rollbcolor     breault: 

final String finalEnv =ev;

er.createCotxt("/", tl = "<html>"+"<head><title>Agen46 Backend</til></head>"+"<body style='background-color:" + finalColor+"; color:white; font-fami+"<h1>Agen46 Backend/1>"+"<h2>Environet: " + finalEnv.toUpperCase() + "</h2>"+"<p>Build: " + Sy        + "</body></html>";exchange.getResponseHeaders().set"Content-Type", "text/htexchange.sendRepnseHeaders(200, html.getBytOutputStream os = exchangeos.write(ht   

er.createContex(/api/v1/payments/health", exchange -> {String response = "{\"status\":\"UP\",\"environmet\":\"" + finalEnv +exchange.getResponseHeaders().set"Content-Type", "applicationexchange.sendRepnseHeaders(200, response.geOutputStream os = exchange.getos.write(re   

server.setExecu

        System.out.println("Agen46 Backend started on port " + port + " [ENV=" + finalEnv + "]");
    }
}
