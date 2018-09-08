package com.ali.controller;

import com.ali.domain.SysLog;
import com.ali.service.SysLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;

@Component
@Aspect
public class LogAop {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private SysLogService sysLogService;

    private Date visitTime;//开始时间
    private Class clazz;//访问的类
    private Method method;//访问的方法

    //前置通知   开始时间，执行方法，执行类
    @Before("execution(* com.ali.controller.*.*(..))")
    public void doBefore(JoinPoint jp) throws NoSuchMethodException {

        visitTime = new Date();// 访问时间
        clazz = jp.getTarget().getClass();// 获取访问的类
        String methodName = jp.getSignature().getName();// 获取访问的方法的名称

        Object[] args = jp.getArgs();// 获取访问的方法的参数
        if (args == null || args.length == 0) {
            method = clazz.getMethod(methodName);// 只能获取无参数方法
        } else {
            // 有参数，就将args中所有元素遍历，获取对应的Class,装入到一个Class[]
            Class[] classeArgs = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                classeArgs[i] = args[i].getClass();
            }
            method = clazz.getMethod(methodName, classeArgs);
        }
    }

    //后置通知
    @After("execution(* com.ali.controller.*.*(..))")
    public void doAfter(JoinPoint jp) {
        long time = new Date().getTime() - visitTime.getTime();

        String url = "";
        if (clazz != null && method != null && clazz != LogAop.class) {
            RequestMapping clazzAnnotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
            if (clazzAnnotation != null) {
                String[] classValues = clazzAnnotation.value();
                RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
                if (methodAnnotation != null) {
                    String[] methodalue = methodAnnotation.value();
                    url = classValues[0] + methodalue[0];

                    //获取ip
                    String ip = request.getRemoteAddr();

                    //获取操作者
                    SecurityContext context = SecurityContextHolder.getContext();
                    User user = (User) context.getAuthentication().getPrincipal();
                    String username = user.getUsername();

                    //将sysLog对象属性封装
                    SysLog sysLog = new SysLog();
                    sysLog.setExecutionTime(time);
                    sysLog.setIp(ip);
                    sysLog.setMethod("[类名] " + clazz.getName() + "[方法名]" + method.getName());
                    sysLog.setUrl(url);
                    sysLog.setUsername(username);
                    sysLog.setVisitTime(visitTime);

                    sysLogService.save(sysLog);
                }
            }
        }


    }
}
