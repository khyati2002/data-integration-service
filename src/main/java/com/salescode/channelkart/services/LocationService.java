package com.salescode.channelkart.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.Location;
import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.models.enums.Sequence;
import com.salescode.channelkart.repository.LocationRepository;
import com.salescode.channelkart.utils.GlobalLock;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.channelkart.utils.StringUtils;
import org.apache.commons.beanutils.PropertyUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class LocationService extends AbstractCDMService<Location> {
    protected static final String delimiter = " > ";
    private static final String DOMAIN_NAME = "location";
    //	private static final String ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION = "Associate getter/setter method missing for one of the field from list[{}]. Exception:[{}]";
    private static final String DOMAIN_TYPE = "level";
    //
    private static final Object lock1 = new Object();

    private LocationRepository locationRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    //
    private MetaDataService metadataservice;
    private final SequenceInfoService sequenceInfoService;
    @Value("${location.column : area,pincode,territory,city,state,region,zone,cluster,branch,country}")
    private String locationColumns;

    @Autowired
    public LocationService(

            MetaDataService metadataservice,
            SequenceInfoService sequenceInfoService,
            LocationRepository locationRepository


    ) {
        super(locationRepository);
        this.metadataservice = metadataservice;
        this.sequenceInfoService = sequenceInfoService;
        this.locationRepository = locationRepository;


    }

    //


    public Location findByLocationHierarchy(String locationHierarchy) {
        return findByLocationHierarchy(locationHierarchy, true);
    }

    public Location findByLocationHierarchy(String locationHierarchy, boolean cached) {
        //String lob = SecurityContextUtils.getLob();
        Function<String, Location> function = (String locationHie) -> {
            LocationRepository repo = SpringContext.getBean(LocationRepository.class);
            return repo.findByLocationHierarchy(locationHie);
        };


        return function.apply(locationHierarchy);
    }


    public Location createNewLocationObj(Location locationObj, String[] locationColumns) {
        try {
            Location finalLocation = new Location();
            for (String locationName : locationColumns) {
                Object locationValue = PropertyUtils.getProperty(locationObj, locationName);
                if (NullUtils.isNotNull(locationValue)) {
                    PropertyUtils.setProperty(finalLocation, locationName, locationValue);
                }
                if (StringUtils.isEmpty(finalLocation.getLocationType()) && NullUtils.isNotNull(locationValue)) {
                    finalLocation.setLocationName(String.valueOf(locationValue));
                    finalLocation.setLocationType(locationName);
                }
            }
            String[] secondaryColumns = getLocationSecondaryColumns(finalLocation.getLocationType());
            if (secondaryColumns.length > 0) {
                for (String columnKey : secondaryColumns) {
                    Object columnValue = PropertyUtils.getProperty(locationObj, columnKey);
                    if (NullUtils.isNotNull(columnValue)) {
                        PropertyUtils.setProperty(finalLocation, columnKey, columnValue);
                    }
                }
            }
            return finalLocation;
        } catch (IllegalAccessException | InvocationTargetException e1) {

        } catch (NoSuchMethodException e2) {


        }
        return locationObj;
    }
    public String formHierarchyUsingColumns(Location location) {
        return formHierarchyUsingColumns(location,getLocationColumns(),delimiter);
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
                if (NullUtils.isNotNull(locationVal)) {
                    if (hierarchyStr.toString().equals("")) {
                        hierarchyStr.append(locationVal.toString());
                    } else {
                        hierarchyStr.append(delimiter + locationVal.toString());
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e1) {


        } catch (NoSuchMethodException e2) {


        }
        return hierarchyStr.toString();
    }


    public Location findLocationOrPersistLocation(Location dataObj) {
        if (NullUtils.isNotNull(dataObj)) {

            String[] columnList = getLocationColumns();
            Location tLocation = dataObj;
            String hierarchyStr = formHierarchyUsingColumns(tLocation, columnList, delimiter);
            if (StringUtils.isNotEmpty(hierarchyStr)) {
                Location locationRes = findByLocationHierarchy(hierarchyStr);
                if (locationRes != null) {
                    return locationRes;
                } else {
                    GlobalLock.withLock(hierarchyStr, k ->
                            saveRecursiveLocationHierarchies(tLocation, columnList)
                    );
                    Location locdata = findByLocationHierarchy(hierarchyStr, false);
                    return locdata;

                }

            } else {
                //throw new ResourceNotFoundException("Location hierarchy string found null");
            }

        } else {
            //throw new ResourceNotFoundException("No location object found.");
        }
        return dataObj;
    }

    //
    public ArrayNode convertToArrayNode(JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            // If it's already an ArrayNode, cast and return
            return (ArrayNode) jsonNode;
        } else {
            // Create a new ArrayNode and add the current JsonNode
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add(jsonNode);
            return arrayNode;
        }
    }

    public String[] getLocationColumns() {
        //String lob = SecurityContextUtils.getLob();

        MetaData metadata = metadataservice.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE, true);
        if (metadata == null) {

            return locationColumns.split(",");
        } else {
            List<Entry<String, JsonNode>> localdata = new ArrayList<>();
            ArrayNode arraynode = convertToArrayNode(metadata.getDomainValues());
            Iterator<JsonNode> iter = arraynode.elements();
            while (iter.hasNext()) {
                JsonNode node = iter.next();
                for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
                    localdata.add(iterator.next());
                }
            }
            Collections.sort(localdata, (Entry<String, JsonNode> o1, Entry<String, JsonNode> o2) -> Integer.compare(o1.getValue().intValue(), o2.getValue().intValue()));
            List<String> result = localdata.stream().map(Entry::getKey).collect(Collectors.toList());
            return result.toArray(new String[0]);
        }


    }



    public String[] getLocationSecondaryColumns(String key) {
        //	return distributedCache.withCache(SecurityContextUtils.getLob(), CACHE_DOMAIN, "LocationType" + key, ldata -> {
        MetaData metaData = metadataservice.fetchByValue(DOMAIN_NAME, "secondary_columns", true);
        ArrayNode columnNode = JSONUtils.getObjectMapper().createArrayNode();
        if (metaData != null && metaData.getDomainValues().get(0).has(key)) {
            columnNode = (ArrayNode) metaData.getDomainValues().get(0).get(key);
        }
        String[] columnArr = new String[columnNode.size()];
        for (int i = 0; i < columnNode.size(); i++) {
            columnArr[i] = columnNode.get(i).asText();
        }
        return columnArr;
        //	});
    }

    private Location saveRecursiveLocationHierarchies(final Location location, String[] columns) {
        Location result = null;
        for (int i = 0; i < columns.length; i++) {
            String[] columnsList= new String[columns.length-i];
            System.arraycopy(columns, i, columnsList, 0, columnsList.length);
            String hierarchyStr =formHierarchyUsingColumns(location, columnsList, delimiter);
            if(StringUtils.isNotBlank(hierarchyStr)) {
                synchronized (lock1) {
                    Location locdata = findByLocationHierarchy(hierarchyStr);
                    if(locdata == null) {
                        Location finalLocation = createNewLocationObj(location, columnsList);
                        finalLocation.setLocationHierarchy(hierarchyStr);
                        Location tresult = this.save(refresh(finalLocation));
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


    @Override
    public Location refresh(Location cdmObject){
        String[] columnList=getLocationColumns();
        String hierarchy=formHierarchyUsingColumns(cdmObject, columnList, delimiter);
        cdmObject = createLocationObj(cdmObject, columnList);
        cdmObject.setLocationHierarchy(hierarchy);
        cdmObject = setSalescodeId(cdmObject);
        return super.refresh(cdmObject);
    }

    public Location createLocationObj(Location locationObj,String[] locationColumns) {
        try {
            for(String locationName:locationColumns) {
                Object locationValue = PropertyUtils.getProperty(locationObj, locationName);
                if(NullUtils.isNotNull(locationValue)){
                    locationObj.setLocationName(String.valueOf(locationValue));
                    locationObj.setLocationType(locationName);
                    break;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e1) {
          //  throw new AccessException(e1.getCause(), ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,e1.getLocalizedMessage());
        } catch(NoSuchMethodException e2) {
         //   throw new UnknownKeyException(e2.getCause(), ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
          //          Arrays.toString(locationColumns),e2.getLocalizedMessage());
        }
        return locationObj;
    }

    public Location setSalescodeId(Location locationObj){
        if(StringUtils.isNullOrBlank(locationObj.getSalescodeId())) {
            locationObj.setSalescodeId(sequenceInfoService.generateSalescodeId(Sequence.LOCATION.getSequenceName()));
        }
        return locationObj;
    }





}
