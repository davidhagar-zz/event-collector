package com.proofpoint.collector.calligraphus.combiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.proofpoint.collector.calligraphus.combiner.S3StorageHelper.appendSuffix;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

public class CombinedGroup
{
    public static CombinedGroup createInitialCombinedGroup(URI locationPrefix, String creator)
    {
        checkNotNull(locationPrefix, "locationPrefix is null");
        checkNotNull(creator, "creator is null");
        return new CombinedGroup(locationPrefix, 0, creator, currentTimeMillis(), ImmutableList.<CombinedStoredObject>of());
    }

    private final URI locationPrefix;
    private final long version;
    private final String creator;
    private final long updatedTimestamp;
    private final List<CombinedStoredObject> combinedObjects;

    @JsonCreator
    public CombinedGroup(
            @JsonProperty("locationPrefix") URI locationPrefix,
            @JsonProperty("version") long version,
            @JsonProperty("creator") String creator,
            @JsonProperty("updatedTimestamp") long updatedTimestamp,
            @JsonProperty("combinedObjects") List<CombinedStoredObject> combinedObjects)
    {
        this.locationPrefix = locationPrefix;
        this.version = version;
        this.creator = creator;
        this.updatedTimestamp = updatedTimestamp;
        this.combinedObjects = combinedObjects;
    }

    @JsonProperty
    public URI getLocationPrefix()
    {
        return locationPrefix;
    }

    @JsonProperty
    public long getVersion()
    {
        return version;
    }

    @JsonProperty
    public String getCreator()
    {
        return creator;
    }

    @JsonProperty
    public long getUpdatedTimestamp()
    {
        return updatedTimestamp;
    }

    @JsonProperty
    public List<CombinedStoredObject> getCombinedObjects()
    {
        return combinedObjects;
    }

    public CombinedStoredObject getCombinedObject(URI location) {
        for (CombinedStoredObject combinedObject : combinedObjects) {
            if (location.equals(combinedObject.getLocation())) {
                return combinedObject;
            }
        }
        return null;
    }

    public boolean isCombinedObjectNewOrChanged(CombinedStoredObject newObject)
    {
        CombinedStoredObject object = getCombinedObject(newObject.getLocation());
        return (object == null) || !object.getSourceParts().equals(newObject.getSourceParts());
    }

    public CombinedGroup update(String creator, List<CombinedStoredObject> combinedObjects)
    {
        checkNotNull(creator, "creator is null");
        checkNotNull(combinedObjects, "combinedObjects is null");
        return new CombinedGroup(locationPrefix, version + 1, creator, currentTimeMillis(), combinedObjects);
    }

    public CombinedGroup updateCombinedObject(String creator, CombinedStoredObject combinedObject)
    {
        checkNotNull(creator, "creator is null");
        checkNotNull(combinedObject, "combinedObject is null");
        List<CombinedStoredObject> newList = Lists.newArrayList();
        boolean found = false;
        for (CombinedStoredObject object : combinedObjects) {
            if (combinedObject.getLocation().equals(object.getLocation())) {
                found = true;
                object = combinedObject;
            }
            newList.add(object);
        }
        if (!found) {
            throw new IllegalArgumentException("combinedObjects does not contain object to update");
        }
        return update(creator, newList);
    }

    public CombinedGroup addNewCombinedObject(String creator, URI baseURI, List<StoredObject> parts)
    {
        checkNotNull(creator, "creator is null");
        checkNotNull(baseURI, "baseURI is null");
        checkNotNull(parts, "parts is null");
        URI location = appendSuffix(baseURI, format("%05d.json.snappy", combinedObjects.size()));
        CombinedStoredObject newObject = new CombinedStoredObject(location, currentTimeMillis(), parts);
        return update(creator, concat(combinedObjects, newObject));
    }

    private static <T> ImmutableList<T> concat(Iterable<T> base, T item)
    {
        return ImmutableList.<T>builder().addAll(base).add(item).build();
    }
}
