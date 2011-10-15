package updater.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import updater.util.XMLUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Catalog {

    protected List<Update> updates;

    public Catalog(List<Update> updates) {
        this.updates = new ArrayList<Update>(updates);
    }

    public List<Update> getUpdates() {
        return new ArrayList<Update>(updates);
    }

    public void setUpdates(List<Update> updates) {
        this.updates = new ArrayList<Update>(updates);
    }

    public static Catalog read(byte[] content) throws InvalidFormatException {
        Document doc = XMLUtil.readDocument(content);
        if (doc == null) {
            throw new InvalidFormatException();
        }

        Element _updatesNode = doc.getDocumentElement();

        List<Update> _updates = new ArrayList<Update>();

        NodeList _updateNodeList = _updatesNode.getElementsByTagName("update");
        for (int i = 0, iEnd = _updateNodeList.getLength(); i < iEnd; i++) {
            Element _updateNode = (Element) _updateNodeList.item(i);
            _updates.add(Update.read(_updateNode));
        }

        return new Catalog(_updates);
    }

    public String output() {
        Document doc = XMLUtil.createEmptyDocument();
        if (doc == null) {
            return null;
        }

        Element rootElement = doc.createElement("updates");
        doc.appendChild(rootElement);

        for (Update catalogUpdate : updates) {
            rootElement.appendChild(catalogUpdate.getElement(doc));
        }

        return XMLUtil.getOutput(doc);
    }

    public static class Update {

        protected int id;
        protected String versionFrom;
        protected String versionTo;
        protected String patchUrl;
        protected String patchChecksum;
        protected int patchLength;
        protected String patchEncryptionType;
        protected String patchEncryptionKey;
        protected String patchEncryptionIV;

        public Update(int id, String versionFrom, String versionTo, String patchUrl, String patchChecksum, int patchLength, String patchEncryptionType, String patchEncryptionKey, String patchEncryptionIV) {
            this.id = id;
            this.versionFrom = versionFrom;
            this.versionTo = versionTo;
            this.patchUrl = patchUrl;
            this.patchChecksum = patchChecksum;
            this.patchLength = patchLength;
            this.patchEncryptionType = patchEncryptionType;
            this.patchEncryptionKey = patchEncryptionKey;
            this.patchEncryptionIV = patchEncryptionIV;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
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

        protected static Update read(Element updateElement) throws InvalidFormatException {
            if (updateElement == null) {
                throw new NullPointerException();
            }

            int _id = 0;
            try {
                _id = Integer.parseInt(updateElement.getAttribute("id"));
            } catch (Exception ex) {
                throw new InvalidFormatException("attribute 'id' for 'update' element not exist");
            }

            Element _versionElement = XMLUtil.getElement(updateElement, "version", true);
            String _versionFrom = XMLUtil.getTextContent(_versionElement, "from", true);
            String _versionTo = XMLUtil.getTextContent(_versionElement, "to", true);

            Element _patchElement = XMLUtil.getElement(updateElement, "patch", true);
            String _patchUrl = XMLUtil.getTextContent(_patchElement, "url", true);
            String _patchChecksum = XMLUtil.getTextContent(_patchElement, "checksum", true);
            int _patchLength = Integer.parseInt(XMLUtil.getTextContent(_patchElement, "length", true));

            String _encryptionType = null;
            String _encryptionKey = null;
            String _encryptionIV = null;
            Element _encryptionElement = XMLUtil.getElement(_patchElement, "encryption", false);
            if (_encryptionElement != null) {
                _encryptionType = XMLUtil.getTextContent(_encryptionElement, "type", true);
                _encryptionKey = XMLUtil.getTextContent(_encryptionElement, "key", true);
                _encryptionIV = XMLUtil.getTextContent(_encryptionElement, "IV", true);
            }

            return new Update(_id, _versionFrom, _versionTo, _patchUrl, _patchChecksum, _patchLength, _encryptionType, _encryptionKey, _encryptionIV);
        }

        protected Element getElement(Document doc) {
            Element _update = doc.createElement("update");
            _update.setAttribute("id", Integer.toString(id));

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

        try {
            Catalog catalog = Catalog.read(content);
            System.out.println(catalog.output());
        } catch (InvalidFormatException ex) {
        }
    }
}
