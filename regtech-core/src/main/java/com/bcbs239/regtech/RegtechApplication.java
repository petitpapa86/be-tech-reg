package com.bcbs239.regtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.bcbs239.regtech.core",
    "com.bcbs239.regtech.iam",
    "com.bcbs239.regtech.billing"
})
@EnableTransactionManagement
@EnableAsync
@EnableAspectJAutoProxy
public class RegtechApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegtechApplication.class, args);
	}

}
