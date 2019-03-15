/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.session.data.mongo.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.Map;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.AbstractMongoSessionConverter;
import org.springframework.session.data.mongo.JdkMongoSessionConverter;
import org.springframework.session.data.mongo.MongoSession;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link org.springframework.session.data.mongo.MongoOperationsSessionRepository} that use
 * {@link JdkMongoSessionConverter} based session serialization.
 *
 * @author Jakub Kubrynski
 * @author Vedran Pavic
 * @author Greg Turnquist
 */
@ContextConfiguration
public class MongoRepositoryJdkSerializationITest extends AbstractMongoRepositoryITest {

	@Test
	public void findByDeletedSecurityPrincipalNameReload() throws Exception {

		MongoSession toSave = this.repository.createSession();
		toSave.setAttribute(SPRING_SECURITY_CONTEXT, this.context);

		this.repository.save(toSave);

		MongoSession getSession = this.repository.findById(toSave.getId());
		getSession.setAttribute(INDEX_NAME, null);
		this.repository.save(getSession);

		Map<String, MongoSession> findByPrincipalName = this.repository.findByIndexNameAndIndexValue(INDEX_NAME,
				getChangedSecurityName());

		assertThat(findByPrincipalName).isEmpty();
	}

	@Test
	public void findByPrincipalNameNoSecurityPrincipalNameChangeReload() throws Exception {

		MongoSession toSave = this.repository.createSession();
		toSave.setAttribute(SPRING_SECURITY_CONTEXT, this.context);

		this.repository.save(toSave);

		toSave = this.repository.findById(toSave.getId());

		toSave.setAttribute("other", "value");
		this.repository.save(toSave);

		Map<String, MongoSession> findByPrincipalName = this.repository.findByIndexNameAndIndexValue(INDEX_NAME,
				getSecurityName());

		assertThat(findByPrincipalName).hasSize(1);
		assertThat(findByPrincipalName.keySet()).containsOnly(toSave.getId());
	}

	// tag::sample[]
	@Configuration
	@EnableMongoHttpSession
	static class Config extends BaseConfig {

		@Bean
		public AbstractMongoSessionConverter mongoSessionConverter() {
			return new JdkMongoSessionConverter(Duration.ofMinutes(30));
		}
	}
	// end::sample[]
}
