/*
 * Copyright (C) 2017 University of South Florida
 * All rights reserved.
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
package edu.usf.cutr.gtfsrtvalidator.test.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;
import edu.usf.cutr.gtfsrtvalidator.test.FeedMessageTest;
import edu.usf.cutr.gtfsrtvalidator.test.util.TestUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.StopTimeUpdateValidator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.MIN_POSIX_TIME;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests for rules implemented in StopTimeUpdateValidator
 */
public class StopTimeUpdateValidatorTest extends FeedMessageTest {

    public StopTimeUpdateValidatorTest() throws Exception {
    }

    /**
     * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
     */
    @Test
    public void testE002() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        // tripDescriptor is a required field in tripUpdate, and we need schedule_relationship to avoid W009 warning
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        // ordered stop sequence 1, 5
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        /* Adding stop sequence 3. So, the stop sequence now is 1, 5, 3 which is unordered.
           So, the validation fails and the assertion test passes
        */
        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E002, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E036 - Sequential stop_time_updates have the same stop_sequence
     */
    @Test
    public void testE036() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequences 1, 5 - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_ids - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop sequence 5 twice (and to make sure we support it, no stopId). So, the stop sequence now is 1, 5, 5 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E036, 1);
        TestUtils.assertResults(expected, results);

        // stop_sequence 5 twice again, but include stop_id for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("3000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E036, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E037 - Sequential stop_time_updates have the same stop_id
     */
    @Test
    public void testE037() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_ids 1000, 2000 - no errors
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 2
        assertEquals(2, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_id 2000 twice (and to make sure we support it, no stop_sequence). So, repeating stop_ids 3000 - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E037, 1);
        TestUtils.assertResults(expected, results);

        // stop_id 2000 twice again, but include stop_sequence for last stop_time_update - one error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("2000");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 3
        assertEquals(3, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E037, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E040 - stop_time_update doesn't contain stop_id or stop_sequence
     */
    @Test
    public void testE40() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1234");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // No stop_id or stop_sequence - 1 error
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E040, 1);
        TestUtils.assertResults(expected, results);

        // Add stop_id but no stop_sequence - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence but no stop_id - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopSequence(1);
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // Add stop_sequence and stop_id - no errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clear();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E041 - trip doesn't have any stop_time_updates
     */
    @Test
    public void testE41() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // No stop_time_updates - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 0
        assertEquals(0, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E041, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update added - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // No stop_time_updates, but trip is CANCELED - 0 errors
        stopTimeUpdateBuilder.clear();
        tripUpdateBuilder.clearStopTimeUpdate();
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 0
        assertEquals(0, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E042 - arrival or departure provided for NO_DATA stop_time_update
     */
    @Test
    public void testE42() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update with schedule_relationship SCHEDULED and a departure - 0 errors
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship SCHEDULED and an arrival - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship NO_DATA and a departure - 1 error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E042, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update with schedule_relationship NO_DATA and an arrival - 1 error
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E042, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E043 - stop_time_update doesn't have arrival or departure
     */
    @Test
    public void testE43() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update without arrival or departure - 1 error
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E043, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival or departure, but schedule_relationship SKIPPED - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival or departure, but schedule_relationship NO_DATA - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopId("1.1");
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with arrival - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure - 0 errors
        stopTimeUpdateBuilder.clear();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E044 - stop_time_update arrival/departure doesn't have delay or time
     */
    @Test
    public void testE44() {
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // One stop_time_update with arrival delay - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with arrival time - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(MIN_POSIX_TIME).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure delay - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update with departure time - 0 errors
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(MIN_POSIX_TIME).build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // One stop_time_update without arrival time or delay - 1 error
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E044, 1);
        TestUtils.assertResults(expected, results);

        // One stop_time_update without departure time or delay - 1 error
        stopTimeUpdateBuilder.clearDeparture();
        stopTimeUpdateBuilder.clearArrival();
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopId("1.1");
        stopTimeUpdateBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().build());
        tripUpdateBuilder.clearStopTimeUpdate();
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
        // StopTimeUpdate count should be 1
        assertEquals(1, feedMessageBuilder.getEntity(0).getTripUpdate().getStopTimeUpdateCount());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, gtfsData, gtfsDataMetadata, feedMessageBuilder.build(), null);
        expected.put(E044, 1);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }

    /**
     * E045 - GTFS-rt stop_time_update stop_sequence and stop_id do not match GTFS
     */
    @Test
    public void testE45() {
        /**
         * bullrunner-gtfs.zip (bullRunnerGtfs) has the following in stop_times.txt:
         *
         * trip_id,arrival_time,departure_time,stop_id,stop_sequence
         * 1,07:00:00,07:00:00,222,1
         * 1,07:01:04,07:01:04,230,2
         * 1,07:01:38,07:01:38,214,3
         * 1,07:02:15,07:02:15,204,4
         * 1,07:02:56,07:02:56,102,5
         * 1,07:03:38,07:03:38,101,6
         * 1,07:04:04,07:04:04,108,7
         * 1,07:04:32,07:04:32,110,8
         * 1,07:05:38,07:05:38,166,9
         * 1,07:06:44,07:06:44,162,10
         * 1,07:07:48,07:07:48,158,11
         * 1,07:08:30,07:08:30,154,12
         * 1,07:09:20,07:09:20,150,13
         * 1,07:09:52,07:09:52,446,14
         * 1,07:11:01,07:11:01,432,15
         * 1,07:11:49,07:11:49,430,16
         * 1,07:12:34,07:12:34,426,17
         * 1,07:13:41,07:13:41,418,18
         * 1,07:14:34,07:14:34,401,19
         * 1,07:16:07,07:16:07,414,20
         * 1,07:16:53,07:16:53,330,21
         * 1,07:17:21,07:17:21,328,22
         * 1,07:17:59,07:17:59,326,23
         * 1,07:18:43,07:18:43,226,24
         * 1,07:19:43,07:19:43,222,25
         */
        StopTimeUpdateValidator stopSequenceValidator = new StopTimeUpdateValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();

        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
        GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1");
        tripDescriptorBuilder.setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);

        // tripDescriptor is a required field in tripUpdate
        tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());

        // stop_sequence and stop_id pairings all correctly match GTFS - no errors
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // first stop_sequence and stop_id pairing is wrong - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("204");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 1);
        TestUtils.assertResults(expected, results);

        // first two stop_sequence and stop_id pairings are wrong - 2 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("204");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("222"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // first and third stop_sequence and stop_id pairings are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("240"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("240"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // Third and fourth stop_sequence and stop_id pairings are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3); // Wrong
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("201"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence and stop_id pairings all correctly match GTFS - no errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("162");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.clear();
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence 10 is wrong - 1 error
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("160"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("222");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 1);
        TestUtils.assertResults(expected, results);

        // start at stop_sequence 2 - stop_sequence 10 and 25 are wrong - 2 errors
        tripUpdateBuilder.clearStopTimeUpdate();

        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setStopId("230");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(3);
        stopTimeUpdateBuilder.setStopId("214");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(4);
        stopTimeUpdateBuilder.setStopId("204");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(5);
        stopTimeUpdateBuilder.setStopId("102");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(6);
        stopTimeUpdateBuilder.setStopId("101");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(10);
        stopTimeUpdateBuilder.setStopId("160"); // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(12);
        stopTimeUpdateBuilder.setStopId("154");
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        stopTimeUpdateBuilder.setStopSequence(25);
        stopTimeUpdateBuilder.setStopId("101");  // Wrong
        stopTimeUpdateBuilder.setScheduleRelationship(GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(60).build());
        tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder.build());

        feedEntityBuilder.setTripUpdate(tripUpdateBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

        results = stopSequenceValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null);
        expected.put(E045, 2);
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields();
    }
}
