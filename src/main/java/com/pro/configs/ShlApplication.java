package com.pro.configs;

import birzha.AunitImpls;
import birzha.AunitWsServiceImpl;
import com.pro.jdbc.WhoDAO;
import com.pro.resource.annotations.Table;
import com.zaxxer.hikari.HikariDataSource;
import generator.DDLgenerator;
import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.websocket.ClientEndpoint;
import java.util.Set;

@SpringBootApplication(scanBasePackages = {"com.pro"})
@EnableScheduling
@ClientEndpoint
public class ShlApplication extends SpringBootServletInitializer implements CommandLineRunner {

    RestTemplate restTemplate = new RestTemplate();

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ShlApplication.class); //test it
    }

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext run = SpringApplication.run(ShlApplication.class, args);
        WhoDAO bean = run.getBean(WhoDAO.class);
        System.out.println(bean);
    }


    @Bean
    public DataSource masterDataSource(){
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setUsername("root");
        hikariDataSource.setPassword("root");
        hikariDataSource.setJdbcUrl("jdbc:mysql://localhost:3306/prositutki");
        //todo
        return hikariDataSource;
    }

    @PostConstruct
    public void setUp(){
        Reflections reflections = new Reflections("com.pro.entity");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Table.class);
        DDLgenerator.process(typesAnnotatedWith);
    }


    public void run(String... args) throws Exception {



//        Date timestamp = new Date();
//
//        HmacSignature signature = new HmacSignature.Builder()
//                .algorithm("HmacSHA256")
//                .delimiter("|")
//                .apiSecret("AnL7vZlcfyIYAzLTrrQhWKigSmdlm7OHGwnTpXHb")
//                .endpoint("/openapi/v1/public/currency_pairs")
//                .requestMethod("GET")
//                .timestamp(timestamp.getTime())
//                .publicKey("dyFh1UGDZJomAhQahaE63WNoRjlRLS969IpO2ykE").build();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        headers.add("API-KEY", "dyFh1UGDZJomAhQahaE63WNoRjlRLS969IpO2ykE");
//        headers.add("API-TIME", String.valueOf(timestamp.getTime()));
//        headers.add("API-SIGN", signature.getSignatureHexString());
//
//        HttpEntity<String> entity = new HttpEntity<String>(new ObjectMapper().writeValueAsString(new CallbackURL("testedawdawrhttmp", 100)), headers);
//
//
//        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity("http://localhost:8079/openapi/v1/public/currency_pairs", entity, String.class);
//        String s = stringResponseEntity.toString();
//        System.out.println(stringResponseEntity.toString());
    }


}
