package com.progweb.backend.configuracion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class ConfiguracionReloj {
    @Bean public Clock relojInicio(){return Clock.systemUTC();}
}
