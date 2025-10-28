package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.converters.JSONObjectConverter;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;


import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class SalesDetails extends com.salescode.dim.jooq.generated.tables.pojos.SalesDetails implements Serializable {
	private static final long serialVersionUID = 1L;

	private String batchCode;

    private JsonNode extendedAttributes;

	private String skuCode;
	
    @Column(name="sale_id")
	private String invoiceNumber;
	
	@Transient
	private ProductDetails productDetails;
	
	private float price;
	
	private float pieceQuantity;
	
	private float otherUnitQuantity;
	/**
	 *  initial piece quantity
	 */
	private float initialQuantity;
	
	private float initialPieceQuantity;
	
	private float initialCaseQuantity;
	
	private float initialOtherUnitQuantity;

	private float caseQuantity;
	
	private String quantityUnit;

	private double nw;

	private float normalizedQuantity;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float otherUnitPrice;

	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float casePrice;
	
	@Column(columnDefinition = "FLOAT NOT NULL DEFAULT 0")
	private float initialNormalizedQuantity;
	
	@Column(columnDefinition = "json")
	@Convert(converter= JSONObjectConverter.class)
	private JsonNode productInfo;

	@Column(columnDefinition = "json", name = "discount_info")
	@Convert(converter = JSONObjectConverter.class)
	private JsonNode discountInfo;





	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SalesDetails that = (SalesDetails) o;
		return Float.compare(that.price, price) == 0 &&
				Float.compare(that.pieceQuantity, pieceQuantity) == 0 &&
				Float.compare(that.otherUnitQuantity, otherUnitQuantity) == 0 &&
				Float.compare(that.initialQuantity, initialQuantity) == 0 &&
				Float.compare(that.initialPieceQuantity, initialPieceQuantity) == 0 &&
				Float.compare(that.initialCaseQuantity, initialCaseQuantity) == 0 &&
				Float.compare(that.initialOtherUnitQuantity, initialOtherUnitQuantity) == 0 &&
				Float.compare(that.caseQuantity, caseQuantity) == 0 &&
				Float.compare(that.normalizedQuantity, normalizedQuantity) == 0 &&
				Float.compare(that.otherUnitPrice, otherUnitPrice) == 0 &&
				Float.compare(that.casePrice, casePrice) == 0 &&
				Float.compare(that.initialNormalizedQuantity, initialNormalizedQuantity) == 0 &&
				Objects.equals(batchCode, that.batchCode) &&
				Objects.equals(skuCode, that.skuCode) &&
				Objects.equals(nw, that.nw) &&
				Objects.equals(invoiceNumber, that.invoiceNumber) &&
				Objects.equals(quantityUnit, that.quantityUnit) &&
				Objects.equals(productInfo, that.productInfo) &&
				Objects.equals(discountInfo, that.discountInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				nw,
				batchCode,
				skuCode,
				invoiceNumber,
				price,
				pieceQuantity,
				otherUnitQuantity,
				initialQuantity,
				initialPieceQuantity,
				initialCaseQuantity,
				initialOtherUnitQuantity,
				caseQuantity,
				quantityUnit,
				normalizedQuantity,
				otherUnitPrice,
				casePrice,
				initialNormalizedQuantity,
				productInfo,
				discountInfo
		);
	}
}
