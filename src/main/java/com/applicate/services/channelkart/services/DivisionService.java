package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.DivisionRepository;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.Division;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DivisionService extends AbstractCDMService<Division> {


    private static DivisionRepository divisionRepository;
    public DivisionService(){

        divisionRepository = new DivisionRepository(getDslContext());

    }

    public Collection<Division> findByChannelDivisionOrderByLevelAsc() {

            Collection<Division> data = divisionRepository.findByChannelDivisionOrderByLevelAsc(true);
            if (data == null || data.isEmpty()) {
                return null;
            }
            return data;
    }



    public boolean isChannelDivisionPresent() {
        List<Division> divisions = (List<Division>) findByChannelDivisionOrderByLevelAsc();
        if (divisions == null || divisions.isEmpty()) {
           // logger.error("Channel division not found. Please ensure division data are present.");
            return false;
        }
        return true;
    }

    public boolean isChannelDivision(String divisionName) {
        List<Division> divisions = (List<Division>) findByChannelDivisionOrderByLevelAsc();
        if (divisions == null || divisions.isEmpty()) {
            throw new RuntimeException(
                    "Channel division not found. [Hint : Make sure division data present in database]");
        }
        Optional<Division> division = divisions.parallelStream().filter(
                        element -> element.getDivisionName().equalsIgnoreCase(divisionName))
                .findAny();
        return division.isPresent();
    }

    public List<Division> findByDivisionName(String divisionName) {
        Collection<Division> divisions = findAllOrderByLevelAsc(true);
        Function<String, List<Division>> function = division -> divisions.stream()
                .filter(p -> p.getDivisionName().equalsIgnoreCase(division)).collect(Collectors.toList());

        return (ObjectUtils.isNotEmpty(divisions)) ? function.apply(divisionName) : List.of();
    }

    public Collection<Division> findAllOrderByLevelAsc(boolean cache) {

        return divisionRepository.findByOrderByLevelAsc();
    }

    public List<AuthRole> findRolesByDivisionId(String divisionId) {
        return divisionRepository.findRolesByDivisionId(divisionId);
    }


}
