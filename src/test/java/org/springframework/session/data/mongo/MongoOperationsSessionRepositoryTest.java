/*
 * Copyright 2014-2017 the original author or authors.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.session.FindByIndexNameSessionRepository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Tests for {@link MongoOperationsSessionRepository}.
 *
 * @author Jakub Kubrynski
 * @author Vedran Pavic
 * @author Greg Turnquist
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoOperationsSessionRepositoryTest {

	@Mock
	private AbstractMongoSessionConverter converter;

	@Mock
	private MongoOperations mongoOperations;

	private MongoOperationsSessionRepository repository;

	@Before
	public void setUp() {
		
		this.repository = new MongoOperationsSessionRepository(this.mongoOperations);
		this.repository.setMongoSessionConverter(this.converter);
	}

	@Test
	public void shouldCreateSession() {

		// when
		MongoSession session = this.repository.createSession();

		// then
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isNew()).isTrue();
		assertThat(session.getMaxInactiveInterval().getSeconds())
				.isEqualTo(MongoOperationsSessionRepository.DEFAULT_INACTIVE_INTERVAL);
	}

	@Test
	public void shouldCreateSessionWhenMaxInactiveIntervalNotDefined() {

		// when
		this.repository.setMaxInactiveIntervalInSeconds(null);
		MongoSession session = this.repository.createSession();

		// then
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isNew()).isTrue();
		assertThat(session.getMaxInactiveInterval().getSeconds())
				.isEqualTo(MongoOperationsSessionRepository.DEFAULT_INACTIVE_INTERVAL);
	}

	@Test
	public void shouldSaveNewSession() {

		// given
		MongoSession session = new MongoSession();
		BasicDBObject dbSession = new BasicDBObject();

		given(this.converter.convert(session,
				TypeDescriptor.valueOf(MongoSession.class),
				TypeDescriptor.valueOf(DBObject.class))).willReturn(dbSession);
		
		// when
		this.repository.save(session);

		// then
		verify(this.mongoOperations).save(dbSession, MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME);

		assertThat(session.isNew()).isFalse();
	}

	@Test
	public void shouldHandleInvalidatedSession() {

		MongoSession session = new MongoSession();
		session.setNew(false);

		assertThatIllegalStateException().isThrownBy(() -> {
			this.repository.save(session);
		}).withMessage("Session was invalidated");
	}

	@Test
	public void shouldSaveExistingSession() {

		// given
		MongoSession session = new MongoSession();
		session.setNew(false);
		BasicDBObject dbSession = new BasicDBObject();

		Document sessionDocument = new Document();

		given(this.mongoOperations.findById(session.getId(), Document.class,
			MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME)).willReturn(sessionDocument);

		given(this.converter.convert(session,
			TypeDescriptor.valueOf(MongoSession.class),
			TypeDescriptor.valueOf(DBObject.class))).willReturn(dbSession);

		// when
		this.repository.save(session);

		// then
		verify(this.mongoOperations).save(dbSession, MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME);
	}

	@Test
	public void shouldGetSession() {

		// given
		String sessionId = UUID.randomUUID().toString();
		Document sessionDocument = new Document();

		given(this.mongoOperations.findById(sessionId, Document.class,
			MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME)).willReturn(sessionDocument);

		MongoSession session = new MongoSession();

		given(this.converter.convert(sessionDocument, TypeDescriptor.valueOf(Document.class),
				TypeDescriptor.valueOf(MongoSession.class))).willReturn(session);

		// when
		MongoSession retrievedSession = this.repository.findById(sessionId);

		// then
		assertThat(retrievedSession).isEqualTo(session);
	}

	@Test
	public void shouldHandleExpiredSession() {

		// given
		String sessionId = UUID.randomUUID().toString();
		Document sessionDocument = new Document();

		given(this.mongoOperations.findById(sessionId, Document.class,
			MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME)).willReturn(sessionDocument);

		MongoSession session = mock(MongoSession.class);

		given(session.isExpired()).willReturn(true);
		given(this.converter.convert(sessionDocument, TypeDescriptor.valueOf(Document.class),
			TypeDescriptor.valueOf(MongoSession.class))).willReturn(session);
		given(session.getId()).willReturn("sessionId");

		// when
		this.repository.findById(sessionId);

		// then
		verify(this.mongoOperations).remove(any(Document.class),
				eq(MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME));
	}

	@Test
	public void shouldDeleteSession() {

		// given
		String sessionId = UUID.randomUUID().toString();

		Document sessionDocument = new Document();
		sessionDocument.put("id", sessionId);

		MongoSession mongoSession = new MongoSession(sessionId, MongoOperationsSessionRepository.DEFAULT_INACTIVE_INTERVAL);

		given(this.converter.convert(sessionDocument, TypeDescriptor.valueOf(Document.class),
			TypeDescriptor.valueOf(MongoSession.class))).willReturn(mongoSession);
		given(this.mongoOperations.findById(eq(sessionId), eq(Document.class),
			eq(MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME))).willReturn(sessionDocument);

		// when
		this.repository.deleteById(sessionId);

		// then
		verify(this.mongoOperations).remove(any(Document.class),
			eq(MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME));
	}

	@Test
	public void shouldGetSessionsMapByPrincipal() {

		// given
		String principalNameIndexName = FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME;

		Document document = new Document();

		given(this.converter.getQueryForIndex(anyString(), any(Object.class))).willReturn(mock(Query.class));
		given(this.mongoOperations.find(any(Query.class), eq(Document.class),
				eq(MongoOperationsSessionRepository.DEFAULT_COLLECTION_NAME)))
						.willReturn(Collections.singletonList(document));

		String sessionId = UUID.randomUUID().toString();

		MongoSession session = new MongoSession(sessionId, 1800);

		given(this.converter.convert(document, TypeDescriptor.valueOf(Document.class),
				TypeDescriptor.valueOf(MongoSession.class))).willReturn(session);

		// when
		Map<String, MongoSession> sessionsMap =
			this.repository.findByIndexNameAndIndexValue(principalNameIndexName, "john");

		// then
		assertThat(sessionsMap).containsOnlyKeys(sessionId);
		assertThat(sessionsMap).containsValues(session);
	}

	@Test
	public void shouldReturnEmptyMapForNotSupportedIndex() {
		
		// given
		String index = "some_not_supported_index_name";

		// when
		Map<String, MongoSession> sessionsMap = this.repository
				.findByIndexNameAndIndexValue(index, "some_value");

		// then
		assertThat(sessionsMap).isEmpty();
	}
}
