/*
 * Copyright 2011-2013 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.event.collector;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.proofpoint.event.client.EventClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

@Path("/v2/event")
public class EventResource
{
    private final Set<EventWriter> writers;
    private final Set<String> acceptedEventTypes;
    private final ScheduledExecutorService executor;
    private final EventClient eventClient;
    private final MapMaker maker = new MapMaker();
    private ConcurrentMap<String, ProcessStats> stats = maker.makeMap();

    @Inject
    public EventResource(Set<EventWriter> writers, ServerConfig config, ScheduledExecutorService executor, EventClient eventClient)
    {
        Preconditions.checkNotNull(writers, "writer must not be null");
        this.writers = writers;
        this.acceptedEventTypes = ImmutableSet.copyOf(config.getAcceptedEventTypes());
        this.stats = Maps.newConcurrentMap();
        this.executor = executor;
        this.eventClient = eventClient;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(List<Event> events)
            throws IOException
    {
        Set<String> badEvents = Sets.newHashSet();
        for (Event event : events) {
            if (acceptedEventType(event.getType())) {
                for (EventWriter writer : writers) {
                    writer.write(event);
                }
                increment(event.getType());
            }
            else {
                badEvents.add(event.getType());
            }
        }
        if (!badEvents.isEmpty()) {
            String errorMessage = "Invalid event type(s): " + Joiner.on(", ").join(badEvents);
            return Response.status(Status.BAD_REQUEST).entity(errorMessage).build();

        }
        return Response.status(Response.Status.ACCEPTED).build();
    }

    private boolean acceptedEventType(String type)
    {
        return acceptedEventTypes.isEmpty() || acceptedEventTypes.contains(type);
    }

    private void increment(String eventType)
    {
        ProcessStats eventStats = stats.get(eventType);
        if (eventStats == null) {
            stats.putIfAbsent(eventType, new ProcessStats(executor, eventClient, eventType));
            eventStats = stats.get(eventType);
            eventStats.start();
        }
        eventStats.processed(1);
    }
}
