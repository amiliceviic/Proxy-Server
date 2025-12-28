/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package proxyServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 *
 * @author Aleksandar Milicevic
 */
public class ProxyWithCaching extends Thread {
    
    Socket clientSocket;
    static String cacheFolder = "Cached/";
    static ArrayList<CachedObject> listCachedObjects = new ArrayList<>();
    static Object lock = new Object();
    
    public ProxyWithCaching(Socket clientSocket) {
        this.clientSocket = clientSocket;
        start();
    }
    
    public static void loadCacheFromDisk() {
        int fileNumber = 0;
        
        System.out.println("Ucitavam kes: ");
        
        while(true) {
            File file = new File(cacheFolder + fileNumber + ".txt");
            
            if(!file.exists()) break;
            
            CachedObject co = new CachedObject(fileNumber);
            
            listCachedObjects.add(co);
            
            System.out.println(fileNumber + ": " + co.getHost() + co.getUrl());
            fileNumber++;
        }
    }
    
    public static void main(String[] args) {
        
        try {
            if(args.length > 0) {
                cacheFolder = args[0];
                
                if(!cacheFolder.endsWith("/")) {
                    cacheFolder = cacheFolder + "/";
                }
            }
            
            loadCacheFromDisk();
            
            ServerSocket serverSocket = new ServerSocket(8080);
            
            System.out.println("Proxy poceo sa radom na portu 8080");
            
            while(true) {
                new ProxyWithCaching(serverSocket.accept());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader ulazniTokOdKlijenta = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintStream izlazniTokKaKlijentu = new PrintStream(clientSocket.getOutputStream());
            
            String textLine = ulazniTokOdKlijenta.readLine();
            String[] request = textLine.split(" ");
            
            if(request.length != 3) {
                System.out.println("Lose zaglavlje: " + textLine);
                clientSocket.close();
                return;
            }
            
            String header = "";
            String host = null;
            int port = 80;
            
            while(true) {
                textLine = ulazniTokOdKlijenta.readLine();
                
                if(textLine.equals("")) break;
                
                if(textLine.startsWith("Host")) host = textLine.substring(textLine.indexOf(" ") + 1);
                
                if(host.indexOf(":") > 0) {
                    String[] tmp = host.split(":");
                    host = tmp[0];
                    port = Integer.parseInt(tmp[1]);
                }
                
                if(!textLine.startsWith("Proxy") && (!textLine.startsWith("Pragma")) && (!textLine.startsWith("Cache"))) {
                    header = header + textLine + "\n";
                }
            }
            
            if(request[1].startsWith("http://")) {
                request[1] = request[1].substring(7 + host.length());
            }
            
            byte[] buffer = new byte[10240];
            
            CachedObject co = new CachedObject(request[1], host);
            
            int i = listCachedObjects.indexOf(co);
            
            if(i != -1) {
                co = listCachedObjects.get(i);
                
                izlazniTokKaKlijentu.print(co.response);
                izlazniTokKaKlijentu.print("Proxt-Connection: close\n");
                izlazniTokKaKlijentu.print("\n");
                
                FileInputStream ulazniTokFajl = new FileInputStream(cacheFolder + i + ".bin");
                
                int n;
                
                while(true) {
                    n = ulazniTokFajl.read(buffer);
                    
                    if(n == -1) break;
                    
                    izlazniTokKaKlijentu.write(buffer, 0, n);
                }
                clientSocket.close();
                return;
            }
            
            System.out.println(request[1] + " nije kesiran, povezuje se na " + host);
            
            Socket soketKaServeru = new Socket(host, port);
            
            DataInputStream ulazniTokOdServera = new DataInputStream(soketKaServeru.getInputStream());
            PrintStream izlazniTokKaServeru = new PrintStream(soketKaServeru.getOutputStream(), true);
            
            izlazniTokKaServeru.print(request[0] + " " + request[1] + " HTTP/1.1\n");
            izlazniTokKaServeru.print(header);
            izlazniTokKaServeru.print("Connection: close\n");
            izlazniTokKaServeru.print("\n");
            
            String responseLine = ulazniTokOdServera.readLine();
            
            if(responseLine != null && responseLine.contains("HTTP/1.1 200")) {
                synchronized (lock) {
                    listCachedObjects.add(co);
                }
                System.out.println("Dodaj u kes: " + request[1]);
            }
            
            int numInCache = listCachedObjects.indexOf(co);
            System.out.println("Br u kesu: " + numInCache);
            
            while(responseLine != null) {
                if(numInCache >= 0) co.addResponseLine(responseLine);
                
                izlazniTokKaKlijentu.print(responseLine + "\n");
                
                responseLine = ulazniTokOdServera.readLine();
                
                if(responseLine == null) {
                    clientSocket.close();
                    soketKaServeru.close();
                    return;
                }
                
                if(responseLine.equals("")) break;
            }
            
            izlazniTokKaKlijentu.print("Proxy-Connection: close\n");
            izlazniTokKaKlijentu.print("\n");
            
            FileOutputStream izlazniTokFajl = null;
            
            if(numInCache >= 0) {
                izlazniTokFajl = new FileOutputStream(cacheFolder + numInCache + ".bin");
            }
            
            while(true) {
                int n = ulazniTokOdServera.read(buffer);
                
                if(n == -1) break;
                
                izlazniTokKaKlijentu.write(buffer, 0, n);
                
                if(numInCache >= 0) {
                    izlazniTokKaKlijentu.write(buffer, 0, n);
                }
            }
            
            if(numInCache >= 0) {
                izlazniTokFajl.close();
                co.save(numInCache);
            }
            
            clientSocket.close();
            soketKaServeru.close();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
