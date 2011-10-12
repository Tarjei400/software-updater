package starter.script;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

    protected List<CatalogUpdate> updates;

    public Catalog(List<CatalogUpdate> updates) {
        this.updates = new ArrayList<CatalogUpdate>(updates);
    }

    public List<CatalogUpdate> getUpdates() {
        return new ArrayList<CatalogUpdate>(updates);
    }

    public void setUpdates(List<CatalogUpdate> updates) {
        this.updates = new ArrayList<CatalogUpdate>(updates);
    }

    public static Catalog read(byte[] content) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(new ByteArrayInputStream(content));
            Element _updatesNode = doc.getDocumentElement();

            List<CatalogUpdate> _updates = new ArrayList<CatalogUpdate>();

            NodeList _updateNodeList = _updatesNode.getElementsByTagName("update");
            for (int i = 0, iEnd = _updateNodeList.getLength(); i < iEnd; i++) {
                Element _updateNode = (Element) _updateNodeList.item(i);
                CatalogUpdate _catalogUpdate = CatalogUpdate.read(_updateNode);
                if (_catalogUpdate != null) {
                    _updates.add(_catalogUpdate);
                }
            }

            return new Catalog(_updates);
        } catch (Exception ex) {
            Logger.getLogger(Catalog.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String output() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("updates");
            doc.appendChild(rootElement);

            for (CatalogUpdate catalogUpdate : updates) {
                rootElement.appendChild(catalogUpdate.getElement(doc));
            }


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

            return writer.toString();
        } catch (Exception ex) {
            Logger.getLogger(Catalog.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    public static class CatalogUpdate {

        protected String versionFrom;
        protected String versionTo;
        protected String patchUrl;
        protected String patchChecksum;
        protected int patchLength;
        protected String patchEncryptionType;
        protected String patchEncryptionKey;
        protected String patchEncryptionIV;

        public CatalogUpdate(String versionFrom, String versionTo, String patchUrl, String patchChecksum, int patchLength, String patchEncryptionType, String patchEncryptionKey, String patchEncryptionIV) {
            this.versionFrom = versionFrom;
            this.versionTo = versionTo;
            this.patchUrl = patchUrl;
            this.patchChecksum = patchChecksum;
            this.patchLength = patchLength;
            this.patchEncryptionType = patchEncryptionType;
            this.patchEncryptionKey = patchEncryptionKey;
            this.patchEncryptionIV = patchEncryptionIV;
        }

        public String getVersionFrom() {
            return versionFrom;
        }

        public void setVersionFrom(String versionFrom) {
            this.versionFrom = versionFrom;
        }

        public String getPatchChecksum() {
            return patchChecksum;
        }

        public void setPatchChecksum(String patchChecksum) {
            this.patchChecksum = patchChecksum;
        }

        public String getVersionTo() {
            return versionTo;
        }

        public void setVersionTo(String versionTo) {
            this.versionTo = versionTo;
        }

        public String getPatchUrl() {
            return patchUrl;
        }

        public void setPatchUrl(String patchUrl) {
            this.patchUrl = patchUrl;
        }

        public int getPatchLength() {
            return patchLength;
        }

        public void setPatchLength(int patchLength) {
            this.patchLength = patchLength;
        }

        public String getPatchEncryptionType() {
            return patchEncryptionType;
        }

        public void setPatchEncryptionType(String patchEncryptionType) {
            this.patchEncryptionType = patchEncryptionType;
        }

        public String getPatchEncryptionKey() {
            return patchEncryptionKey;
        }

        public void setPatchEncryptionKey(String patchEncryptionKey) {
            this.patchEncryptionKey = patchEncryptionKey;
        }

        public String getPatchEncryptionIV() {
            return patchEncryptionIV;
        }

        public void setPatchEncryptionIV(String patchEncryptionIV) {
            this.patchEncryptionIV = patchEncryptionIV;
        }

        protected static CatalogUpdate read(Element updateElement) {
            if (updateElement == null) {
                return null;
            }

            NodeList _versionNodeList = updateElement.getElementsByTagName("version");
            if (_versionNodeList.getLength() == 0) {
                return null;
            }
            Element _versionElement = (Element) _versionNodeList.item(0);

            NodeList _versionFromNodeList = _versionElement.getElementsByTagName("from");
            NodeList _versionToNodeList = _versionElement.getElementsByTagName("to");
            if (_versionFromNodeList.getLength() == 0 || _versionToNodeList.getLength() == 0) {
                return null;
            }
            Element _versionFromElement = (Element) _versionFromNodeList.item(0);
            Element _versionToElement = (Element) _versionToNodeList.item(0);

            String _versionFrom = _versionFromElement.getTextContent();
            String _versionTo = _versionToElement.getTextContent();


            NodeList _patchNodeList = updateElement.getElementsByTagName("patch");
            if (_patchNodeList.getLength() == 0) {
                return null;
            }
            Element _patchElement = (Element) _patchNodeList.item(0);

            NodeList _patchUrlNodeList = _patchElement.getElementsByTagName("url");
            NodeList _patchChecksumNodeList = _patchElement.getElementsByTagName("checksum");
            NodeList _patchLengthNodeList = _patchElement.getElementsByTagName("length");
            if (_patchUrlNodeList.getLength() == 0 || _patchChecksumNodeList.getLength() == 0 || _patchLengthNodeList.getLength() == 0) {
                return null;
            }
            Element _patchUrlElement = (Element) _patchUrlNodeList.item(0);
            Element _patchChecksumElement = (Element) _patchChecksumNodeList.item(0);
            Element _patchLengthElement = (Element) _patchLengthNodeList.item(0);

            String _patchUrl = _patchUrlElement.getTextContent();
            String _patchChecksum = _patchChecksumElement.getTextContent();
            int _patchLength = Integer.parseInt(_patchLengthElement.getTextContent());


            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;

            NodeList _encryptionNodeList = _patchElement.getElementsByTagName("encryption");
            if (_encryptionNodeList.getLength() != 0) {
                Element _encryptionElement = (Element) _encryptionNodeList.item(0);

                NodeList _encryptionTypeNodeList = _encryptionElement.getElementsByTagName("type");
                NodeList _encryptionKeyNodeList = _encryptionElement.getElementsByTagName("key");
                NodeList _encryptionIVNodeList = _encryptionElement.getElementsByTagName("IV");
                if (_encryptionTypeNodeList.getLength() == 0 || _encryptionKeyNodeList.getLength() == 0 || _encryptionIVNodeList.getLength() == 0) {
                    return null;
                }
                Element _encryptionTypeElement = (Element) _encryptionTypeNodeList.item(0);
                Element _encryptionKeyElement = (Element) _encryptionKeyNodeList.item(0);
                Element _encryptionIVElement = (Element) _encryptionIVNodeList.item(0);

                _encryptionType = _encryptionTypeElement.getTextContent();
                _encryptionKey = _encryptionKeyElement.getTextContent();
                _encryptionIV = _encryptionIVElement.getTextContent();
            }


            return new CatalogUpdate(_versionFrom, _versionTo, _patchUrl, _patchChecksum, _patchLength, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _update = doc.createElement("update");

            //<editor-fold defaultstate="collapsed" desc="version">
            Element _version = doc.createElement("version");
            _update.appendChild(_version);

            Element _versionFrom = doc.createElement("from");
            _versionFrom.appendChild(doc.createTextNode(versionFrom));
            _version.appendChild(_versionFrom);

            Element _versionTo = doc.createElement("to");
            _versionTo.appendChild(doc.createTextNode(versionTo));
            _version.appendChild(_versionTo);
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="patch">
            Element _patch = doc.createElement("patch");
            _update.appendChild(_patch);

            Element _patchUrl = doc.createElement("url");
            _patchUrl.appendChild(doc.createTextNode(patchUrl));
            _patch.appendChild(_patchUrl);

            Element _patchChecksum = doc.createElement("checksum");
            _patchChecksum.appendChild(doc.createTextNode(patchChecksum));
            _patch.appendChild(_patchChecksum);

            Element _patchLength = doc.createElement("length");
            _patchLength.appendChild(doc.createTextNode(Integer.toString(patchLength)));
            _patch.appendChild(_patchLength);

            if (patchEncryptionType != null) {
                Element _encryption = doc.createElement("encryption");
                _patch.appendChild(_encryption);

                Element _encryptionType = doc.createElement("type");
                _encryptionType.appendChild(doc.createTextNode(patchEncryptionType));
                _encryption.appendChild(_encryptionType);

                Element _encryptionKey = doc.createElement("key");
                _encryptionKey.appendChild(doc.createTextNode(patchEncryptionKey));
                _encryption.appendChild(_encryptionKey);

                Element _encryptionIV = doc.createElement("IV");
                _encryptionIV.appendChild(doc.createTextNode(patchEncryptionIV));
                _encryption.appendChild(_encryptionIV);
            }
            //</editor-fold>

            return _update;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("catalog.xml");
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = new FileInputStream(file);
        fin.read(content);
        fin.close();

        Catalog catalog = Catalog.read(content);
        System.out.println(catalog.output());
    }
}
