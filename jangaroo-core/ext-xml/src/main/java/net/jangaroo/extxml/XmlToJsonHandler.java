/*
 * Copyright (c) 2009, CoreMedia AG, Hamburg. All rights reserved.
 */
package net.jangaroo.extxml;

import net.jangaroo.extxml.json.Json;
import net.jangaroo.extxml.json.JsonArray;
import net.jangaroo.extxml.json.JsonObject;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

/**
 * Generates an internal representation for the component XML
 */
public final class XmlToJsonHandler implements ContentHandler {

  private Locator locator;
  private Json result;
  private ComponentSuite componentSuite;

  private Set<String> imports = new LinkedHashSet<String>();
  private String componentDescription = "";
  private List<ConfigAttribute> cfgs = new ArrayList<ConfigAttribute>();

  private ErrorHandler errorHandler;

  private static String numberPattern = "-?([0-9]+\\.[0-9]*)|(\\.[0-9]+)|([0-9]+)([eE][+-]?[0-9]+)?";

  private NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

  private Stack<net.jangaroo.extxml.json.Json> objects = new Stack<Json>();
  private Stack<String> attributes = new Stack<String>();

  //stores all characters
  private StringBuffer characterStack;

  private boolean expectObjects = true;

  private boolean expectsOptionalConfigDescription = false;
  private boolean expectsOptionalComponentDescription = false;

  public XmlToJsonHandler(ComponentSuite componentSuite, ErrorHandler handler) {
    this.componentSuite = componentSuite;
    this.errorHandler = handler;
  }

  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  public void startDocument() throws SAXException {
  }

  public void endDocument() throws SAXException {
    if (!objects.empty()) {
      errorHandler.error("Json object stack not empty at the end of the document.",
          locator.getLineNumber(),
          locator.getColumnNumber());
    }
    if (!attributes.empty()) {
      errorHandler.error("Attribute stack not empty at the end of the document.",
          locator.getLineNumber(),
          locator.getColumnNumber());
    }
    if (!expectObjects) {
      errorHandler.error("The parser is in the wrong state at the end of the document.",
          locator.getLineNumber(),
          locator.getColumnNumber());
    }
  }

  public void startPrefixMapping(String prefix, String uri) throws SAXException {
  }

