package com.proofpoint.event.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.proofpoint.event.collector.FilteringMapSerializer.MapFilter;
import com.proofpoint.json.JsonCodec;
import com.proofpoint.json.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestFilteringMapSerializer
{
    private final JsonCodec<Event> eventCodec = JsonCodec.jsonCodec(Event.class);
    private final String uuid = UUID.randomUUID().toString();
    private final DateTime now = DateTime.now();

    @Test
    public void testSimpleFiltering()
            throws JsonProcessingException
    {
        ObjectMapper mapper = getMapper(new FilteringMapSerializer(ImmutableList.of(new MapFilter("data", ImmutableSet.of("key1", "key2")))));
        Event event = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                        "key2", "value2", "key3", ImmutableMap.of("subkey1", "subvalue2"), "key4", ImmutableList.of("listElement1", "listElement2")));
        Event filteredEvent1 = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                "key2", "value2"));

        String fromMyObjectMapper = mapper.writeValueAsString(event);

        Event eventFromFilteredString = eventCodec.fromJson(fromMyObjectMapper);
        assertEquals(eventFromFilteredString, filteredEvent1);
    }

    @Test
    public void testExtraInapplicableFilters()
            throws JsonProcessingException
    {
        ObjectMapper mapper = getMapper(new FilteringMapSerializer(ImmutableList.of(new MapFilter("data", ImmutableSet.of("key1", "key2", "key15")))));
        Event event = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                "key2", "value2", "key3", ImmutableMap.of("subkey1", "subvalue2"), "key4", ImmutableList.of("listElement1", "listElement2")));
        Event filteredEvent1 = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                "key2", "value2"));

        String fromMyObjectMapper = mapper.writeValueAsString(event);

        Event eventFromFilteredString = eventCodec.fromJson(fromMyObjectMapper);
        assertEquals(eventFromFilteredString, filteredEvent1);
    }

    @Test
    public void testNoFilters()
            throws JsonProcessingException
    {
        ObjectMapper mapper = getMapper(new FilteringMapSerializer(ImmutableList.<MapFilter>of()));
        Event event = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                "key2", "value2", "key3", ImmutableMap.of("subkey1", "subvalue2"), "key4", ImmutableList.of("listElement1", "listElement2")));

        String fromMyObjectMapper = mapper.writeValueAsString(event);

        Event eventFromFilteredString = eventCodec.fromJson(fromMyObjectMapper);
        assertEquals(eventFromFilteredString, event);
    }

    @Test
    public void testSubstructuresAreNotFiltered()
            throws JsonProcessingException
    {
        ObjectMapper mapper = getMapper(new FilteringMapSerializer(ImmutableList.of(new MapFilter("data", ImmutableSet.of("key3", "key4", "subkey1", "listElement1")))));
        Event event = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key1", "value1",
                "key2", "value2", "key3", ImmutableMap.of("subkey1", "subvalue2"), "key4", ImmutableList.of("listElement1", "listElement2")));
        Event filteredEvent1 = new Event("TestEvent", uuid, "localhost", now, ImmutableMap.of("key3", ImmutableMap.of("subkey1", "subvalue2"), "key4", ImmutableList.of("listElement1", "listElement2")));

        String fromMyObjectMapper = mapper.writeValueAsString(event);

        Event eventFromFilteredString = eventCodec.fromJson(fromMyObjectMapper);
        assertEquals(eventFromFilteredString, filteredEvent1);
    }

    private ObjectMapper getMapper(FilteringMapSerializer filteringMapSerializer)
    {
        SimpleModule testModule = new SimpleModule("FilteringEventModule", Version.unknownVersion());
        testModule.addSerializer(filteringMapSerializer);
        ObjectMapper mapper =  new ObjectMapperProvider().get();
        mapper.registerModule(testModule);
        return mapper;
    }
}