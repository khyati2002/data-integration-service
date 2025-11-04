package com.salescode.dim.jooq.impl;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;
import java.io.Serializable;
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutletActivity extends com.salescode.dim.jooq.generated.tables.pojos.OutletActivity implements Serializable {
	private static final long serialVersionUID = 6364280713919356300L;
	public OutletActivity(){
		super();
	}
	private OutletActivity(com.salescode.dim.jooq.generated.tables.pojos.OutletActivity targets) {
		super(targets);
	}
	@JsonSetter("outletCode")
	public void setOutletCode(String outletCode) {
		super.setOutletcode(outletCode);
	}
	@JsonSetter("loginId")
	public void setLoginId(String loginId) {
		setLoginid(loginId);
	}
	public String getLoginId() {
		return getLoginid();
	}
	public String getOutletCode() {
		return getOutletcode();
	}
	public static OutletActivity of(com.salescode.dim.jooq.generated.tables.pojos.OutletActivity targets) {
		if(targets == null) {
			return null;
		}
		return new OutletActivity(targets);
	}
}