  public void endPrefixMapping(String prefix) throws SAXException {
  }

  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if ("component".equals(localName)) {
      //prepare characterStack for optional component description
      expectsOptionalComponentDescription = true;
    } else if ("import".equals(localName)) {
      imports.add(atts.getValue("class"));
    } else if ("cfg".equals(localName)) {
      //handle config elements
      cfgs.add(new ConfigAttribute(atts.getValue("name"), atts.getValue("type")));
      expectsOptionalConfigDescription = true;
    } else if ("description".equals(localName)) {
      if(expectsOptionalConfigDescription || expectsOptionalComponentDescription) {
        characterStack = new StringBuffer();
      }
    } else if ("object".equals(localName)) {
      //handle json elements differently: either as a anonymous element with attributes or
      //json as text node of the element.
      if (expectObjects) {
        if (atts.getLength() == 0) {
          //create new character Statck that will store the JSON string
          characterStack = new StringBuffer();
        } else {
          addElementToJsonObject(createJsonObject(atts));
        }
      }
    } else {
      JsonObject jsonObject = createJsonObject(atts);
      Json parentJson = objects.empty() ? null : objects.peek();

      if (expectObjects) {
        String type = "xtype";
        if (!attributes.isEmpty() && attributes.peek().equals("plugins")) {
          type = "ptype";
        }
        //find component class for namespace + localName:
        ComponentClass compClazz = componentSuite.getComponentClassByNamespaceAndLocalName(uri, localName);
        if (compClazz != null) {
          imports.add(compClazz.getFullClassName());
          jsonObject.set(type, compClazz.getXtype());
        } else {
          errorHandler.error(String.format("No component class for element name '%s' found in component suite '%s'!", localName, uri), locator.getLineNumber(), locator.getColumnNumber());
          jsonObject.set(type, localName);
        }
        addElementToJsonObject(jsonObject);
        addObjectToStack(jsonObject);
      } else {
        assert parentJson != null;
        addAttributeToStack(localName);
        if (!jsonObject.isEmpty()) {
          parentJson.set(localName, jsonObject);
          addObjectToStack(jsonObject);
        }
      }
    }
  }

  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("component".equals(localName)) {
      //done
    } else if ("import".equals(localName)) {
    } else if ("cfg".equals(localName)) {
    } else if ("description".equals(localName)) {
      if (characterStack != null) {
        if(expectsOptionalConfigDescription) {
          cfgs.get(cfgs.size() - 1).setDescription(characterStack.toString().trim());
          expectsOptionalConfigDescription = false;
        } else if (expectsOptionalComponentDescription) {
          componentDescription = characterStack.toString().trim();
          expectsOptionalComponentDescription = false;
        }
      }
      characterStack = null;
    } else if ("object".equals(localName)) {
      if (characterStack != null) {
        addElementToJsonObject("{" + characterStack.toString().trim() + "}");
        characterStack = null;
      }
    } else {
      if (expectObjects) {
        String attr = attributes.pop();
        assert localName.equals(attr);
        expectObjects = false;
      } else {
        net.jangaroo.extxml.json.Json json = removeObjectFromStack();
        if (json.get("xtype") == null && json.get("ptype") == null) {
          removeAttributeFromStack();
        }
        //if the the last object has been removed,
        //store the result
        if (objects.empty()) {
          result = json;

        }
      }
    }
  }

  public void characters(char[] ch, int start, int length) throws SAXException {
    if (characterStack != null) {
      String cdata = new String(ch, start, length);
      characterStack.append(cdata);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
  }

  public void processingInstruction(String target, String data) throws SAXException {
  }

  public void skippedEntity(String name) throws SAXException {
  }

  private JsonObject createJsonObject(Attributes atts) {
    JsonObject newJsonObject = new JsonObject();
    for (int i = 0; i < atts.getLength(); i++) {
      String attsValue = atts.getValue(i);
      Object typedValue;
      if ("true".equals(attsValue) || "false".equals(attsValue)) {
        typedValue = Boolean.parseBoolean(attsValue);
      } else if (attsValue.matches(numberPattern)) {
        try {
          typedValue = nf.parse(attsValue);
        } catch (ParseException e) {
          //well seems to be not a number...
          typedValue = attsValue;
        }
      } else {
        typedValue = attsValue;
      }
      newJsonObject.set(atts.getLocalName(i), typedValue);
    }
    return newJsonObject;
  }

  /**
   * adds the element to the current json object, if it is an array knows how to handle it...
   *
   * @param element the element to add.
   */
  private void addElementToJsonObject(Object element) {
    Json parentJson = objects.empty() ? null : objects.peek();
    if (parentJson != null) {
      assert !attributes.empty();
      String attr = attributes.peek();
      Object obj = parentJson.get(attr);
      if (obj == null) {
        parentJson.set(attr, element);
      } else if (!(obj instanceof JsonArray)) {
        JsonArray array = new JsonArray();
        array.push(obj);
        array.push(element);
        parentJson.set(attr, array);
      } else {
        ((JsonArray) obj).push(element);
      }
    }
  }

  /**
   * Adds the Json to the stack and
   * indicates that no other json object is expected by the parser
   *
   * @param jsonObject the json to add to the stack
   */
  private void addObjectToStack(Json jsonObject) {
    objects.push(jsonObject);
    expectObjects = false;
  }

  /**
   * Adds the Attribute to the stack and
   * indicates that now only json objects are expected by the parser
   *
   * @param attribute the name of the attribute
   */
  private void addAttributeToStack(String attribute) {
    attributes.push(attribute);
    expectObjects = true;
  }

  /**
   * Removes the last json object from stack and indicates that other json objects
   * are expected by the parser
   *
   * @return the last json object from the stack
   */
  private Json removeObjectFromStack() {
    expectObjects = true;
    return objects.pop();
  }

  /**
   * Remove the last attribute from the attribute stack and indicate that attributes are
   * expected by the parser
   */
  private void removeAttributeFromStack() {
    attributes.pop();
    expectObjects = false;
  }

  public Json getJSON() {
    return result;
  }

  public List<String> getImports() {
    return new ArrayList<String>(imports);
  }

  public List<ConfigAttribute> getCfgs() {
    return cfgs;
  }

  public String getComponentDescription() {
    return componentDescription;
  }

  public String getSuperClassName() {
    if (result != null) {
      String xtype = (String) this.result.get("xtype");
      if (xtype == null) {
        errorHandler.error("Component xtype not found.");
        return null;
      }

      ComponentClass componentClass = componentSuite.findComponentClassByXtype(xtype);
      if (componentClass == null) {
        errorHandler.error(MessageFormat.format("Super component class for xtype ''{0}'' not found.", xtype));
        return null;
      }
      return componentClass.getFullClassName();
    } else {
      errorHandler.error("Xml Parser has no result.");
      return null;
    }
  }

  public Json getJson() {
    if (result != null) {
      ((JsonObject) result).remove("xtype");
      return (result);
    } else {
      errorHandler.error("Xml Parser has no result.");
      return null;
    }
  }

}