/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package simpleProxyServer;

import java.io.*;
import java.net.*;

/**
 *
 * @author Aleksandar Milicevic
 */
public class ProxyThread extends Thread {
    
    Socket clientSocket;
    
    public ProxyThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        start();
    }
    
    public static void main(String[] args) {
        
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            
            System.out.println("Proxy poceo sa radom na portu 8080");
            
            while(true) {
                new ProxyThread(serverSocket.accept());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    @Override
    public void run() {
        try {
            BufferedReader tokOdKlijenta = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream tokKaKlijentu = new DataOutputStream(clientSocket.getOutputStream());
            
            String textLine = tokOdKlijenta.readLine();
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
                textLine = tokOdKlijenta.readLine();
                System.out.println(textLine);
                
                if(textLine.equals("")) break;
                
                if(textLine.startsWith("Host")) host = textLine.substring(textLine.indexOf(' ') + 1);
                System.out.println(host);
                
                if(host.indexOf(':') > 0) {
                    String[] tmp = host.split(":");
                    host = tmp[0];
                    port = Integer.parseInt(tmp[1]);
                }
                
                if(!textLine.startsWith("Proxy") && (!textLine.startsWith("Pragma")) && (!textLine.startsWith("Cache"))) {
                    header = header + textLine + "\n";
                }
            }
            
            Socket socketKaServeru = new Socket(host, port);
            
            DataInputStream tokOdServera = new DataInputStream(socketKaServeru.getInputStream());
            DataOutputStream tokKaServeru = new DataOutputStream(socketKaServeru.getOutputStream());
            
            System.out.println("Connecting " + host);
            
            if(request[1].startsWith("http://")) {
                request[1] = request[1].substring(7 + host.length());
            }
            
            tokKaServeru.writeBytes(request[0] + " " + request[1] + " HTTP/1.1\n");
            tokKaServeru.writeBytes(header);
            tokKaServeru.writeBytes("Connection: close\n");
            tokKaServeru.writeBytes("\n");
            
            while(true) {
                String responseLine = tokOdServera.readLine();
                if(responseLine == null) {
                    clientSocket.close();
                    return;
                }
                if(responseLine.equals("")) break;
                
                tokKaKlijentu.writeBytes(responseLine + "\n");  
            }
            
            tokKaKlijentu.writeBytes("Proxy-Connection: close");  
            tokKaKlijentu.writeBytes("\n");  
            
            byte[] arrayBytes = new byte[1024];
            
            while(true) {
                int loadedBytes = tokOdServera.read(arrayBytes);
                    
                if(loadedBytes == -1) {
                    break;
                }
                tokKaKlijentu.write(arrayBytes, 0, loadedBytes);
            }
            clientSocket.close();
            socketKaServeru.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    

}
