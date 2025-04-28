package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.repository.MetaDataRepository;
import com.applicate.services.channelkart.repository.RedeemActivityRepository;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails;
import com.salescode.dim.jooq.generated.tables.records.CkScoreDetailsRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SCORE_DETAILS;
import static org.reflections.Reflections.log;

public class ScoreDetailsService extends AbstractCDMService<ScoreDetails> {
	private static final Logger LOG = LoggerFactory.getLogger(ScoreDetailsService.class);

	private static RedeemActivityRepository redeemActivityRepository;

	public ScoreDetailsService() {
		if(redeemActivityRepository == null) {
			redeemActivityRepository = new  RedeemActivityRepository(getDslContext());
		}
	}

	public List<List<ScoreDetails>> getDataToSaveList(List<ScoreDetails> scoreDetailsList) {
		List<List<ScoreDetails>> result = new ArrayList<>();

		if (scoreDetailsList == null || scoreDetailsList.isEmpty()) {
			result.add(new ArrayList<>());
			result.add(new ArrayList<>());
			return result;
		}

		List<String> scoreId = scoreDetailsList.stream().map(ScoreDetails::getId).collect(Collectors.toList());

		Map<String, ScoreDetails> savedList = getDslContext().selectFrom(CK_SCORE_DETAILS).where(CK_SCORE_DETAILS.PROGRAM_NUMBER.in(scoreId)).fetch().intoMap(CK_SCORE_DETAILS.ID, record -> record.into(ScoreDetails.class));

		List<ScoreDetails> itemsToInsert = new ArrayList<>();
		List<ScoreDetails> itemsToUpdate = new ArrayList<>();

		for (ScoreDetails outlet : scoreDetailsList) {
			fillAttributes(outlet, savedList.get(outlet.getId()));
			fillCommonAttributes(outlet);

			if (savedList.get(outlet.getId()) == null) {
				outlet.setVersion(0);
				outlet.setOperationPerformed(ActionType.INSERT);
				itemsToInsert.add(outlet);
			} else {
				ScoreDetails existingOutlet = savedList.get(outlet.getId());
				outlet.setVersion(existingOutlet.getVersion() + 1);
				outlet.setOperationPerformed(ActionType.UPDATE);
				itemsToUpdate.add(outlet);
			}
		}

		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}
	public boolean pendingStatusValidator(ScoreDetails score, List<String> errors) {
		try {
			if (!redeemActivityRepository.redeemActivityStatus().isEmpty()) {
				String errorstr = StringUtils.format("Please first change pending status for historical data");
			//	AuditLogger.log("Pending Status Validation Error","<<< " + errorstr + " >>> for score >>> "+score, AuditLogger.Status.FAILURE, "Pending Status Validation", AuditLogger.Operations.GET.toString(), score.toString());
				errors.add(errorstr);
				return true;
			}
		}
		catch (RuntimeException e) {
			LOG.error("ERROR");
		}
		return false;
	}


	@Override
	public Collection<ScoreDetails> batchSave(Collection<ScoreDetails> scoreDetailsList) {
		LOG.info("Size of list is {}", scoreDetailsList.size());

		List<List<ScoreDetails>> saveItemsList = getDataToSaveList(new ArrayList<>(scoreDetailsList));

		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(outlet -> getDslContext().newRecord(CK_SCORE_DETAILS, outlet)).collect(Collectors.toList())).execute();
		}

		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(outlet -> getDslContext().newRecord(CK_SCORE_DETAILS, outlet)).collect(Collectors.toList())).execute();
		}

		LOG.info("Batch save is successful");
		return scoreDetailsList;
	}
}
