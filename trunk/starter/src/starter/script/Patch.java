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
public class Patch {

    protected String versionFrom;
    protected String versionTo;
    protected List<Operation> operations;
    protected List<ValidationFile> validations;

    public Patch(String versionFrom, String versionTo, List<Operation> operations, List<ValidationFile> validations) {
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.operations = new ArrayList<Operation>(operations);
        this.validations = new ArrayList<ValidationFile>(validations);
    }

    public String getVersionFrom() {
        return versionFrom;
    }

    public void setVersionFrom(String versionFrom) {
        this.versionFrom = versionFrom;
    }

    public String getVersionTo() {
        return versionTo;
    }

    public void setVersionTo(String versionTo) {
        this.versionTo = versionTo;
    }

    public List<Operation> getOperations() {
        return new ArrayList<Operation>(operations);
    }

    public void setOperations(List<Operation> files) {
        this.operations = new ArrayList<Operation>(files);
    }

    public List<ValidationFile> getValidations() {
        return new ArrayList<ValidationFile>(validations);
    }

    public void setValidations(List<ValidationFile> validations) {
        this.validations = new ArrayList<ValidationFile>(validations);
    }

    public static Patch read(byte[] content) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(new ByteArrayInputStream(content));
            Element _updateNode = doc.getDocumentElement();


            NodeList _versionNodeList = _updateNode.getElementsByTagName("version");
            if (_versionNodeList.getLength() == 0) {
                return null;
            }
            NodeList _versionFromNode = ((Element) _versionNodeList.item(0)).getElementsByTagName("from");
            NodeList _versionToNode = ((Element) _versionNodeList.item(0)).getElementsByTagName("to");
            if (_versionFromNode.getLength() == 0 || _versionToNode.getLength() == 0) {
                return null;
            }
            Element _versionFromElement = (Element) _versionFromNode.item(0);
            Element _versionToElement = (Element) _versionToNode.item(0);

            String _versionFrom = _versionFromElement.getTextContent();
            String _versionTo = _versionToElement.getTextContent();


            NodeList _operationsNodeList = _updateNode.getElementsByTagName("operations");
            if (_operationsNodeList.getLength() == 0) {
                return null;
            }

            List<Operation> _operations = new ArrayList<Operation>();

            NodeList _operationNodeList = ((Element) _operationsNodeList.item(0)).getElementsByTagName("operation");
            for (int i = 0, iEnd = _operationNodeList.getLength(); i < iEnd; i++) {
                Element _operationNode = (Element) _operationNodeList.item(i);
                Operation _file = Operation.read(_operationNode);
                if (_file != null) {
                    _operations.add(_file);
                }
            }


            NodeList _validationNodeList = _updateNode.getElementsByTagName("validation");
            if (_validationNodeList.getLength() == 0) {
                return null;
            }

            List<ValidationFile> _validations = new ArrayList<ValidationFile>();

            NodeList _validationFileNodeList = ((Element) _validationNodeList.item(0)).getElementsByTagName("file");
            for (int i = 0, iEnd = _validationFileNodeList.getLength(); i < iEnd; i++) {
                Element _validationFileNode = (Element) _validationFileNodeList.item(i);
                ValidationFile _validation = ValidationFile.read(_validationFileNode);
                if (_validation != null) {
                    _validations.add(_validation);
                }
            }

