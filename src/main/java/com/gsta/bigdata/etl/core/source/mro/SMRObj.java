package com.gsta.bigdata.etl.core.source.mro;

public class SMRObj implements Comparable<SMRObj> {
	private String MR_LteNcRSRP;
	private String MR_LteNcRSRQ;
	private String MR_LteNcEarfcn;
	private String MR_LteNcPci;
	
	public final static String  FIELD_MR_LteNcRSRP = "MR_LteNcRSRP";
	public final static String  FIELD_MR_LteNcRSRQ = "MR_LteNcRSRQ";
	public final static String  FIELD_MR_LteNcEarfcn = "MR_LteNcEarfcn";
	public final static String  FIELD_MR_LteNcPci = "MR_LteNcPci";
	
	public SMRObj(String MR_LteNcRSRP,String MR_LteNcRSRQ,
			String MR_LteNcEarfcn,String MR_LteNcPci){
		this.MR_LteNcEarfcn = MR_LteNcEarfcn;
		this.MR_LteNcPci = MR_LteNcPci;
		this.MR_LteNcRSRP = MR_LteNcRSRP;
		this.MR_LteNcRSRQ = MR_LteNcRSRQ;
	}

	@Override
	/**
	 * reverse order
	 */
	public int compareTo(SMRObj o) {
		try {
			return Integer.parseInt(o.getMR_LteNcRSRP())
					- Integer.parseInt(this.MR_LteNcRSRP);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public String getMR_LteNcRSRP() {
		return MR_LteNcRSRP;
	}

	public String getMR_LteNcRSRQ() {
		return MR_LteNcRSRQ;
	}

	public String getMR_LteNcEarfcn() {
		return MR_LteNcEarfcn;
	}

	public String getMR_LteNcPci() {
		return MR_LteNcPci;
	}
}
