/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package proxyServer;

import java.io.*;

/**
 *
 * @author Aleksandar Milicevic
 */
public class CachedObject {
    
    String host;
    String url;
    String response = "";

    public CachedObject(String url, String host) {
        this.url = url;
        this.host = host;
    }
    
    public CachedObject(int n) {
        load(n);
    }
    
    public void addResponseLine(String s) {
        response += s + "\n";
    }
    
    public void save(int n) {
        try {
            FileWriter fileWrite = new FileWriter(ProxyWithCaching.cacheFolder + n + ".txt");
            
            fileWrite.write(url + "\n");
            fileWrite.write(host + "\n");
            fileWrite.write(response + "\n");
            fileWrite.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void load(int n) {
        try {
            BufferedReader ulazniTokIzFajla = new BufferedReader(new FileReader(ProxyWithCaching.cacheFolder + n + ".txt"));
            
            url = ulazniTokIzFajla.readLine();
            host = ulazniTokIzFajla.readLine();
            String s;
            
            while(true) {
                s = ulazniTokIzFajla.readLine();
                
                if(s == null || s.equals("")) break;
                
                addResponseLine(s);
            }
            
            ulazniTokIzFajla.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object arg0) {
        if(arg0 instanceof CachedObject) {
            CachedObject cachedObject = (CachedObject) arg0;
            return cachedObject.host.equals(host) && cachedObject.url.equals(url);
        }
        return false;
    }

    public String getHost() {
        return host;
    }

    public String getUrl() {
        return url;
    }

    public String getResponse() {
        return response;
    }

}
