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
 * @author Andreas Gawecki
 */
public class Ide extends NodeImplBase {

  JooSymbol ide;

  public Ide(JooSymbol ide) {
    this.ide = ide;
  }

  @Override
  public Node analyze(Node parentNode, AnalyzeContext context) {
    super.analyze(parentNode, context);
    Scope declaringScope = context.getScope().findScopeThatDeclares(this);
    if (declaringScope != null) {
      Node ideDeclaration = declaringScope.getIdeDeclaration(this);
      if (ideDeclaration instanceof ImportDirective) {
        ((ImportDirective)ideDeclaration).wasUsed(context);
      }
    }
    return this;
  }

  public void generateCode(JsWriter out) throws IOException {
     out.writeSymbol(ide);
  }

  public String[] getQualifiedName() {
    return new String[] { getName() };
  }

  public String getQualifiedNameStr() {
    return getName();
  }

  public String getName() {
    return ide.getText();
  }

  public JooSymbol getSymbol() {
    return ide;
  }


}