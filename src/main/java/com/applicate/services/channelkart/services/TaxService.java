package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkTaxRecord;
import com.salescode.dim.jooq.impl.Tax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_TAX;

public class TaxService extends AbstractCDMService<Tax> {
    private static final Logger LOG = LoggerFactory.getLogger(TaxService.class);

    public TaxService() {
    }

    public List<List<Tax>> getDataToSaveList(List<Tax> taxList) {
        List<List<Tax>> result = new ArrayList<>();

        List<String> taxIds = taxList.stream().map(Tax::getId).collect(Collectors.toList());

        Map<String, Tax> savedList = getDslContext().selectFrom(CK_TAX).where(CK_TAX.ID.in(taxIds)).fetch().intoMap(CK_TAX.ID, this::convertToTax);

        List<Tax> itemsToInsert = new ArrayList<>();
        List<Tax> itemsToUpdate = new ArrayList<>();

        for (Tax tax : taxList) {

            fillAttributes(tax, savedList.get(tax.getId()));
            fillCommonAttributes(tax);

            if (tax.getId() == null) {
                tax.setId(new IdGenerator(tax.getClass().getSimpleName()).getId(tax));
            }

            if (savedList.get(tax.getId()) == null) {
                itemsToInsert.add(tax);
                tax.setOperationPerformed(ActionType.INSERT);
                tax.setActiveStatus(ActiveStatus.ACTIVE);
                tax.setChanged(true);
            } else {
                itemsToUpdate.add(tax);
                tax.setOperationPerformed(ActionType.UPDATE);
                tax.setActiveStatus(ActiveStatus.ACTIVE);
                tax.setChanged(true);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private Tax convertToTax(CkTaxRecord taxRecord) {
        Tax tax = new Tax();
        tax.setId(taxRecord.getId());
        tax.setChanged(true);
        tax.setActiveStatus(taxRecord.getActiveStatus());
        return tax;
    }

    @Override
    public Collection<Tax> batchSave(Collection<Tax> taxList) {
        LOG.info("Size of Tax list: {}", taxList.size());

        List<List<Tax>> saveItemsList = getDataToSaveList(new ArrayList<>(taxList));

        // INSERT
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(saveItemsList.get(0).stream().map(tax -> getDslContext().newRecord(CK_TAX, tax)).collect(Collectors.toList())).execute();
        }

        // UPDATE
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream().map(tax -> getDslContext().newRecord(CK_TAX, tax)).collect(Collectors.toList())).execute();
        }

        LOG.info("Batch save for Tax is successful");
        return taxList;
    }
}