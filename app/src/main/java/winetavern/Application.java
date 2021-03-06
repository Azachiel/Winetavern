/*
 * Copyright 2014.2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package winetavern;

import org.salespointframework.EnableSalespoint;
import org.salespointframework.SalespointSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@EnableSalespoint
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    @Configuration
    static class WebSecurityConfiguration extends SalespointSecurityConfiguration {

        /**
         * Disabling Spring Security's CSRF support as we do not implement pre-flight request handling for the sake of
         * simplicity. Setting up basic security and defining login and logout options.
         *
         * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure(org.springframework.security.config.annotation.web.builders.HttpSecurity)
         */
        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.csrf().disable();

            http.authorizeRequests().
                    antMatchers("/accountancy/**").hasAnyRole("ADMIN, ACCOUNTANT").
                    antMatchers("/service/**").hasAnyRole("ADMIN, SERVICE").
                    antMatchers("/kitchen/**").hasAnyRole("ADMIN, COOK").
                    antMatchers("/admin/**").hasRole("ADMIN").
                    antMatchers("/*/**").authenticated().and().
                    formLogin().loginPage("/login").loginProcessingUrl("/login").permitAll().and().
                    logout().logoutUrl("/logout").logoutSuccessUrl("/login");
        }

    }

}
