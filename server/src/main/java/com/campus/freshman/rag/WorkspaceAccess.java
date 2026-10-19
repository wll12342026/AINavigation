package com.campus.freshman.rag;
import com.campus.freshman.auth.AdminSessions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Configuration
@RestController
public class WorkspaceAccess implements WebMvcConfigurer {
    private final AdminSessions sessions;
    private final boolean demoMode;
    public WorkspaceAccess(AdminSessions sessions, @Value("${campus.workspace.demo-mode:false}") boolean demoMode) {this.sessions=sessions;this.demoMode=demoMode;}
    @GetMapping("/api/workspace-access")
    public Map<String,Boolean> access() {return Map.of("requiresLogin",!demoMode,"demoMode",demoMode);}
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                if(demoMode || "OPTIONS".equals(request.getMethod())) return true;
                if(!sessions.valid(request.getHeader("Authorization"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录知识工作台；服务重启后需重新登录");
                return true;
            }
        }).addPathPatterns("/api/workspace/**");
    }
}
