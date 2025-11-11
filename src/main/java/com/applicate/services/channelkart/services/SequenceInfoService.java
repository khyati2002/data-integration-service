package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.SequenceInfo;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.UUID;

import static com.salescode.dim.jooq.generated.Tables.CK_SEQUENCE_INFO;

public class SequenceInfoService extends AbstractCDMService<SequenceInfo> {

	public SequenceInfoService() {
		// Default constructor to align with your service style (no @Autowired / DI)
	}

	/**
	 * Checks whether sequence generator should be enabled
	 * for given entity & field combination.
	 */
	public boolean shouldEnableSequenceGenerator(String entityName, String fieldName, String data) {
		Record record = getDslContext()
				.select()
				.from(CK_SEQUENCE_INFO)
				.where(CK_SEQUENCE_INFO.ENTITY.eq(entityName))
				.and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
				.fetchOne();

		return record != null;
	}

	/**
	 * Retrieves and increments sequence number for given entity & field.
	 * Uses transactional update to avoid race conditions.
	 */
	public int getSequenceNumber(String entityName, String fieldName) {
		DSLContext dsl = getDslContext();

		return dsl.transactionResult(configuration -> {
			DSLContext ctx = DSL.using(configuration);

			Record record = ctx.selectFrom(CK_SEQUENCE_INFO)
					.where(CK_SEQUENCE_INFO.ENTITY.eq(entityName))
					.and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
					.fetchOne();

			if (record == null) {
				// Optional: Create a new entry automatically if not found
				SequenceInfo seqInfo = new SequenceInfo();
				seqInfo.setId(UUID.randomUUID().toString());
				seqInfo.setEntity(entityName);
				seqInfo.setFieldName(fieldName);
				seqInfo.setCurrentValue(1);
				seqInfo.setIncrementValue(1);
				ctx.insertInto(CK_SEQUENCE_INFO)
						.set(CK_SEQUENCE_INFO.ID, seqInfo.getId())
						.set(CK_SEQUENCE_INFO.ENTITY, seqInfo.getEntity())
						.set(CK_SEQUENCE_INFO.FIELD_NAME, seqInfo.getFieldName())
						.set(CK_SEQUENCE_INFO.CURRENT_VALUE, seqInfo.getCurrentValue())
						.set(CK_SEQUENCE_INFO.INCREMENT_VALUE, seqInfo.getIncrementValue())
						.execute();
				return seqInfo.getCurrentValue();
			}

			int currentValue = record.get(CK_SEQUENCE_INFO.CURRENT_VALUE);
			int increment = record.get(CK_SEQUENCE_INFO.INCREMENT_VALUE) != null
					? record.get(CK_SEQUENCE_INFO.INCREMENT_VALUE)
					: 1;
			int newValue = currentValue + increment;

			ctx.update(CK_SEQUENCE_INFO)
					.set(CK_SEQUENCE_INFO.CURRENT_VALUE, newValue)
					.where(CK_SEQUENCE_INFO.ENTITY.eq(entityName))
					.and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
					.execute();

			return newValue;
		});
	}
}