            return new Patch(_versionFrom, _versionTo, _operations, _validations);
        } catch (Exception ex) {
            Logger.getLogger(Patch.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String output() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("update");
            doc.appendChild(rootElement);


            Element versionElement = doc.createElement("version");
            rootElement.appendChild(versionElement);

            Element versionFromElement = doc.createElement("from");
            versionFromElement.setTextContent(versionFrom);
            versionElement.appendChild(versionFromElement);
            Element versionToElement = doc.createElement("to");
            versionToElement.setTextContent(versionTo);
            versionElement.appendChild(versionToElement);


            Element operationsElement = doc.createElement("operations");
            rootElement.appendChild(operationsElement);
            for (Operation operation : operations) {
                operationsElement.appendChild(operation.getElement(doc));
            }


            Element validationElement = doc.createElement("validation");
            rootElement.appendChild(validationElement);
            for (ValidationFile file : validations) {
                validationElement.appendChild(file.getElement(doc));
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

    public static class Operation {

        protected String type;
        //
        protected int patchPos;
        protected int patchLength;
        //
        protected String oldFilePath;
        protected String oldFileChecksum;
        protected int oldFileLength;
        //
        protected String newFilePath;
        protected String newFileChecksum;
        protected int newFileLength;

        public Operation(String type, int patchPos, int patchLength, String oldFilePath, String oldFileChecksum, int oldFileLength, String newFilePath, String newFileChecksum, int newFileLength) {
            this.type = type;
            this.patchPos = patchPos;
            this.patchLength = patchLength;
            this.oldFilePath = oldFilePath;
            this.oldFileChecksum = oldFileChecksum;
            this.oldFileLength = oldFileLength;
            this.newFilePath = newFilePath;
            this.newFileChecksum = newFileChecksum;
            this.newFileLength = newFileLength;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getPatchPos() {
            return patchPos;
        }

        public void setPatchPos(int patchPos) {
            this.patchPos = patchPos;
        }

        public int getPatchLength() {
            return patchLength;
        }

        public void setPatchLength(int patchLength) {
            this.patchLength = patchLength;
        }

        public String getOldFilePath() {
            return oldFilePath;
        }

        public void setOldFilePath(String oldFilePath) {
            this.oldFilePath = oldFilePath;
        }

        public String getOldFileChecksum() {
            return oldFileChecksum;
        }

        public void setOldFileChecksum(String oldFileChecksum) {
            this.oldFileChecksum = oldFileChecksum;
        }

        public int getOldFileLength() {
            return oldFileLength;
        }

        public void setOldFileLength(int oldFileLength) {
            this.oldFileLength = oldFileLength;
        }

        public String getNewFilePath() {
            return newFilePath;
        }

        public void setNewFilePath(String newFilePath) {
            this.newFilePath = newFilePath;
        }

        public String getNewFileChecksum() {
            return newFileChecksum;
        }

        public void setNewFileChecksum(String newFileChecksum) {
            this.newFileChecksum = newFileChecksum;
        }

        public int getNewFileLength() {
            return newFileLength;
        }

        public void setNewFileLength(int newFileLength) {
            this.newFileLength = newFileLength;
        }

        protected static Operation read(Element fileElement) {
            if (fileElement == null) {
                return null;
            }

            NodeList _typeNodeList = fileElement.getElementsByTagName("type");
            if (_typeNodeList.getLength() == 0) {
                return null;
            }
            Element _typeElement = (Element) _typeNodeList.item(0);
            String _type = _typeElement.getTextContent();

            //<editor-fold defaultstate="collapsed" desc="patch">
            int pos = -1;
            int length = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("new")) {
                NodeList _patchNodeList = fileElement.getElementsByTagName("patch");
                if (_patchNodeList.getLength() == 0) {
                    return null;
                }
                Element _patchElement = (Element) _patchNodeList.item(0);

                NodeList _posNodeList = _patchElement.getElementsByTagName("pos");
                if (_posNodeList.getLength() == 0) {
                    return null;
                }
                Element _posElement = (Element) _posNodeList.item(0);
                NodeList _lengthNodeList = _patchElement.getElementsByTagName("length");
                if (_lengthNodeList.getLength() == 0) {
                    return null;
                }
                Element _lengthElement = (Element) _lengthNodeList.item(0);

                pos = Integer.parseInt(_posElement.getTextContent());
                length = Integer.parseInt(_lengthElement.getTextContent());
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="old">
            String oldPath = null;
            String oldChecksum = null;
            int oldLength = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("remove")) {
                NodeList _oldFileNodeList = fileElement.getElementsByTagName("old-file");
                if (_oldFileNodeList.getLength() == 0) {
                    return null;
                }
                Element _oldFileElement = (Element) _oldFileNodeList.item(0);

                NodeList _oldPathNodeList = _oldFileElement.getElementsByTagName("path");
                if (_oldPathNodeList.getLength() == 0) {
                    return null;
                }
                Element _oldPathElement = (Element) _oldPathNodeList.item(0);
                NodeList _oldChecksumNodeList = _oldFileElement.getElementsByTagName("checksum");
                if (_oldChecksumNodeList.getLength() == 0) {
                    return null;
                }
                Element _oldChecksumElement = (Element) _oldChecksumNodeList.item(0);
                NodeList _oldLengthNodeList = _oldFileElement.getElementsByTagName("length");
                if (_oldLengthNodeList.getLength() == 0) {
                    return null;
                }
                Element _oldLengthElement = (Element) _oldLengthNodeList.item(0);

                oldPath = _oldPathElement.getTextContent();
                oldChecksum = _oldChecksumElement.getTextContent();
                oldLength = Integer.parseInt(_oldLengthElement.getTextContent());
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="new">
            String newPath = null;
            String newChecksum = null;
            int newLength = -1;
            if (_type.equals("patch") || _type.equals("replace") || _type.equals("new")) {
                NodeList _newFileNodeList = fileElement.getElementsByTagName("new-file");
                if (_newFileNodeList.getLength() == 0) {
                    return null;
                }
                Element _newFileElement = (Element) _newFileNodeList.item(0);

                NodeList _newPathNodeList = _newFileElement.getElementsByTagName("path");
                if (_newPathNodeList.getLength() == 0) {
                    return null;
                }
                Element _newPathElement = (Element) _newPathNodeList.item(0);
                NodeList _newChecksumNodeList = _newFileElement.getElementsByTagName("checksum");
                if (_newChecksumNodeList.getLength() == 0) {
                    return null;
                }
                Element _newChecksumElement = (Element) _newChecksumNodeList.item(0);
                NodeList _newLengthNodeList = _newFileElement.getElementsByTagName("length");
                if (_newLengthNodeList.getLength() == 0) {
                    return null;
                }
                Element _newLengthElement = (Element) _newLengthNodeList.item(0);

                newPath = _newPathElement.getTextContent();
                newChecksum = _newChecksumElement.getTextContent();
                newLength = Integer.parseInt(_newLengthElement.getTextContent());
            }
            //</editor-fold>

            return new Operation(_type, pos, length, oldPath, oldChecksum, oldLength, newPath, newChecksum, newLength);
        }

        protected Element getElement(Document doc) {
            Element _file = doc.createElement("file");

            Element _type = doc.createElement("type");
            _type.appendChild(doc.createTextNode(type));
            _file.appendChild(_type);

            //<editor-fold defaultstate="collapsed" desc="patch">
            if (patchPos != -1) {
                Element _patch = doc.createElement("patch");
                _file.appendChild(_patch);

                Element _patchUrl = doc.createElement("pos");
                _patchUrl.appendChild(doc.createTextNode(Integer.toString(patchPos)));
                _patch.appendChild(_patchUrl);

                Element _patchLength = doc.createElement("length");
                _patchLength.appendChild(doc.createTextNode(Integer.toString(patchLength)));
                _patch.appendChild(_patchLength);
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="old">
            if (oldFilePath != null) {
                Element _old = doc.createElement("old");
                _file.appendChild(_old);

                Element _oldFilePath = doc.createElement("path");
                _oldFilePath.appendChild(doc.createTextNode(oldFilePath));
                _old.appendChild(_oldFilePath);

                Element _oldFileChecksum = doc.createElement("checksum");
                _oldFileChecksum.appendChild(doc.createTextNode(oldFileChecksum));
                _old.appendChild(_oldFileChecksum);

                Element _oldFileLength = doc.createElement("length");
                _oldFileLength.appendChild(doc.createTextNode(Integer.toString(oldFileLength)));
                _old.appendChild(_oldFileLength);
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="new">
            if (newFilePath != null) {
                Element _new = doc.createElement("new");
                _file.appendChild(_new);

                Element _newFilePath = doc.createElement("path");
                _newFilePath.appendChild(doc.createTextNode(newFilePath));
                _new.appendChild(_newFilePath);

                Element _newFileChecksum = doc.createElement("checksum");
                _newFileChecksum.appendChild(doc.createTextNode(newFileChecksum));
                _new.appendChild(_newFileChecksum);

                Element _newFileLength = doc.createElement("length");
                _newFileLength.appendChild(doc.createTextNode(Integer.toString(newFileLength)));
                _new.appendChild(_newFileLength);
            }
            //</editor-fold>

            return _file;
        }
    }

    public static class ValidationFile {

        private String filePath;
        private String fileChecksum;
        private String fileLength;

        public ValidationFile(String filePath, String fileChecksum, String fileLength) {
            this.filePath = filePath;
            this.fileChecksum = fileChecksum;
            this.fileLength = fileLength;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileChecksum() {
            return fileChecksum;
        }

        public void setFileChecksum(String fileChecksum) {
            this.fileChecksum = fileChecksum;
        }

        public String getFileLength() {
            return fileLength;
        }

        public void setFileLength(String fileLength) {
            this.fileLength = fileLength;
        }

        protected static ValidationFile read(Element fileElement) {
            if (fileElement == null) {
                return null;
            }

            NodeList _pathNodeList = fileElement.getElementsByTagName("path");
            if (_pathNodeList.getLength() == 0) {
                return null;
            }
            Element _pathElement = (Element) _pathNodeList.item(0);

            NodeList _checksumNodeList = fileElement.getElementsByTagName("checksum");
            if (_checksumNodeList.getLength() == 0) {
                return null;
            }
            Element _checksumElement = (Element) _checksumNodeList.item(0);

            NodeList _lengthNodeList = fileElement.getElementsByTagName("length");
            if (_lengthNodeList.getLength() == 0) {
                return null;
            }
            Element _lengthElement = (Element) _lengthNodeList.item(0);

            String _path = _pathElement.getTextContent();
            String _checksum = _checksumElement.getTextContent();
            String _length = _lengthElement.getTextContent();

            return new ValidationFile(_path, _checksum, _length);
        }

        protected Element getElement(Document doc) {
            Element _file = doc.createElement("file");

            Element _path = doc.createElement("path");
            _path.appendChild(doc.createTextNode(filePath));
            _file.appendChild(_path);

            Element _checksum = doc.createElement("checksum");
            _checksum.appendChild(doc.createTextNode(fileChecksum));
            _file.appendChild(_checksum);

            Element _length = doc.createElement("length");
            _length.appendChild(doc.createTextNode(fileLength));
            _file.appendChild(_length);

            return _file;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("1.0.0_1.0.1.xml");
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = new FileInputStream(file);
        fin.read(content);
        fin.close();

        Patch update = Patch.read(content);
        System.out.println(update.output());
    }
}
