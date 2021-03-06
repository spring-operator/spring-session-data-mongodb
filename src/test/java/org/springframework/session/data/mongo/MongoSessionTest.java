/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.session.data.mongo;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

/**
 * @author Rob Winch
 * @author Greg Turnquist
 */
public class MongoSessionTest {

	@Test
	public void isExpiredWhenIntervalNegativeThenFalse() {
		MongoSession session = new MongoSession();
		session.setMaxInactiveInterval(Duration.ofSeconds(-1));
		session.setLastAccessedTime(Instant.ofEpochMilli(0L));

		assertThat(session.isExpired()).isFalse();
	}
}
