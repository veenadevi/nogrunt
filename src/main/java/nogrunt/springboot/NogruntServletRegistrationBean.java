package nogrunt.springboot;

import nogrunt.*;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NogruntServletRegistrationBean {
    @Bean
    public ServletRegistrationBean<ReactApp> myServlet() {
        return new ServletRegistrationBean<>(new ReactApp(), "/existing-servlet");
    }
}

