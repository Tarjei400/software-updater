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
public class Client {

    protected String version;
    protected List<Patch> patches;

    public Client(String version, List<Patch> patches) {
        this.version = version;
        this.patches = new ArrayList<Patch>(patches);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Patch> getPatches() {
        return new ArrayList<Patch>(patches);
    }

    public void setPatches(List<Patch> patches) {
        this.patches = new ArrayList<Patch>(patches);
    }

    public static Client read(byte[] content) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(new ByteArrayInputStream(content));
            Element _rootNode = doc.getDocumentElement();


            NodeList _versionNodeList = _rootNode.getElementsByTagName("version");
            if (_versionNodeList.getLength() == 0) {
                return null;
            }
            Element _versionElement = (Element) _versionNodeList.item(0);

            String _version = _versionElement.getTextContent();


            NodeList _patchesNodeList = _rootNode.getElementsByTagName("patches");
            if (_patchesNodeList.getLength() == 0) {
                return null;
            }
            Element _patchesElement = (Element) _patchesNodeList.item(0);

            List<Patch> _patches = new ArrayList<Patch>();

            NodeList _patchNodeList = _patchesElement.getElementsByTagName("patch");
            for (int i = 0, iEnd = _patchNodeList.getLength(); i < iEnd; i++) {
                Element _patchNode = (Element) _patchNodeList.item(i);
                Patch _patch = Patch.read(_patchNode);
                if (_patch != null) {
                    _patches.add(_patch);
                }
            }

            return new Client(_version, _patches);
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
            Element rootElement = doc.createElement("root");
            doc.appendChild(rootElement);


            Element versionElement = doc.createElement("version");
            versionElement.setTextContent(version);
            rootElement.appendChild(versionElement);


            Element patchesElement = doc.createElement("patches");
            rootElement.appendChild(patchesElement);

            for (Patch patch : patches) {
                Element _patchElement = patch.getElement(doc);
                if (_patchElement != null) {
                    patchesElement.appendChild(_patchElement);
                }
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

    public static class Patch {

        private String path;
        private String encryptionType;
        private String encryptionKey;
        private String encryptionIV;

        public Patch(String path, String encryptionType, String encryptionKey, String encryptionIV) {
            this.path = path;
            this.encryptionType = encryptionType;
            this.encryptionKey = encryptionKey;
            this.encryptionIV = encryptionIV;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getEncryptionType() {
            return encryptionType;
        }

        public void setEncryptionType(String encryptionType) {
            this.encryptionType = encryptionType;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public String getEncryptionIV() {
            return encryptionIV;
        }

        public void setEncryptionIV(String encryptionIV) {
            this.encryptionIV = encryptionIV;
        }

        protected static Patch read(Element updateElement) {
            if (updateElement == null) {
                return null;
            }

            NodeList _pathNodeList = updateElement.getElementsByTagName("path");
            if (_pathNodeList.getLength() == 0) {
                return null;
            }
            Element _pathElement = (Element) _pathNodeList.item(0);
            String _path = _pathElement.getTextContent();

            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;

            NodeList _encryptionNodeList = updateElement.getElementsByTagName("encryption");
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

            return new Patch(_path, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _patch = doc.createElement("patch");

            Element _path = doc.createElement("path");
            _path.appendChild(doc.createTextNode(path));
            _patch.appendChild(_path);

            if (encryptionType != null) {
                Element _encryption = doc.createElement("encryption");
                _patch.appendChild(_encryption);

                Element _encryptionType = doc.createElement("type");
                _encryptionType.appendChild(doc.createTextNode(encryptionType));
                _encryption.appendChild(_encryptionType);

                Element _encryptionKey = doc.createElement("key");
                _encryptionKey.appendChild(doc.createTextNode(encryptionKey));
                _encryption.appendChild(_encryptionKey);

                Element _encryptionIV = doc.createElement("IV");
                _encryptionIV.appendChild(doc.createTextNode(encryptionIV));
                _encryption.appendChild(_encryptionIV);
            }

            return _patch;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("updater.xml");
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = new FileInputStream(file);
        fin.read(content);
        fin.close();

        Client client = Client.read(content);
        System.out.println(client.output());
    }
}
