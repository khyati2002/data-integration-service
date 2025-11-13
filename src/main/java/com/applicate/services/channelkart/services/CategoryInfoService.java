package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.CategoryInfoRepository;
import com.applicate.services.channelkart.repository.UserParentRepository;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkCategoryInfoRecord;
import com.salescode.dim.jooq.impl.CategoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_CATEGORY_INFO;

public class CategoryInfoService extends AbstractCDMService<CategoryInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryInfoService.class);

    private static CategoryInfoRepository categoryInfoRepository;

    public CategoryInfoService(){
        if(categoryInfoRepository == null){
            categoryInfoRepository = new CategoryInfoRepository(getDslContext());
        }
    }

    public List<List<CategoryInfo>> getDataToSaveList(List<CategoryInfo> categoryInfoList) {
        List<List<CategoryInfo>> result = new ArrayList<>();
        List<String> categoryIds = categoryInfoList.stream()
                .map(CategoryInfo::getId)
                .collect(Collectors.toList());

        Map<String, CategoryInfo> savedList = getDslContext()
                .selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.ID.in(categoryIds))
                .fetch()
                .intoMap(CK_CATEGORY_INFO.ID, this::convertToCategoryInfo);

        List<CategoryInfo> itemsToInsert = new ArrayList<>();
        List<CategoryInfo> itemsToUpdate = new ArrayList<>();

        for (CategoryInfo category : categoryInfoList) {
            fillAttributes(category, savedList.get(category.getId()));
            fillCommonAttributes(category);

            if (category.getId() == null) {
                category.setId(new IdGenerator(category.getClass().getSimpleName()).getId(category));
            }

            if (savedList.get(category.getId()) == null) {
                itemsToInsert.add(category);
                category.setOperationPerformed(ActionType.INSERT);
                category.setActiveStatus(ActiveStatus.ACTIVE);
                category.setChanged(true);
            } else {
                category.setOperationPerformed(ActionType.UPDATE);
                category.setActiveStatus(ActiveStatus.ACTIVE);
                category.setChanged(true);
                itemsToUpdate.add(category);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private CategoryInfo convertToCategoryInfo(CkCategoryInfoRecord ckCategoryRecord) {
        CategoryInfo category = new CategoryInfo();
        category.setId(ckCategoryRecord.getId());
        category.setChanged(true);
        category.setActiveStatus(ckCategoryRecord.getActiveStatus());
        return category;
    }

    public List<String> findBatchCodesByCategoryFilters(Map<String, String> freeProductCategoryMap){
        return categoryInfoRepository.findBatchCodesByCategoryFilters(freeProductCategoryMap);
    }

    @Override
    public Collection<CategoryInfo> batchSave(Collection<CategoryInfo> categoryInfoList) {
        LOG.info("Size of list is {}", categoryInfoList.size());

        List<List<CategoryInfo>> saveItemsList = getDataToSaveList(new ArrayList<>(categoryInfoList));

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(saveItemsList.get(0).stream()
                    .map(category -> getDslContext().newRecord(CK_CATEGORY_INFO, category))
                    .collect(Collectors.toList())).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream()
                    .map(category -> getDslContext().newRecord(CK_CATEGORY_INFO, category))
                    .collect(Collectors.toList())).execute();
        }

        LOG.info("Batch save for category info is successful");
        return categoryInfoList;
    }
}