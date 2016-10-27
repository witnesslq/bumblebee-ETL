package com.gsta.bigdata.etl.flume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

public class GZDPIInterceptor implements Interceptor {
	private String delimiter;
	private String fields;
	private String headerFields;
	private String[] lstFields;
	private String[] lstHeaderFields;
	private static final String NotSeeCharDefineInConf = "001";
	
	public GZDPIInterceptor(String delimiter, String fields, String headerFields) {
		super();
		this.delimiter = delimiter;
		this.fields = fields;
		this.headerFields = headerFields;
	}

	@Override
	public void initialize() {
		if (this.fields != null) {
			this.lstFields = this.fields.split(",", -1);
		}
		
		if(this.headerFields != null){
			this.lstHeaderFields = this.headerFields.split(",",-1);
		}
		
		if(NotSeeCharDefineInConf.equals(this.delimiter)){
			this.delimiter = "\001";
		}
	}

	@Override
	public Event intercept(Event event) {
		if (event == null) {
			return null;
		}

		String line = new String(event.getBody());
		if (line == null || "".equals(line)) {
			return null;
		}
		
		String[] fieldValues = line.split(this.delimiter, -1);
		if (fieldValues.length == this.lstFields.length) {
			Map<String, String> data = new HashMap<String, String>();
			for (int i = 0; i < fieldValues.length; i++) {
				data.put(this.lstFields[i], fieldValues[i]);
			}

			for (String header : this.lstHeaderFields) {
				event.getHeaders().put(header, data.get(header));
			}
		}

		return event;
	}

	@Override
	public List<Event> intercept(List<Event> events) {
		List<Event> retEvents = new ArrayList<Event>();

		for (Event event : events) {
			Event interceptedEvent = intercept(event);  
            if (interceptedEvent != null) {  
            	retEvents.add(interceptedEvent);  
            }  
		}

		return retEvents;
	}

	@Override
	public void close() {
		
	}
	
	public static class Builder implements Interceptor.Builder {
		private String delimiter;
		private String fields;
		private String headerFields;
		
		@Override
		public void configure(Context context) {
			this.delimiter = context.getString("delimiter");
			this.fields = context.getString("fields");
			this.headerFields = context.getString("headerFields");
		}

		@Override
		public Interceptor build() {
			return new GZDPIInterceptor(this.delimiter, this.fields, this.headerFields);
		}  
	}
}
