/*
 * Copyright 2008 CoreMedia AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

package net.jangaroo.jooc;

import java.io.IOException;

/**
 * @author Frank Wienberg
 */
public class NamespacedIde extends Ide {

  private JooSymbol namespace;
  private JooSymbol symNamespaceSep;


  public NamespacedIde(JooSymbol namespace, JooSymbol symNamespaceSep, JooSymbol symIde) {
    super(symIde);
    this.namespace = namespace;
    this.symNamespaceSep = symNamespaceSep;
  }

  static void warnUndefinedNamespace(AnalyzeContext context, JooSymbol namespace) {
    if (namespace.sym==sym.IDE) { // all other symbols should be predefined namespaces like "public" etc.
      String namespaceName = namespace.getText();
      if (!context.getCurrentPackage().isNamespace(namespaceName) && context.getScope().findScopeThatDeclares(namespaceName)==null) {
        Jooc.warning(namespace, "Undeclared namespace '"+ namespaceName +"', assuming it already is in scope.");
      }
    }
  }
  @Override
  public Node analyze(Node parentNode, AnalyzeContext context) {
    warnUndefinedNamespace(context, namespace);
    return super.analyze(parentNode, context);
  }

  public void generateCode(JsWriter out) throws IOException {
    // so far, namespaces are only comments:
    out.beginComment();
    out.writeSymbol(namespace);
    out.writeSymbol(symNamespaceSep);
    out.endComment();
    out.writeSymbol(ide);
  }

  static String getNamespacePrefix(JooSymbol namespace) {
    return namespace==null || namespace.sym!=sym.IDE ? "" : namespace.getText()+"::";
  }

  @Override
  public String getName() {
    return getNamespacePrefix(namespace)+super.getName();
  }

  public String[] getQualifiedName() {
    return new String[]{namespace.getText(), ide.getText()};
  }

  @Override
  public String getQualifiedNameStr() {
    return QualifiedIde.constructQualifiedNameStr(getQualifiedName(), "::");
  }

  public JooSymbol getSymbol() {
    return namespace;
  }

}