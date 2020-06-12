package my.hehe.demo.services;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import my.hehe.demo.common.StreamUtils;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;


public class WebServiceClient {
	static Logger logger = LoggerFactory.getLogger(WebServiceClient.class);

	static void prepaerClient(ServiceClient serviceClient, String wsdl, String qName, String method) throws Throwable {

		Options option;
		serviceClient.setOptions(option = new Options());
		option.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		option.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		option.setAction(String.format("%s%s", qName, method));// 值为targetNamespace+methodName
		option.setTo(new EndpointReference(wsdl));


	}


	public static OMElement call(String wsdl, String qName, String method, KeyValue... keyValues) {
		//返回结果
		try {
			logger.info("[WebService Wsdl]" + wsdl);
			logger.info("[WebService qName]" + qName);
			logger.info("[WebService method]" + method);
			logger.info("[WebService obj]" + ArrayUtils.toString(keyValues));
			ServiceClient serviceClient = new ServiceClient();

			prepaerClient(serviceClient, wsdl, qName, method);
			OMFactory fac = OMAbstractFactory.getOMFactory();
			OMNamespace namespace = fac.createOMNamespace(qName, "");
			OMElement element = fac.createOMElement(method, namespace);
			Arrays.stream(keyValues).forEach(keyValue -> {
				OMElement omElement = fac.createOMElement(keyValue.key, namespace);
				omElement.setText(keyValue.value == null ? "" : keyValue.value.toString());
				element.addChild(omElement);
			});


			element.build();
			System.out.println(element.toString());
			return serviceClient.sendReceive(element);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static OMElement callRpc(String wsdl, String qName, String method, KeyValue... keyValues) {
		//返回结果
		try {
			logger.info("[WebService Wsdl]" + wsdl);
			logger.info("[WebService qName]" + qName);
			logger.info("[WebService method]" + method);
			logger.info("[WebService obj]" + ArrayUtils.toString(keyValues));
			RPCServiceClient serviceClient = new RPCServiceClient();
			/*Options options = serviceClient.getOptions();
			//  指定调用WebService的URL
			EndpointReference targetEPR = new EndpointReference(wsdl);
			options.setTo(targetEPR);
			//  指定add方法的参数值*/
			prepaerClient(serviceClient, wsdl, qName, method);
			Object[] opAddEntryArgs = Arrays.stream(keyValues).map(KeyValue::getValue).toArray();
			//  指定Integer方法返回值的数据类型的Class对象
			//  指定要调用的add方法及WSDL文件的命名空间
			QName opAddEntry = new QName(qName, method);
			/**
			 * 调用add方法并输出该方法的返回值
			 *  invokeBlocking方法有三个参数，其中第一个参数的类型是QName对象，
			 *  表示要调用的方法名；第二个参数表示要调用的WebService方法的参数值，
			 *   参数类型为Object[]；  第三个参数表示WebService方法的返回值类型的Class对象，
			 *   参数类型为Class[],当方法没有参数时，invokeBlocking方法的第二个参数值不能是null，
			 *   而要使用new Object[]{}
			 */
			return serviceClient.invokeBlocking(opAddEntry, opAddEntryArgs);
			//如果被调用的WebService方法没有返回值，应使用RPCServiceClient类的invokeRobust方法，
			//该方法只有两个参数，它们的含义与invokeBlocking方法的前两个参数的含义相同
			// serviceClient.invokeRobust(opName, opAddEntryArgs);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static class KeyValue implements Map.Entry<String, Object> {
		String key;
		Object value;

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public Object getValue() {
			return this.value;
		}

		@Override
		public Object setValue(Object value) {
			throw new RuntimeException();
		}
	}

	public static KeyValue keyValue(String key, Object value) {
		KeyValue kv = new KeyValue();
		kv.key = key;
		kv.value = value;
		return kv;
	}

	static void demo() {
		try {
			ServiceClient serviceClient = new ServiceClient();

			Options option = new Options();
			option.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
			option.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			option.setAction("http://WebXml.com.cn/getWeather");
			// 值为targetNamespace+methodName
			EndpointReference epfs = new EndpointReference("http://ws.webxml.com.cn/WebServices/WeatherWS.asmx?wsdl");
			option.setTo(epfs);
			serviceClient.setOptions(option);

			OMFactory fac = OMAbstractFactory.getOMFactory();
			OMNamespace namespace = fac.createOMNamespace("http://WebXml.com.cn/", "");
			OMElement element = fac.createOMElement("getWeather", namespace);
			OMElement theCityCode = fac.createOMElement("theCityCode ", namespace);
			theCityCode.setText("北京");
			element.addChild(theCityCode);
			OMElement theUserID = fac.createOMElement("theUserID ", namespace);
			theUserID.setText("");
			element.addChild(theUserID);

			OMElement result = serviceClient.sendReceive(element);
			System.out.println(result);
			System.out.println("****************************************************************************************************************");
			Iterator in = result.getChildrenWithLocalName("getWeatherResult");
			while (in.hasNext()) {
				OMElement om = (OMElement) in.next();
				Iterator in2 = om.getChildElements();
				while (in2.hasNext()) {
//                System.out.println(in2.next().toString());
					System.out.println(((OMElement) in2.next()).getText());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		try {
			// String url = "http://localhost:8080/axis2ServerDemo/services/StockQuoteService";
			String url = "http://134.192.232.69:9009/irec/service/Iresource?wsdl";
			String tns = "http://webservice.sys.resmaster.ztesoft.com/";
			/*OMElement o = call("http://ws.webxml.com.cn/WebServices/WeatherWS.asmx?wsdl", "http://WebXml.com.cn/", "getWeather", keyValue("theCityCode", "北京"), keyValue("theUserID", ""));
			System.out.println(o);*/

			System.out.println(callRpc("http://ws.webxml.com.cn/WebServices/WeatherWS.asmx?wsdl", "http://WebXml.com.cn/", "getWeather", keyValue("theCityCode", "北京"), keyValue("theUserID", "")));
//			demo();
		} catch (Throwable axisFault) {
			axisFault.printStackTrace();
		}
	}


}
