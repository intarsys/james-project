/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.james.jmap.api.vacation.Vacation;
import org.junit.Test;

public class VacationResponseTest {

    public static final String IDENTIFIER = "identifier";
    public static final String MESSAGE = "A message explaining I am in vacation";
    public static final String HTML_MESSAGE = "<p>A message explaining I am in vacation</p>";
    public static final ZonedDateTime FROM_DATE = ZonedDateTime.parse("2016-04-15T11:56:32.224+07:00[Asia/Vientiane]");
    public static final ZonedDateTime TO_DATE = ZonedDateTime.parse("2016-04-16T11:56:32.224+07:00[Asia/Vientiane]");
    public static final String SUBJECT = "subject";
    public static final ZonedDateTime ZONED_DATE_TIME_2014 = ZonedDateTime.parse("2014-09-30T14:10:00Z");
    public static final ZonedDateTime ZONED_DATE_TIME_2015 = ZonedDateTime.parse("2015-09-30T14:10:00Z");
    public static final ZonedDateTime ZONED_DATE_TIME_2016 = ZonedDateTime.parse("2016-09-30T14:10:00Z");

    @Test
    public void vacationResponseBuilderShouldBeConstructedWithTheRightInformation() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .id(IDENTIFIER)
            .enabled(true)
            .fromDate(Optional.of(FROM_DATE))
            .toDate(Optional.of(TO_DATE))
            .textBody(Optional.of(MESSAGE))
            .subject(Optional.of(SUBJECT))
            .htmlBody(Optional.of(HTML_MESSAGE))
            .build();

        assertThat(vacationResponse.getId()).isEqualTo(IDENTIFIER);
        assertThat(vacationResponse.isEnabled()).isEqualTo(true);
        assertThat(vacationResponse.getTextBody()).contains(MESSAGE);
        assertThat(vacationResponse.getHtmlBody()).contains(HTML_MESSAGE);
        assertThat(vacationResponse.getFromDate()).contains(FROM_DATE);
        assertThat(vacationResponse.getToDate()).contains(TO_DATE);
        assertThat(vacationResponse.getSubject()).contains(SUBJECT);
    }

    @Test
    public void vacationResponseBuilderRequiresABodyTextOrHtmlWhenEnabled() {
        assertThatThrownBy(() ->
            VacationResponse.builder()
                .id(IDENTIFIER)
                .enabled(true)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void vacationResponseBuilderShouldNotRequiresABodyTextOrHtmlWhenDisabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .id(IDENTIFIER)
            .enabled(false)
            .build();

        assertThat(vacationResponse.getId()).isEqualTo(IDENTIFIER);
        assertThat(vacationResponse.isEnabled()).isEqualTo(false);
        assertThat(vacationResponse.getTextBody()).isEmpty();
        assertThat(vacationResponse.getHtmlBody()).isEmpty();
    }

    @Test
    public void bodyTextShouldBeEnoughWhenEnabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .id(IDENTIFIER)
            .enabled(true)
            .textBody(Optional.of(MESSAGE))
            .build();

        assertThat(vacationResponse.getId()).isEqualTo(IDENTIFIER);
        assertThat(vacationResponse.isEnabled()).isEqualTo(true);
        assertThat(vacationResponse.getTextBody()).contains(MESSAGE);
        assertThat(vacationResponse.getHtmlBody()).isEmpty();
    }

    @Test
    public void htmlTextShouldBeEnoughWhenEnabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .id(IDENTIFIER)
            .enabled(true)
            .htmlBody(Optional.of(MESSAGE))
            .build();

        assertThat(vacationResponse.getId()).isEqualTo(IDENTIFIER);
        assertThat(vacationResponse.isEnabled()).isEqualTo(true);
        assertThat(vacationResponse.getTextBody()).isEmpty();
        assertThat(vacationResponse.getHtmlBody()).contains(MESSAGE);
    }

    @Test
    public void fromVacationShouldMarkOutDatedVacationAsDisabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .fromVacation(
                Vacation.builder()
                    .enabled(true)
                    .textBody("Any text")
                    .fromDate(Optional.of(ZONED_DATE_TIME_2014))
                    .toDate(Optional.of(ZONED_DATE_TIME_2015))
                    .build(),
                ZONED_DATE_TIME_2016)
            .build();

        assertThat(vacationResponse.isEnabled()).isFalse();
    }

    @Test
    public void fromVacationShouldMarkTooEarlyVacationAsDisabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .fromVacation(
                Vacation.builder()
                    .enabled(true)
                    .textBody("Any text")
                    .fromDate(Optional.of(ZONED_DATE_TIME_2015))
                    .toDate(Optional.of(ZONED_DATE_TIME_2016))
                    .build(),
                ZONED_DATE_TIME_2014)
            .build();

        assertThat(vacationResponse.isEnabled()).isFalse();
    }

    @Test
    public void fromVacationShouldMarkInRangeVacationAsEnabled() {
        VacationResponse vacationResponse = VacationResponse.builder()
            .fromVacation(
                Vacation.builder()
                    .enabled(true)
                    .textBody("Any text")
                    .fromDate(Optional.of(ZONED_DATE_TIME_2014))
                    .toDate(Optional.of(ZONED_DATE_TIME_2016))
                    .build(),
                ZONED_DATE_TIME_2015)
            .build();

        assertThat(vacationResponse.isEnabled()).isTrue();
    }

}
