package domparserspl;

import java.io.IOException;
import java.util.Scanner;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import javax.xml.transform.OutputKeys;
import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuarPL
 */
public class DOMParserSPL {

    private static final String INDENT_NIVEL = " "; // Indentación
    
    
    //MÉTODO MOSTRAR NODOS
    public static void muestraNodo(Node nodo, int nivel, PrintStream ps) {
        if (nodo.getNodeType() == Node.TEXT_NODE) { // Ignora textos vacíos
            String text = nodo.getNodeValue();
            if (text.trim().length() == 0) {
                return;
            }
        }
        for (int i = 0; i < nivel; i++) { //Creamos un bucle para leer los datos del xml y loS mostramos en la consola
            ps.print(INDENT_NIVEL);
        }
        switch (nodo.getNodeType()) { // Escribe información de nodo según tipo
            case Node.DOCUMENT_NODE: // Documento
                Document doc = (Document) nodo;
                ps.println("Documento DOM, versión: " + doc.getXmlVersion()
                        + ", codificación: " + doc.getXmlEncoding());
                break;
            case Node.ELEMENT_NODE: // Elemento
                ps.print("<" + nodo.getNodeName());
                NamedNodeMap listaAtr = nodo.getAttributes(); // Lista atributos
                for (int i = 0; i < listaAtr.getLength(); i++) {
                    Node atr = listaAtr.item(i);
                    ps.print(" @" + atr.getNodeName() + "[" + atr.getNodeValue() + "]");
                }
                ps.println(">");
                break;
            case Node.TEXT_NODE: // Texto
                ps.println(nodo.getNodeName() + "[" + nodo.getNodeValue() + "]");
                break;
            default: // Otro tipo de nodo
                ps.println("(nodo de tipo: " + nodo.getNodeType() + ")");
        }
        NodeList nodosHijos = nodo.getChildNodes(); // Almacenamos nodo hijo con una variable de tipo NodoLIst.
        for (int i = 0; i < nodosHijos.getLength(); i++) { //Muestra nodos hijos
            muestraNodo(nodosHijos.item(i), nivel + 1, ps);
        }
    }

    //MÉTODO SIMPLE VALIDACIÓN MEDIANTE SCHEMA
    public static boolean validateXMLSchema(String xsdPath, String xmlPath) {

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlPath)));
        } catch (IOException | SAXException e) {
            System.out.println("Excepción: " + e.getMessage());
            return false;
        }
        return true;
    }

    
    public static void main(String[] args) {
        String dni, apellido, cp, xsdPath, xmlPath;
        Scanner sc = new Scanner(System.in);

        System.out.println("PROGRAMA VALIDACIÓN E INSERCIÓN DE DATOS EN XML DE CLIENTES EXISTENTE BY SPL");
        System.out.println("Pulse cualquier tecla para continuar...");
        sc.nextLine();

        try {
// Parse del archivo XML
            xmlPath = "./clientes.xml";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlPath);
            muestraNodo(doc, 0, System.out);

// Validar XML con schema
            //Comprueba que exista el schema, si no, finaliza el programa
            File fEsq = new File("clientes.xsd");
            xsdPath = "./clientes.xsd";
            if (!fEsq.isFile()) {
                System.err.println("Fichero xsd no encontrado. Introduce el schema en la raíz del proyecto.");
                return;
            } else {
                System.out.println("VALIDANDO ARCHIVO " + xmlPath + " CON EL SCHEMA " + xsdPath);
            }

            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            //Comprueba que la validación sea correcta
            if (validateXMLSchema(xsdPath, xmlPath)) {

                System.out.println("La validación del archivo" + xmlPath + " con el schema" + xsdPath + " fue exitosa.");

                System.out.println("Introduciendo nuevo cliente");
                // Almacenar datos del nuevo cliente
                System.out.println("Introduzca DNI del nuevo cliente: ");
                dni = sc.nextLine();
                System.out.println("Introduzca apellido del nuevo cliente: ");
                apellido = sc.nextLine();
                System.out.println("Introduzca Código Postal del nuevo cliente: ");
                cp = sc.nextLine();

// Crear nuevo elemento cliente
                Element elCliente = doc.createElement("cliente");
                elCliente.setAttribute("DNI", dni);
                Element elApell = doc.createElement("apellidos");
                elApell.appendChild(doc.createTextNode(apellido.toUpperCase()));
                elCliente.appendChild(elApell);
                Element elCP = doc.createElement("CP");
                elCP.appendChild(doc.createTextNode(cp));
                elCliente.appendChild(elCP);

// Obtener elemento raíz y primer hijo
                Element root = doc.getDocumentElement();
                Node firstChild = root.getFirstChild();

// Insertar el elemento cliente antes del primer hijo
                root.insertBefore(elCliente, firstChild);

// Escribir el doc modificado a un nuevo archivo XML
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                //Renombrar archivo actual para crear backup con fecha
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                Date date = new Date();
                String dateToStr = (dateFormat.format(date) + "clientes.xml.bak");
                File f1 = new File("clientes.xml");

                File f2 = new File(dateToStr);
                System.out.println(dateToStr);
                boolean correcto = f1.renameTo(f2);

                if (correcto) {
                    System.out.println("Backup generado y nuevo archivo guardado");
                    System.out.println("Mostrando nuevo archivo generado:");
                    muestraNodo(doc, 0, System.out);
                } else {
                    System.out.println("Error de renombrado. Tiene permisos suficientes?");
                }
                StreamResult result = new StreamResult("clientes.xml");
                transformer.transform(source, result);

            } else {
                System.out.println("Validación de schema no válida, programa finalizado.");
            }

        } catch (IOException | IllegalArgumentException | ParserConfigurationException | TransformerException | DOMException | SAXException e) {
            System.out.println("Excepción: " + e.getMessage());
        }

    }

}
