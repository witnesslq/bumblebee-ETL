package com.gsta.bigdata.etl.localFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gsta.bigdata.etl.ETLException;
import com.gsta.bigdata.etl.core.ETLData;
import com.gsta.bigdata.etl.core.ETLProcess;
import com.gsta.bigdata.etl.core.TransformException;
import com.gsta.bigdata.utils.StringUtils;
import com.gsta.bigdata.utils.XmlTools;

/**
 * deal zte xml source data file
 * 
 * @author Shine
 *
 */
public class ZteNODEBXMLHandler extends AbstractHandler {
	private StringBuffer xmlBuffer = new StringBuffer();

	private String beginTime;

	private boolean xmlFlag = false;

	private String beginFileHeader = "<fileHeader";
	private String endFileHeader = "</fileHeader>";
	private String beginMeasData = "<measData>";
	private String endMeasData = "</measData>";

	// node path
	private String fileHeaderPath = "/fileHeader/measCollec";
	private String measTypesPath = "/measData/measInfo/measTypes";
	private String measValuePath = "/measData/measInfo/measValue";

	// date format pattern
	private String oldPattern = "yyyy-MM-dd'T'HH:mm:ss";
	private String newPattern = "yyyyMMddHHmmss";

	private static final String ATTR_BEGIN_TIME = "beginTime";
	private static final String ATTR_MEASOBJLDN = "measObjLdn";
	private static final String TAG_MEASRESULTS = "measResults";

	private static final String FIELD_COLLECTTIME = "COLLECTTIME";
	private static final String FIELD_SBNID = "SBNID";
	private static final String FIELD_ENODEBID = "ENODEBID";
	private static final String FIELD_CELLID = "CellID";

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ZteNODEBXMLHandler(ETLProcess process) {
		super(process);
	}

	@Override
	protected void _handle(String line) throws ETLException {
		if (line.contains(beginFileHeader)) {
			this.xmlFlag = true;
			// clear up,deal next block
			this.xmlBuffer.delete(0, this.xmlBuffer.toString().length());
		} else if (line.contains(endFileHeader)) {
			this.xmlFlag = false;
			this.xmlBuffer.append(line);
			String xml = this.xmlBuffer.toString();

			// get begin time
			try {
				getBeginTime(xml);
			} catch (ETLException e) {
				logger.error("get wrong begin time:" + e.getMessage());

				super.errorRecords.add(e.getMessage());
				if (super.errorRecords.size() >= super.errorRecordThreshold) {
					super.writeFiles(super.errorOutStream, super.errorRecords,super.errorFileName);
				}

				super.errorCount++;
			}
		} else if (line.contains(beginMeasData)) {
			// clear up,deal next block
			this.xmlBuffer.delete(0, this.xmlBuffer.toString().length());
			this.xmlFlag = true;
		} else if (line.contains(endMeasData)) {
			this.xmlFlag = false;
			this.xmlBuffer.append(line);
			String xml = this.xmlBuffer.toString();
			try {
				getMeasData(xml);
			} catch (ETLException e) {
				logger.error(e.getMessage());

				super.errorRecords.add(e.getMessage());
				if (super.errorRecords.size() >= super.errorRecordThreshold) {
					super.writeFiles(super.errorOutStream, super.errorRecords,super.errorFileName);
				}

				super.errorCount++;
			}
		}

		// if flag is true,append block content to StringBuffer
		if (this.xmlFlag) {
			this.xmlBuffer.append(line);
		}
	}

