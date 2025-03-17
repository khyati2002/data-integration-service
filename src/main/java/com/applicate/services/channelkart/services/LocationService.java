package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.LocationRepository;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.records.CkLocationRecord;
import com.salescode.dim.jooq.impl.Location;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.kafka.common.errors.ResourceNotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_LOCATION;

public class LocationService extends AbstractCDMService<Location> {

    private static final String DOMAIN_NAME = "location";
    private static final String DOMAIN_TYPE = "level";
    private String locationColumns = "area,pincode,territory,city,state,region,zone,cluster,branch,country";
    protected static final String delimiter= " > ";
    private static final Object lock1 = new Object();

    private static MetaDataService metadataService;
    private static LocationRepository locationRepository;
    public LocationService(){
            metadataService = new MetaDataService();
            locationRepository = new LocationRepository(getDslContext());
    }
    public String[] getLocationColumns() {

        Metadata metadata = metadataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE);
        if (metadata == null) {
//				logger.warn("Location level config not found in metadata. Switching to default.");
            return locationColumns.split(",");
        } else {
            List<Map.Entry<String, JsonNode>> localdata = new ArrayList<>();
            JsonNode node = metadata.getDomainValues();
            ArrayNode arraynode = convertToArrayNode(node);
            Iterator<JsonNode> iter = arraynode.elements();
            while (iter.hasNext()) {
                JsonNode node1 = iter.next();
                for (Iterator<Map.Entry<String, JsonNode>> iterator = node1.fields(); iterator.hasNext(); ) {
                    localdata.add(iterator.next());
                }
            }
            Collections.sort(localdata, (Map.Entry<String, JsonNode> o1, Map.Entry<String, JsonNode> o2) -> Integer.compare(o1.getValue().intValue(), o2.getValue().intValue()));
            List<String> result = localdata.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            return result.toArray(new String[0]);
        }
    }

    public ArrayNode convertToArrayNode(JsonNode jsonNode) {
        ArrayNode arrayNode;
        if (jsonNode.isArray()) {
            // If it's already an ArrayNode, cast and return
            return (ArrayNode) jsonNode;
        } else {
            // Create a new ArrayNode and add the current JsonNode
            arrayNode = JsonNodeFactory.instance.arrayNode().add(jsonNode);
            return arrayNode;
        }
    }

    public String formHierarchyUsingColumns(Location location, String[] columnList,
                                            String delimiter) {
        StringBuilder hierarchyStr = new StringBuilder("");
        if (location == null) {
            //throw new ResourceNotFoundException("No location object found.");
        }
        try {
            for (String columnName : columnList) {
                Object locationVal = PropertyUtils.getProperty(location, columnName);
                if (locationVal!=null) {
                    if (hierarchyStr.toString().equals("")) {
                        hierarchyStr.append(locationVal.toString());
                    } else {
                        hierarchyStr.append(delimiter + locationVal.toString());
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e1) {
//			throw new AccessException(e1.getCause(),
//					ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,
//					e1.getLocalizedMessage());
        } catch (NoSuchMethodException e2) {
//			throw new UnknownKeyException(e2.getCause(),
//					ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
//					locationColumns, e2.getLocalizedMessage());
        }
        return hierarchyStr.toString();
    }

    public Map<String, Location> fetchSavedLocations(List<String> hierarchyList) {
        return getDslContext().selectFrom(CK_LOCATION)
                .where(CK_LOCATION.LOCATION_HIERARCHY.in(hierarchyList))
                .fetch()
                .intoMap(CK_LOCATION.LOCATION_HIERARCHY, record -> record.into(Location.class));

    }

    public List<Location> findLocationOrPersistLocation(List<Location> dataObj)  {
        List<String> locHierarchy = new ArrayList<>();
        List<Location> res = new ArrayList<>();
        String[] columnList = getLocationColumns();
        dataObj.forEach(loc -> {
                    Location tLocation = loc;
                    String hierarchyStr = formHierarchyUsingColumns(tLocation, columnList, delimiter);
                    locHierarchy.add(hierarchyStr);
                }
        );
        Map<String, Location> savedLocationMap = fetchSavedLocations(locHierarchy);
        for(int i=0;i<locHierarchy.size();i++) {
            String hierarchyStr = locHierarchy.get(i);
            if (StringUtils.isNotEmpty(hierarchyStr)) {
                Location locationRes = savedLocationMap.get(hierarchyStr);
                if (locationRes != null) {
                    res.add(locationRes);
                } else {
                    saveRecursiveLocationHierarchies(dataObj.get(i), columnList);
                    Location locdata = locationRepository.findByLocationHierarchy(hierarchyStr);
                    res.add(locdata);
                }

            } else {
                throw new ResourceNotFoundException("Location hierarchy string found null");
            }
        }
        return res;
    }

    private Location saveRecursiveLocationHierarchies(final Location location, String[] columns) {
        Location result = null;
        for (int i = 0; i < columns.length; i++) {
            String[] columnsList= new String[columns.length-i];
            System.arraycopy(columns, i, columnsList, 0, columnsList.length);
            String hierarchyStr =formHierarchyUsingColumns(location, columnsList, delimiter);
            if(StringUtils.isNotBlank(hierarchyStr)) {
                synchronized (lock1) {
                    Location locdata = locationRepository.findByLocationHierarchy(hierarchyStr);
                    if(locdata == null) {
                        Location finalLocation = createNewLocationObj(location, columnsList);
                        finalLocation.setLocationHierarchy(hierarchyStr);
                        Location tresult = save(finalLocation,refresh(finalLocation));
                        if(i == 0) {
                            result= tresult;
                        }
                    }else {
                        if(i == 0) {
                            result= locdata;
                        }
                    }
                }
            }
        }
        return result;
    }

    public Location refresh(Location loc){
        Location savedLoc = getDslContext().selectFrom(CK_LOCATION)
                .where(CK_LOCATION.LOCATION_HIERARCHY.eq(loc.getLocationHierarchy()))
                .fetchOneInto(Location.class);

        return savedLoc;
    }


    public Location createNewLocationObj(Location locationObj,String[] locationColumns) {
        try {
            Location finalLocation = new Location();
            for(String locationName:locationColumns) {
                Object locationValue = PropertyUtils.getProperty(locationObj, locationName);
                if(locationValue != null){
                    PropertyUtils.setProperty(finalLocation, locationName, locationValue );
                }
                if( StringUtils.isEmpty(finalLocation.getLocationType()) && locationValue!=null){
                    finalLocation.setLocationName(String.valueOf(locationValue));
                    finalLocation.setLocationType(locationName);
                }
            }
            String[] secondaryColumns = getLocationSecondaryColumns(finalLocation.getLocationType());
            if (secondaryColumns.length > 0) {
                for (String columnKey : secondaryColumns) {
                    Object columnValue = PropertyUtils.getProperty(locationObj, columnKey);
                    if (columnValue!=null) {
                        PropertyUtils.setProperty(finalLocation, columnKey, columnValue);
                    }
                }
            }
            return finalLocation;
        } catch (IllegalAccessException | InvocationTargetException e1) {
            //   throw new AccessException(e1.getCause(), ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,e1.getLocalizedMessage());
        } catch(NoSuchMethodException e2) {
            //  throw new UnknownKeyException(e2.getCause(), ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
            //          Arrays.toString(locationColumns),e2.getLocalizedMessage());
        }
        return locationObj;
    }

    public String[] getLocationSecondaryColumns(String key) {
        Metadata metaData = metadataService.fetchByValue(DOMAIN_NAME, "secondary_columns");
        ArrayNode columnNode = JSONUtils.getObjectMapper().createArrayNode();
        if (metaData != null && metaData.getDomainValues().get(0).has(key)) {
            columnNode = (ArrayNode) metaData.getDomainValues().get(0).get(key);
        }
        String[] columnArr = new String[columnNode.size()];
        for (int i = 0; i < columnNode.size(); i++) {
            columnArr[i] = columnNode.get(i).asText();
        }
        return columnArr;
    }

    public Location save(Location loc,com.salescode.dim.jooq.generated.tables.pojos.Location  savedLoc){
        super.addHash(loc);
        if(Objects.equals(loc.getHash(), savedLoc.getHash())){
            return loc;
        }
        if(savedLoc != null) {
            loc.setId(savedLoc.getId());
            loc.setVersion(savedLoc.getVersion() + 1);
            loc.setChanges(CdmDiffUtil.getChanges(loc,Location.of(savedLoc)));
        }
        else{
            loc.setId(UUID.randomUUID().toString());
            loc.setVersion(0);
        }
        CkLocationRecord record = getDslContext().newRecord(CK_LOCATION,loc);
        getDslContext().insertInto(CK_LOCATION)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();

        return loc;
    }

}
