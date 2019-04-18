package com.didi.chatroom1.server.multi;


import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端处理客户端连接的任务
 * 1.注册
 * 2.私聊
 * 3.群聊
 * 4.退出
 * 5.显示当前在线用户
 * 6.统计用户活跃度
 */
public class ExecuteClient implements Runnable{
    /**
     * 在线用户集合
     */

    private static Map<String,Socket> ONLINE_USER_MAP = new ConcurrentHashMap<String, Socket>();

    private final Socket client;

    public ExecuteClient(Socket socket) {
        this.client = socket;
    }

    @Override
    public void run() {

        try {

            //1.获取客户端输入
            InputStream clientInput = this.client.getInputStream();
            Scanner scanner = new Scanner(clientInput);

            while(true){
            String line = scanner.nextLine();


            /**
             * 1.注册：userName:<name>
             * 2.私聊：private:<name>:<message>
             * 3.群聊：group:<message>
             * 4.退出:bye
             */

            if(line.startsWith("userName")){
                String userName = line.split("\\:")[1];
                //TODO这里参数校验
                this.register(userName,client);
                continue;
            }

            if(line.startsWith("private")){
                String[] segments = line.split("\\:");
                String userName = segments[1];
                String message = segments[2];
                //TODO这里参数校验
                this.privateChat(userName,message);
                continue;
            }
            if(line.startsWith("group")){
                String message = line.split("\\:")[1];
                //TODO这里参数校验
                this.groupChat(message);
                continue;
            }
            if(line.equals("bye")){
                this.quit();
                break;
            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void quit() {
        String currentUserName = this.getCurrentUserName();
        System.out.println("用户："+currentUserName+"下线");
        Socket socket = ONLINE_USER_MAP.get(currentUserName);
        this.sendMessage(socket,"bye");
        ONLINE_USER_MAP.remove(currentUserName);
        this.sendMessage(socket,"bye");
        printOnlineUser();
    }

    private void groupChat(String message) {
        for(Socket socket:ONLINE_USER_MAP.values()){
            if(socket.equals(this.client)){
                continue;
            }
            this.sendMessage(socket,this.getCurrentUserName()+"说："+message);
        }
    }

    private void privateChat(String userName, String message) {
        String currentUserName = this.getCurrentUserName();
        Socket target = ONLINE_USER_MAP.get(userName);
        if(target != null){
            this.sendMessage(target,currentUserName+"对你说："+message);
        }
    }

    private void register(String userName, Socket client) {
        System.out.println(userName+"加入到聊天室"+client.getRemoteSocketAddress());
        ONLINE_USER_MAP.put(userName,client);
        printOnlineUser();
        sendMessage(this.client,userName+"注册成功!");
    }

    private String getCurrentUserName(){
        String currentUserName = "";
        for(Map.Entry<String,Socket>entry:ONLINE_USER_MAP.entrySet()){
            if(this.client.equals(entry.getValue())){
                currentUserName = entry.getKey();
                break;
            }
        }
        return currentUserName;
    }

    private void sendMessage(Socket socket,String message){
        try {
            OutputStream clientOutput = client.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(clientOutput);
            writer.write(message+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printOnlineUser(){
        System.out.println("当前在线人数："+ONLINE_USER_MAP.size()+"用户名列表如下：");
        for(Map.Entry<String,Socket>entry:ONLINE_USER_MAP.entrySet()){
            System.out.println(entry.getKey());
        }
    }
}