	// parse <MeasData></MeasData> block content get filed name and value
	private void getMeasData(String xml) throws ETLException {
		if (xml == null) {
			return;
		}

		String[] fields;
		NodeList measValues;
		try {
			Document doc = XmlTools.loadFromContent(xml);
			if (doc == null) {
				throw new ETLException("doc is null,xml=");
			}
			// ex:<measTypes>C373546100 C373546101 C373546102 C373546103
			// C373546104 C373546105 C373546106 C373546107 C373546172 C373546173
			// </measTypes>
			Node measTypes = XmlTools.getNodeByPath(doc, measTypesPath);
			if (measTypes == null) {
				throw new ETLException("measTypes is null,xml=" + xml.substring(0,1024));
			}

			String types = XmlTools.getNodeValue((Element) measTypes);
			if (types == null) {
				throw new ETLException("types is null,xml=" + xml.substring(0,1024));
			}

			fields = types.split(" ");
			// ex:<measValue measObjLdn="SBNID=440101,ENODEBID=479232,CellID=0">
			measValues = XmlTools.getNodeListByPath(doc, measValuePath);
			if (measValues == null) {
				throw new ETLException("measValues is null,xml=" + xml.substring(0,1024) );
			}
		} catch (ParserConfigurationException | SAXException | IOException
				| XPathExpressionException e) {
			throw new ETLException(e);
		}

		for (int i = 0; i < measValues.getLength(); i++) {
			ETLData data = new ETLData();
			data.addData(FIELD_COLLECTTIME, this.beginTime);

			Node measValue = measValues.item(i);
			String measObjLdn = null;
			try {
				// measObjLdn="SBNID=440101,ENODEBID=479232,CellID=0"
				measObjLdn = this.getMeasObjLdn((Element) measValue, data);

				// ex:<measResults>0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
				this.getMeasResult((Element) measValue, fields, data);

				super.process.verifyFields(data);

				super.process.onTransform(data);

				String outputValue = super.process.getOutputValue(data);
				super.queue.add(outputValue);
				if (super.queue.size() >= super.recordThreshold) {
					super.writeFiles(super.queueOutStream, super.queue,super.queueFileName);
				}

				super.recordCount++;
			} catch (ETLException|TransformException e) {
				String info = " measObjLdn:" + measObjLdn + ",measTypes=" + e.getMessage() + " does not exist";
				super.errorRecords.add(info);
				super.errorCount++;

				if (super.errorRecords.size() >= super.errorRecordThreshold) {
					super.writeFiles(super.errorOutStream, super.errorRecords,super.errorFileName);
				}
			}
		}//end for
	}

	// ex:<measResults>0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
	// 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
	// 0</measResults>
	private void getMeasResult(Element element, String[] fields, ETLData data)
			throws ETLException {
		if (element == null) {
			throw new ETLException("measResult element is null.");
		}

		Node measResults = XmlTools.getFirstChildByTagName(element,TAG_MEASRESULTS);
		String measResult = "";
		try {
			measResult = XmlTools.getNodeValue((Element) measResults);
		} catch (XPathExpressionException e) {
			throw new ETLException(e);
		}

		String[] values = measResult.split(" ");
		if (fields.length != values.length) {
			throw new ETLException("measTypes size=" + fields.length
					+ ",but measResults size=" + values.length);
		}

		for (int j = 0; j < fields.length; j++) {
			data.addData(fields[j], values[j]);
		}
	}

	private String getMeasObjLdn(Element element, ETLData data)
			throws ETLException {
		if (element == null) {
			throw new ETLException("measObjLdn element is null.");
		}

		String measObjLdn = null;
		try {
			measObjLdn = XmlTools.getNodeAttr(element, ATTR_MEASOBJLDN);
		} catch (XPathExpressionException e) {
			throw new ETLException(e);
		}

		// ex:SBNID=440101,ENODEBID=479232,CellID=0
		String[] objLdns = measObjLdn.split(",");
		for (String objLdn : objLdns) {
			String[] obj = objLdn.split("=");
			if (FIELD_SBNID.equals(obj[0])) {
				data.addData(FIELD_SBNID, obj[1]);
			} else if (FIELD_ENODEBID.equals(obj[0])) {
				data.addData(FIELD_ENODEBID, obj[1]);
			} else if (FIELD_CELLID.equals(obj[0])) {
				data.addData(FIELD_CELLID, obj[1]);
			}
		}

		return measObjLdn;
	}

	// parse <fileHeader></fileHeader> block content get begin time
	private void getBeginTime(String xml) throws ETLException {
		if (xml == null) {
			return;
		}

		try {
			Document doc = XmlTools.loadFromContent(xml);
			// ex: <measCollec
			// beginTime="2015-08-06T10:00:00+08:00"></measCollec>
			Node process = XmlTools.getNodeByPath(doc, fileHeaderPath);
			Map<String, String> attrs = XmlTools.getNodeAttrs((Element) process);
			this.beginTime = attrs.get(ATTR_BEGIN_TIME);
			if (null != this.beginTime) {
				this.beginTime = StringUtils.dateFormat(this.beginTime,oldPattern, newPattern);
			}
		} catch (ParserConfigurationException | SAXException | IOException
				| XPathExpressionException | ParseException e) {
			throw new ETLException(e);
		}
	}

}
