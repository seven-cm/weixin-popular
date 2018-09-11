package weixin.popular.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

/**
 * XML 数据接收对象转换工具类
 * @author LiYi
 *
 */
public class XMLConverUtil{

	private static final ThreadLocal<Map<Class<?>,Marshaller>> mMapLocal = new ThreadLocal<Map<Class<?>,Marshaller>>() {
		@Override
		protected Map<Class<?>, Marshaller> initialValue() {
			return new HashMap<Class<?>, Marshaller>();
		}
	};

	private static final ThreadLocal<Map<Class<?>,Unmarshaller>> uMapLocal = new ThreadLocal<Map<Class<?>,Unmarshaller>>(){
		@Override
		protected Map<Class<?>, Unmarshaller> initialValue() {
			return new HashMap<Class<?>, Unmarshaller>();
		}
	};

	/**
	 * XML to Object
	 * @param <T> T
	 * @param clazz clazz
	 * @param xml xml
	 * @return T
	 */
	public static <T> T convertToObject(Class<T> clazz,String xml){
		return convertToObject(clazz,new StringReader(xml));
	}

	/**
	 * XML to Object
	 * @param <T> T
	 * @param clazz clazz
	 * @param inputStream  inputStream
	 * @return T
	 */
	public static <T> T convertToObject(Class<T> clazz,InputStream inputStream){
		return convertToObject(clazz,new InputStreamReader(inputStream));
	}
	
	/**
	 * XML to Object
	 * @param <T> T
	 * @param clazz clazz
	 * @param inputStream  inputStream
	 * @param charset charset
	 * @return T
	 */
	public static <T> T convertToObject(Class<T> clazz,InputStream inputStream,Charset charset){
		return convertToObject(clazz,new InputStreamReader(inputStream, charset));
	}

	/**
	 * XML to Object
	 * @param <T> T
	 * @param clazz clazz
	 * @param reader reader
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convertToObject(Class<T> clazz,Reader reader){
		try {
			Map<Class<?>, Unmarshaller> uMap = uMapLocal.get();
			if(!uMap.containsKey(clazz)){
				JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				uMap.put(clazz, unmarshaller);
			}
			//XXE漏洞修复
			XMLInputFactory xif = XMLInputFactory.newFactory();
			xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			xif.setProperty(XMLInputFactory.SUPPORT_DTD, true);
			XMLStreamReader xsr = xif.createXMLStreamReader(reader);
			return (T) uMap.get(clazz).unmarshal(xsr);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Object to XML
	 * @param object object
	 * @return xml
	 */
	public static String convertToXML(Object object){
		try {
			Map<Class<?>, Marshaller> mMap = mMapLocal.get();
			if(!mMap.containsKey(object.getClass())){
				JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				//设置CDATA输出字符
				marshaller.setProperty(CharacterEscapeHandler.class.getName(), new CharacterEscapeHandler() {
					public void escape(char[] ac, int i, int j, boolean flag, Writer writer) throws IOException {
						writer.write(ac, i, j);
					}
				});
				mMap.put(object.getClass(), marshaller);
			}
			StringWriter stringWriter = new StringWriter();
			mMap.get(object.getClass()).marshal(object,stringWriter);
			return stringWriter.getBuffer().toString();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 转换简单的xml to map
	 * @param xml xml
	 * @return map
	 */
	public static Map<String,String> convertToMap(String xml){
		Map<String, String> map = new LinkedHashMap<String,String>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			//XXE漏洞修复
			String FEATURE = null;
			try {
				FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
				dbf.setFeature(FEATURE, true);
				FEATURE = "http://xml.org/sax/features/external-general-entities";
				dbf.setFeature(FEATURE, false);
				FEATURE = "http://xml.org/sax/features/external-parameter-entities";
				dbf.setFeature(FEATURE, false);
				FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
				dbf.setFeature(FEATURE, false);
				dbf.setXIncludeAware(false);
				dbf.setExpandEntityReferences(false);
			} catch (ParserConfigurationException e) {
			}

			DocumentBuilder db = dbf.newDocumentBuilder();
			StringReader sr = new StringReader(xml);
			InputSource is = new InputSource(sr);
			Document document = db.parse(is);

			Element root = document.getDocumentElement();
			if(root != null){
				NodeList childNodes = root.getChildNodes();
				if(childNodes != null && childNodes.getLength()>0){
					for(int i = 0;i < childNodes.getLength();i++){
						Node node = childNodes.item(i); 
						if( node != null && node.getNodeType() == Node.ELEMENT_NODE){
							map.put(node.getNodeName(), node.getTextContent());
						}
					}
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
