package com.campus.freshman.rag;

import com.campus.freshman.CampusServerApplication;
import org.springframework.boot.SpringApplication;
import java.nio.file.Files;

/** 仅供浏览器验收：独立端口、内存数据库和临时文件目录，不访问本机业务MySQL。 */
public class WorkspaceBrowserServer {
    public static void main(String[] args) throws Exception {
        System.setProperty("server.port","8099");System.setProperty("server.address","127.0.0.1");
        System.setProperty("campus.workspace.demo-mode","true");
        System.setProperty("spring.datasource.url","jdbc:h2:mem:workspace_browser;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.datasource.driver-class-name","org.h2.Driver");System.setProperty("spring.datasource.username","sa");System.setProperty("spring.datasource.password","");
        System.setProperty("spring.jpa.hibernate.ddl-auto","create-drop");System.setProperty("campus.seed-demo-data","false");
        System.setProperty("campus.knowledge-dir",Files.createTempDirectory("campus-workspace-browser-").toString());
        SpringApplication.run(CampusServerApplication.class,args);
    }
}
