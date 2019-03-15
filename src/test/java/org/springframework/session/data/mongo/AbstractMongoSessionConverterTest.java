/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.session.data.mongo;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.bson.Document;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.FindByIndexNameSessionRepository;

import com.mongodb.DBObject;

/**
 * @author Greg Turnquist
 */
public abstract class AbstractMongoSessionConverterTest {

	abstract AbstractMongoSessionConverter getMongoSessionConverter();

	@Test
	public void verifyRoundTripSerialization() throws Exception {

		// given
		MongoSession toSerialize = new MongoSession();
		toSerialize.setAttribute("username", "john_the_springer");

		// when
		DBObject dbObject = convertToDBObject(toSerialize);
		MongoSession deserialized = convertToSession(dbObject);

		// then
		assertThat(deserialized).isEqualToComparingFieldByField(toSerialize);
	}

	@Test
	public void verifyRoundTripSecuritySerialization() {

		// given
		MongoSession toSerialize = new MongoSession();
		String principalName = "john_the_springer";
		SecurityContextImpl context = new SecurityContextImpl();
		context.setAuthentication(
			new UsernamePasswordAuthenticationToken(principalName, null));
		toSerialize.setAttribute("SPRING_SECURITY_CONTEXT", context);

		// when
		DBObject serialized = convertToDBObject(toSerialize);
		MongoSession deserialized = convertToSession(serialized);

		// then
		assertThat(deserialized).isEqualToComparingOnlyGivenFields(toSerialize,
			"id", "createdMillis", "accessedMillis", "intervalSeconds", "expireAt");

		SecurityContextImpl springSecurityContextBefore = toSerialize.getAttribute("SPRING_SECURITY_CONTEXT");
		SecurityContextImpl springSecurityContextAfter = deserialized.getAttribute("SPRING_SECURITY_CONTEXT");

		assertThat(springSecurityContextBefore).isEqualToComparingOnlyGivenFields(springSecurityContextAfter,
			"authentication.principal", "authentication.authorities", "authentication.authenticated");
		assertThat(springSecurityContextAfter.getAuthentication().getPrincipal()).isEqualTo("john_the_springer");
		assertThat(springSecurityContextAfter.getAuthentication().getCredentials()).isNull();
	}

	@Test
	public void shouldExtractPrincipalNameFromAttributes() throws Exception {

		// given
		MongoSession toSerialize = new MongoSession();
		String principalName = "john_the_springer";
		toSerialize.setAttribute(
			FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
			principalName);

		// when
		DBObject dbObject = convertToDBObject(toSerialize);

		// then
		assertThat(dbObject.get("principal")).isEqualTo(principalName);
	}

	@Test
	public void shouldExtractPrincipalNameFromAuthentication() throws Exception {

		// given
		MongoSession toSerialize = new MongoSession();
		String principalName = "john_the_springer";
		SecurityContextImpl context = new SecurityContextImpl();
		context.setAuthentication(
			new UsernamePasswordAuthenticationToken(principalName, null));
		toSerialize.setAttribute("SPRING_SECURITY_CONTEXT", context);

		// when
		DBObject dbObject = convertToDBObject(toSerialize);

		// then
		assertThat(dbObject.get("principal")).isEqualTo(principalName);
	}

	@Test
	public void sessionWrapperWithNoMaxIntervalShouldFallbackToDefaultValues() {

		// given
		MongoSession toSerialize = new MongoSession();
		DBObject dbObject = convertToDBObject(toSerialize);
		Document document = new Document(dbObject.toMap());
		document.remove("interval");

		// when
		MongoSession convertedSession = getMongoSessionConverter().convert(document);

		// then
		assertThat(convertedSession.getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
	}

	MongoSession convertToSession(DBObject session) {
		return (MongoSession) getMongoSessionConverter().convert(session,
			TypeDescriptor.valueOf(DBObject.class),
			TypeDescriptor.valueOf(MongoSession.class));
	}

	DBObject convertToDBObject(MongoSession session) {
		return (DBObject) getMongoSessionConverter().convert(session,
			TypeDescriptor.valueOf(MongoSession.class),
			TypeDescriptor.valueOf(DBObject.class));
	}


}
