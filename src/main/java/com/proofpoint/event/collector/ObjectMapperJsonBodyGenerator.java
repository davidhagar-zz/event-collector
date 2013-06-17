package com.proofpoint.event.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofpoint.http.client.BodyGenerator;

import java.io.OutputStream;
import java.util.List;

public class ObjectMapperJsonBodyGenerator implements BodyGenerator
{
    private final byte[] json;

    public ObjectMapperJsonBodyGenerator(ObjectMapper mapper, List<Event> events)
            throws JsonProcessingException
    {
        this.json = mapper.writeValueAsBytes(events);
    }

    @Override
    public void write(OutputStream out)
            throws Exception
    {
        out.write(json);
    }
}
